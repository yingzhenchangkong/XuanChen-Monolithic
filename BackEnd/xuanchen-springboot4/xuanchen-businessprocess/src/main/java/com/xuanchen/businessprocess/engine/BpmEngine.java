package com.xuanchen.businessprocess.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuanchen.common.server.WebSocketServer;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.businessprocess.definition.model.DiagramModel;
import com.xuanchen.businessprocess.definition.parse.BpmnDiagramParser;
import com.xuanchen.businessprocess.definition.parse.BusinessProcessDiagramValidator;
import com.xuanchen.businessprocess.handler.BpmBusinessHandler;
import com.xuanchen.businessprocess.handler.BpmHandlerRegistry;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;
import com.xuanchen.businessprocess.instance.mapper.BpmInstanceMapper;
import com.xuanchen.businessprocess.record.entity.BpmRecord;
import com.xuanchen.businessprocess.record.mapper.BpmRecordMapper;
import com.xuanchen.businessprocess.task.entity.BpmTask;
import com.xuanchen.businessprocess.task.mapper.BpmTaskMapper;
import com.xuanchen.system.systodo.entity.SysTodo;
import com.xuanchen.system.systodo.service.ISysTodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.xuanchen.businessprocess.support.mapper.BpmSupportMapper;

/**
 * 审批引擎-->流程推进核心（submit / approve / reject / revoke）
 *
 * <p>推进模型：多 token 沿图快照前进。用户任务生成待办即停；
 * 排他网关按条件表达式选边（全不满足走 default）；并行网关分叉/汇聚（汇聚按入边数计数等齐）；
 * 到达结束节点分支 token 消亡，全部 token 消亡后实例办结。
 * token 状态（并行分叉/汇聚计数/存活数）以 JSON 持久化在 bpm_instance.token_state；
 * 任务处理使用条件 UPDATE 乐观锁防并发重复审批，实例级行锁（FOR UPDATE）串行化同一实例的并发推进。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class BpmEngine {

    private static final Logger log = LoggerFactory.getLogger(BpmEngine.class);

    /** 实例状态：审批中 */
    public static final int INSTANCE_RUNNING = 1;
    /** 实例状态：通过 */
    public static final int INSTANCE_APPROVED = 2;
    /** 实例状态：驳回 */
    public static final int INSTANCE_REJECTED = 3;
    /** 实例状态：撤回 */
    public static final int INSTANCE_REVOKED = 4;

    /** 任务状态：待处理 */
    public static final int TASK_PENDING = 1;
    /** 任务状态：已处理 */
    public static final int TASK_HANDLED = 2;
    /** 任务状态：已作废（驳回/撤回时清理未处理任务） */
    public static final int TASK_VOIDED = 3;
    /** 任务状态：已转办（原任务作废，另生成新任务给被转办人） */
    public static final int TASK_TRANSFERRED = 4;

    /**
     * token 状态（持久化在 bpm_instance.token_state）
     *
     * <p>active = 存活 token 数（含运动中/停驻在用户任务/等待在汇聚网关）；
     * arrivals = 并行汇聚网关已到达未放行的分支计数（joinKey -> 已到达数）。
     * 计量：分叉 active += 出边数-1；汇聚放行 active -= 入边数-1；分支到终点 active -= 1；
     * active 归零即实例办结。</p>
     */
    public static class TokenState {
        public Map<String, Integer> arrivals = new LinkedHashMap<>();
        public int active = 1;
    }

    @Autowired
    private BpmInstanceMapper instanceMapper;
    @Autowired
    private BpmTaskMapper taskMapper;
    @Autowired
    private BpmRecordMapper recordMapper;
    @Autowired
    private BpmAssigneeResolver assigneeResolver;
    @Autowired
    private BpmExprEvaluator exprEvaluator;
    @Autowired
    private BpmHandlerRegistry handlerRegistry;
    @Autowired
    private ISysTodoService sysTodoService;
    @Autowired
    private BpmSupportMapper supportMapper;

    /**
     * 审批通过
     */
    @Transactional(rollbackFor = Exception.class)
    public String approve(String userName, String taskId, String opinion) {
        BpmTask task = requireTask(taskId);
        if (!userName.equals(task.getAssignee())) {
            return "无权处理该审批任务！";
        }
        // 乐观锁：仅待处理可处理
        int rows = claimTask(task.getId());
        if (rows == 0) {
            return "该审批任务已被处理！";
        }
        // 实例级行锁：串行化同一实例的并发推进（并行分支下 token 状态必须串行变更）
        instanceMapper.selectByIdForUpdate(task.getInstanceId());
        task.setStatus(TASK_HANDLED);
        task.setResult(1);
        task.setOpinion(opinion);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        BpmInstance instance = requireInstance(task.getInstanceId());
        if (instance.getStatus() == null || instance.getStatus() != INSTANCE_RUNNING) {
            return "流程已办结，无需继续处理！";
        }
        addRecord(instance.getId(), task.getNodeKey(), task.getNodeName(), userName, 2, opinion);
        closeTodo(task.getId());

        DiagramModel diagram = loadSnapshot(instance);
        int mode = approvalMode(diagram, task.getNodeKey());
        if (mode == 2) {
            // 会签：节点内还有待处理任务则 token 不动
            Long pending = taskMapper.selectCount(new LambdaQueryWrapper<BpmTask>()
                    .eq(BpmTask::getInstanceId, instance.getId())
                    .eq(BpmTask::getNodeKey, task.getNodeKey())
                    .eq(BpmTask::getStatus, TASK_PENDING));
            if (pending != null && pending > 0) {
                return "已同意，等待其他审批人处理";
            }
        }
        advance(instance, diagram, task.getNodeKey(), task.getAssignee(), null);
        return null;
    }

    /**
     * 审批驳回：实例整体办结为驳回（重新提交生成新实例）
     */
    @Transactional(rollbackFor = Exception.class)
    public String reject(String userName, String taskId, String opinion) {
        BpmTask task = requireTask(taskId);
        if (!userName.equals(task.getAssignee())) {
            return "无权处理该审批任务！";
        }
        int rows = claimTask(task.getId());
        if (rows == 0) {
            return "该审批任务已被处理！";
        }
        // 实例级行锁（防与并行分支上的并发审批交错）
        instanceMapper.selectByIdForUpdate(task.getInstanceId());
        task.setStatus(TASK_HANDLED);
        task.setResult(2);
        task.setOpinion(opinion);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        BpmInstance instance = requireInstance(task.getInstanceId());
        if (instance.getStatus() == null || instance.getStatus() != INSTANCE_RUNNING) {
            return "流程已办结，无需驳回！";
        }
        addRecord(instance.getId(), task.getNodeKey(), task.getNodeName(), userName, 3, opinion);
        closeTodo(task.getId());

        // 清理其余待处理任务与待办
        voidPendingTasks(instance.getId());
        instance.setStatus(INSTANCE_REJECTED);
        instance.setCurrentNodeKey(task.getNodeKey());
        instance.setFinishTime(LocalDateTime.now());
        instance.setUpdateBy(userName);
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        clearTokenState(instance.getId());
        BpmBusinessHandler handler = handlerRegistry.get(instance.getBusinessType());
        if (handler != null) {
            handler.onRejected(instance);
        }
        return null;
    }

    /**
     * 发起人撤回：仅当前节点任务全部待处理时允许
     */
    @Transactional(rollbackFor = Exception.class)
    public String revoke(String userName, String instanceId) {
        BpmInstance instance = requireInstance(instanceId);
        if (!userName.equals(instance.getApplyUser())) {
            return "仅发起人可撤回！";
        }
        if (instance.getStatus() != INSTANCE_RUNNING) {
            return "流程已办结，无法撤回！";
        }
        // 当前节点（起始审批节点）尚有未处理任务才可撤回；任一任务已处理即不可
        Long pending = taskMapper.selectCount(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instanceId)
                .eq(BpmTask::getStatus, TASK_PENDING));
        Long handled = taskMapper.selectCount(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instanceId)
                .eq(BpmTask::getStatus, TASK_HANDLED));
        if (pending == null || pending == 0 || (handled != null && handled > 0)) {
            return "已有审批人处理，无法撤回！";
        }
        voidPendingTasks(instanceId);
        instance.setStatus(INSTANCE_REVOKED);
        instance.setFinishTime(LocalDateTime.now());
        instance.setUpdateBy(userName);
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        clearTokenState(instanceId);
        addRecord(instanceId, null, null, userName, 4, null);
        return null;
    }

    /**
     * 转办：当前审批人把待处理任务转给他人（原任务标记已转办，新任务派给被转办人）
     */
    @Transactional(rollbackFor = Exception.class)
    public String transfer(String userName, String taskId, String targetUser, String opinion) {
        if (targetUser == null || targetUser.isBlank()) {
            return "请选择被转办人！";
        }
        if (targetUser.equals(userName)) {
            return "不能转办给自己！";
        }
        if (supportMapper.countEnabledAccount(targetUser) <= 0) {
            return "被转办人不存在或已停用！";
        }
        BpmTask task = requireTask(taskId);
        if (!userName.equals(task.getAssignee())) {
            return "无权处理该审批任务！";
        }
        // 乐观锁：仅待处理可转办
        int rows = taskMapper.update(null, new LambdaUpdateWrapper<BpmTask>()
                .eq(BpmTask::getId, taskId)
                .eq(BpmTask::getStatus, TASK_PENDING)
                .set(BpmTask::getStatus, TASK_TRANSFERRED));
        if (rows == 0) {
            return "该审批任务已被处理！";
        }
        closeTodo(task.getId());

        BpmInstance instance = requireInstance(task.getInstanceId());
        String remark = (opinion == null || opinion.isBlank() ? "" : opinion + "；") + "转办给：" + targetUser;
        addRecord(instance.getId(), task.getNodeKey(), task.getNodeName(), userName, 5, remark);

        // 原节点生成新任务并同步待办/推送
        DiagramModel diagram = loadSnapshot(instance);
        DiagramModel.NodeModel node = nodeOf(diagram, task.getNodeKey());
        if (node == null) {
            throw new IllegalStateException("流程图快照中找不到节点：" + task.getNodeKey());
        }
        createTasks(instance, node, List.of(targetUser), userName);
        return null;
    }

    /**
     * 驳回至指定节点：流程不办结，打回给某个已走过的审批节点重新处理
     *
     * <p>并行流程下驳回会作废全部并行分支（token 状态重置为单 token 从目标节点重新推进）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String rejectToNode(String userName, String taskId, String targetNodeKey, String opinion) {
        if (targetNodeKey == null || targetNodeKey.isBlank()) {
            return "请选择驳回目标节点！";
        }
        BpmTask task = requireTask(taskId);
        if (!userName.equals(task.getAssignee())) {
            return "无权处理该审批任务！";
        }
        if (targetNodeKey.equals(task.getNodeKey())) {
            return "驳回目标不能是当前节点！";
        }
        int rows = claimTask(task.getId());
        if (rows == 0) {
            return "该审批任务已被处理！";
        }
        // 实例级行锁（防与并行分支上的并发审批交错）
        instanceMapper.selectByIdForUpdate(task.getInstanceId());
        task.setStatus(TASK_HANDLED);
        task.setResult(2);
        task.setOpinion(opinion);
        task.setHandleTime(LocalDateTime.now());
        taskMapper.updateById(task);

        BpmInstance instance = requireInstance(task.getInstanceId());
        if (instance.getStatus() == null || instance.getStatus() != INSTANCE_RUNNING) {
            return "流程已办结，无法驳回！";
        }
        DiagramModel diagram = loadSnapshot(instance);
        DiagramModel.NodeModel target = nodeOf(diagram, targetNodeKey);
        if (target == null || target.getType() != 1) {
            return "驳回目标节点不存在或不是审批节点！";
        }
        // 目标必须是本实例已产生过任务的节点（已走过）
        Long visited = taskMapper.selectCount(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instance.getId())
                .eq(BpmTask::getNodeKey, targetNodeKey));
        if (visited == null || visited == 0) {
            return "驳回目标节点尚未走过，无法驳回！";
        }

        addRecord(instance.getId(), task.getNodeKey(), task.getNodeName(), userName, 3,
                (opinion == null || opinion.isBlank() ? "" : opinion + "；") + "驳回至：" + target.getName());
        closeTodo(task.getId());
        // 清理当前及其余待处理任务与待办
        voidPendingTasks(instance.getId());

        // 目标节点重新解析审批人并生成任务（自选审批人不再复用发起时的选择，为空时兜底 admin）
        List<String> assignees = assigneeResolver.resolve(diagram, targetNodeKey, instance.getApplyUser(),
                null, expressionVars(instance));
        if (assignees.isEmpty()) {
            log.warn("流程实例[{}]驳回至节点[{}]审批人解析为空，兜底 admin", instance.getId(), targetNodeKey);
            assignees = List.of("admin");
        }
        instance.setStatus(INSTANCE_RUNNING);
        instance.setCurrentNodeKey(targetNodeKey);
        instance.setUpdateBy(userName);
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        // 全部分支已作废，token 状态重置为单存活 token（停驻在目标节点等待审批，不继续推进）
        instance.setTokenState(null);
        saveTokenState(instance.getId(), new TokenState(), userName);
        createTasks(instance, target, assignees, userName);
        return null;
    }

    /**
     * 可驳回目标节点：本实例已走过（产生过任务）的审批节点，排除当前节点
     */
    public List<Map<String, String>> rejectTargets(String taskId) {
        BpmTask task = requireTask(taskId);
        BpmInstance instance = requireInstance(task.getInstanceId());
        DiagramModel diagram = loadSnapshot(instance);
        Set<String> visitedKeys = taskMapper.selectList(new LambdaQueryWrapper<BpmTask>()
                        .eq(BpmTask::getInstanceId, instance.getId())
                        .select(BpmTask::getNodeKey)).stream()
                .map(BpmTask::getNodeKey)
                .filter(k -> k != null && !k.equals(task.getNodeKey()))
                .collect(java.util.stream.Collectors.toSet());
        List<Map<String, String>> targets = new ArrayList<>();
        for (DiagramModel.NodeModel node : diagram.getNodes()) {
            if (node.getType() == 1 && visitedKeys.contains(node.getKey())) {
                targets.add(Map.of("nodeKey", node.getKey(), "nodeName",
                        node.getName() == null ? node.getKey() : node.getName()));
            }
        }
        return targets;
    }

    /**
     * 催办：发起人提醒当前待处理审批人（WebSocket 推送 + 审批记录留痕）
     */
    @Transactional(rollbackFor = Exception.class)
    public String urge(String userName, String instanceId) {
        BpmInstance instance = requireInstance(instanceId);
        if (!userName.equals(instance.getApplyUser())) {
            return "仅发起人可催办！";
        }
        if (instance.getStatus() != INSTANCE_RUNNING) {
            return "流程已办结，无需催办！";
        }
        List<BpmTask> pendings = taskMapper.selectList(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instanceId)
                .eq(BpmTask::getStatus, TASK_PENDING));
        if (pendings == null || pendings.isEmpty()) {
            return "当前没有待处理的审批任务！";
        }
        String title = instance.getBusinessSummary() == null
                ? instance.getDefinitionName() : instance.getBusinessSummary();
        pendings.stream().map(BpmTask::getAssignee).distinct().forEach(assignee -> {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "bpmUrge");
            message.put("userName", assignee);
            message.put("title", "【催办】" + title);
            WebSocketServer.sendMessage(assignee, JsonUtils.toJson(message));
        });
        String nodeName = pendings.get(0).getNodeName();
        addRecord(instanceId, pendings.get(0).getNodeKey(), nodeName, userName, 7, "发起人催办");
        return null;
    }

    // ==================== 内部方法 ====================

    /**
     * 发起：创建实例并从开始节点推进（由 BpmInstanceService 组装后调用引擎）
     */
    @Transactional(rollbackFor = Exception.class)
    public BpmInstance createAndStart(BpmInstance instance, String bpmnXml, String userName,
                                     Map<String, List<String>> chosenAssignees) {
        DiagramModel diagram;
        try {
            diagram = BpmnDiagramParser.parse(bpmnXml);
            String error = BusinessProcessDiagramValidator.validate(diagram);
            if (error != null) {
                throw new IllegalArgumentException(error);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("流程图解析失败：" + e.getMessage());
        }
        instance.setGraphSnapshot(JsonUtils.toJson(diagram));
        instance.setStatus(INSTANCE_RUNNING);
        instance.setApplyUser(userName);
        instance.setCreateBy(userName);
        instance.setCreateTime(LocalDateTime.now());
        instanceMapper.insert(instance);

        addRecord(instance.getId(), null, null, userName, 1, null);
        // 从开始节点推进（token 状态默认单存活 token）
        String startKey = diagram.getNodes().stream()
                .filter(n -> n.getType() == 2).findFirst()
                .map(DiagramModel.NodeModel::getKey).orElse(null);
        advance(instance, diagram, startKey, userName, chosenAssignees);
        BpmBusinessHandler handler = handlerRegistry.get(instance.getBusinessType());
        if (handler != null) {
            handler.onApply(instance);
        }
        return instance;
    }

    /**
     * 推进 token：从 fromNodeKey 出发走完当前分支直至停驻
     *
     * <p>并行网关分叉时分裂出多个分支 token（递归 walk），汇聚网关按入边数计数等齐后放行；
     * 分支到达终点时 token 消亡，全部 token 消亡后实例办结。
     * token 状态在 walk 结束后按实例最新状态持久化。</p>
     */
    private void advance(BpmInstance instance, DiagramModel diagram, String fromNodeKey,
                         String operator, Map<String, List<String>> chosenAssignees) {
        TokenState ts = loadTokenState(instance);
        walk(instance, diagram, fromNodeKey, operator, chosenAssignees, ts);
        if (instance.getStatus() != null && instance.getStatus() == INSTANCE_RUNNING) {
            saveTokenState(instance.getId(), ts, operator);
        } else {
            // 已办结（complete 内部清理过一次，此处兜底确保清空）
            clearTokenState(instance.getId());
        }
    }

    /**
     * 单个分支 token 推进：从 fromNodeKey 沿出边前进，直到停驻（用户任务/汇聚等待/分支消亡）
     */
    private void walk(BpmInstance instance, DiagramModel diagram, String fromNodeKey,
                      String operator, Map<String, List<String>> chosenAssignees, TokenState ts) {
        String current = fromNodeKey;
        int guard = 0;
        while (guard++ < 200) {
            DiagramModel.NodeModel cur = nodeOf(diagram, current);
            if (cur == null) {
                branchEnd(instance, operator, ts);
                return;
            }
            int curType = cur.getType() == null ? 0 : cur.getType();
            // 并行网关：分叉 / 汇聚 / 直通（形态由保存校验保证：不得同时多入多出）
            if (curType == 6) {
                List<DiagramModel.FlowModel> outs = flowsFrom(diagram, current);
                int ins = countInFlows(diagram, current);
                if (outs.isEmpty()) {
                    branchEnd(instance, operator, ts);
                    return;
                }
                if (ins > 1) {
                    // 汇聚：分支 token 等齐所有入边后放行（吸收 ins-1 个 token）
                    int arrived = ts.arrivals.merge(current, 1, Integer::sum);
                    if (arrived < ins) {
                        log.debug("流程实例[{}]并行汇聚[{}]等待分支 {}/{}", instance.getId(), current, arrived, ins);
                        return;
                    }
                    ts.arrivals.remove(current);
                    ts.active -= (ins - 1);
                    if (ts.active <= 0) {
                        complete(instance, operator);
                        return;
                    }
                    current = outs.get(0).getTo();
                    continue;
                }
                if (outs.size() > 1) {
                    // 分叉：1 token -> N tokens，其余分支递归推进
                    ts.active += outs.size() - 1;
                    for (int i = 1; i < outs.size(); i++) {
                        walk(instance, diagram, outs.get(i).getTo(), operator, chosenAssignees, ts);
                    }
                    if (ts.active <= 0) {
                        complete(instance, operator);
                        return;
                    }
                    current = outs.get(0).getTo();
                    continue;
                }
                current = outs.get(0).getTo();
                continue;
            }
            List<DiagramModel.FlowModel> outs = flowsFrom(diagram, current);
            if (outs.isEmpty()) {
                // 悬空节点（保存校验已拦截，此处兜底）：分支视为到终点
                branchEnd(instance, operator, ts);
                return;
            }
            String next;
            if (curType == 4) {
                // 排他网关：按条件选边，兜底 default
                next = pickGatewayOut(instance, diagram, outs);
            } else {
                next = outs.get(0).getTo();
            }
            DiagramModel.NodeModel target = nodeOf(diagram, next);
            if (target == null) {
                branchEnd(instance, operator, ts);
                return;
            }
            int targetType = target.getType() == null ? 0 : target.getType();
            switch (targetType) {
                case 1 -> {
                    // 用户任务：解析审批人 → 生成任务 + 待办 → 停止推进
                    List<String> assignees = assigneeResolver.resolve(diagram, target.getKey(),
                            instance.getApplyUser(), chosenAssignees, expressionVars(instance));
                    if (assignees.isEmpty()) {
                        log.warn("流程实例[{}]节点[{}]审批人解析为空，兜底 admin", instance.getId(), target.getKey());
                        assignees = List.of("admin");
                    }
                    instance.setCurrentNodeKey(target.getKey());
                    instance.setUpdateBy(operator);
                    instance.setUpdateTime(LocalDateTime.now());
                    instanceMapper.updateById(instance);
                    createTasks(instance, target, assignees, operator);
                    return;
                }
                case 3 -> {
                    // 结束节点：分支 token 消亡（全部消亡时办结）
                    branchEnd(instance, operator, ts);
                    return;
                }
                case 5 -> {
                    // 抄送节点：写记录 + 推送知会，不生成任务，继续推进
                    handleCc(instance, target, operator);
                    current = target.getKey();
                }
                case 4, 6 -> current = target.getKey();
                default -> current = target.getKey();
            }
        }
        // 防死循环护栏（理论不可达，校验器已拦截环路）
        log.error("流程实例[{}]推进超过循环护栏，强制办结为通过", instance.getId());
        complete(instance, operator);
    }

    /**
     * 分支 token 到达终点：消亡一个 token，全部消亡时办结实例
     */
    private void branchEnd(BpmInstance instance, String operator, TokenState ts) {
        ts.active--;
        if (ts.active <= 0) {
            complete(instance, operator);
        }
    }

    /** 加载实例 token 状态（历史实例/发起时无状态 -> 单存活 token） */
    private TokenState loadTokenState(BpmInstance instance) {
        if (instance.getTokenState() == null || instance.getTokenState().isBlank()) {
            return new TokenState();
        }
        try {
            TokenState ts = JsonUtils.fromJson(instance.getTokenState(), TokenState.class);
            if (ts == null || ts.arrivals == null) {
                return new TokenState();
            }
            return ts;
        } catch (Exception e) {
            log.warn("流程实例[{}]token 状态解析失败，重置为单 token：{}", instance.getId(), e.getMessage());
            return new TokenState();
        }
    }

    /** 持久化 token 状态（updateById 忽略 null 字段，必须显式 set） */
    private void saveTokenState(String instanceId, TokenState ts, String operator) {
        instanceMapper.update(null, new LambdaUpdateWrapper<BpmInstance>()
                .eq(BpmInstance::getId, instanceId)
                .set(BpmInstance::getTokenState, JsonUtils.toJson(ts))
                .set(BpmInstance::getUpdateBy, operator)
                .set(BpmInstance::getUpdateTime, LocalDateTime.now()));
    }

    /** 清空 token 状态（办结/驳回/撤回后清理） */
    private void clearTokenState(String instanceId) {
        instanceMapper.update(null, new LambdaUpdateWrapper<BpmInstance>()
                .eq(BpmInstance::getId, instanceId)
                .set(BpmInstance::getTokenState, null));
    }

    /**
     * 排他网关选边：按边序取第一条条件成立者；全不满足走 default；无 default 报错
     */
    private String pickGatewayOut(BpmInstance instance, DiagramModel diagram, List<DiagramModel.FlowModel> outs) {
        Map<String, Object> vars = expressionVars(instance);
        DiagramModel.FlowModel fallback = null;
        for (DiagramModel.FlowModel flow : outs) {
            if (flow.isDefaultFlow()) {
                fallback = flow;
                continue;
            }
            if (flow.getCondition() != null && !flow.getCondition().isBlank()
                    && exprEvaluator.evaluate(flow.getCondition(), vars)) {
                return flow.getTo();
            }
        }
        if (fallback != null) {
            return fallback.getTo();
        }
        throw new IllegalStateException("流程网关无可走路径（条件均不成立且未设置默认流）");
    }

    private void createTasks(BpmInstance instance, DiagramModel.NodeModel node,
                             List<String> assignees, String operator) {
        LocalDateTime now = LocalDateTime.now();
        for (String assignee : assignees) {
            BpmTask task = new BpmTask();
            task.setInstanceId(instance.getId());
            task.setNodeKey(node.getKey());
            task.setNodeName(node.getName());
            task.setAssignee(assignee);
            task.setStatus(TASK_PENDING);
            task.setCreateTime(now);
            taskMapper.insert(task);
            // 同步 sys_todo：content 用任务唯一引用，处理完成后精确闭环
            SysTodo todo = new SysTodo();
            todo.setUserName(assignee);
            todo.setTitle(instance.getBusinessSummary() == null
                    ? instance.getDefinitionName() : instance.getBusinessSummary());
            todo.setContent("审批任务:" + task.getId());
            todo.setPriority(2);
            todo.setStatus(0);
            todo.setCreateBy(operator);
            todo.setCreateTime(now);
            sysTodoService.save(todo);
            // 实时推送（非数组 JSON 载荷，现有通知角标处理器会安全忽略）
            Map<String, Object> message = new HashMap<>();
            message.put("type", "bpmTask");
            message.put("userName", assignee);
            message.put("title", todo.getTitle());
            WebSocketServer.sendMessage(assignee, JsonUtils.toJson(message));
        }
    }

    /**
     * 抄送节点处理：解析抄送对象 → 写审批记录（action=6）+ WebSocket 知会，不阻塞推进
     */
    private void handleCc(BpmInstance instance, DiagramModel.NodeModel node, String operator) {
        List<String> targets = assigneeResolver.resolve(loadSnapshot(instance), node.getKey(),
                instance.getApplyUser(), null, null);
        if (targets.isEmpty()) {
            log.warn("流程实例[{}]抄送节点[{}]对象解析为空，已跳过", instance.getId(), node.getKey());
            return;
        }
        String title = instance.getBusinessSummary() == null
                ? instance.getDefinitionName() : instance.getBusinessSummary();
        for (String target : targets) {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "bpmCc");
            message.put("userName", target);
            message.put("title", "【抄送】" + title);
            WebSocketServer.sendMessage(target, JsonUtils.toJson(message));
        }
        addRecord(instance.getId(), node.getKey(), node.getName(), operator, 6,
                "抄送给：" + String.join("、", targets));
    }

    private void complete(BpmInstance instance, String operator) {
        // 防重复办结（并行分支消亡路径可能多次触发）
        if (instance.getStatus() != null && instance.getStatus() == INSTANCE_APPROVED) {
            return;
        }
        instance.setStatus(INSTANCE_APPROVED);
        instance.setCurrentNodeKey(null);
        instance.setFinishTime(LocalDateTime.now());
        instance.setUpdateBy(operator);
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        voidPendingTasks(instance.getId());
        clearTokenState(instance.getId());
        BpmBusinessHandler handler = handlerRegistry.get(instance.getBusinessType());
        if (handler != null) {
            handler.onApproved(instance);
        }
    }

    private void voidPendingTasks(String instanceId) {
        List<BpmTask> pendings = taskMapper.selectList(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instanceId)
                .eq(BpmTask::getStatus, TASK_PENDING));
        for (BpmTask task : pendings) {
            task.setStatus(TASK_VOIDED);
            taskMapper.updateById(task);
            closeTodo(task.getId());
        }
    }

    private int claimTask(String taskId) {
        return taskMapper.update(null, new LambdaUpdateWrapper<BpmTask>()
                .eq(BpmTask::getId, taskId)
                .eq(BpmTask::getStatus, TASK_PENDING)
                .set(BpmTask::getStatus, TASK_HANDLED));
    }

    private void closeTodo(String taskId) {
        sysTodoService.update(new LambdaUpdateWrapper<SysTodo>()
                .eq(SysTodo::getUserName, sysTodoUserNameByTask(taskId))
                .eq(SysTodo::getContent, "审批任务:" + taskId)
                .eq(SysTodo::getStatus, 0)
                .set(SysTodo::getStatus, 1)
                .set(SysTodo::getFinishTime, LocalDateTime.now()));
    }

    private String sysTodoUserNameByTask(String taskId) {
        BpmTask task = taskMapper.selectById(taskId);
        return task == null ? null : task.getAssignee();
    }

    private Map<String, Object> expressionVars(BpmInstance instance) {
        Map<String, Object> vars = new HashMap<>();
        if (instance.getFormData() != null && !instance.getFormData().isBlank()) {
            try {
                Map<String, Object> form = JsonUtils.parseToMap(instance.getFormData());
                if (form != null) {
                    vars.putAll(form);
                }
            } catch (Exception e) {
                log.warn("流程实例[{}]表单快照解析失败：{}", instance.getId(), e.getMessage());
            }
        }
        vars.putIfAbsent("applyUser", instance.getApplyUser());
        vars.putIfAbsent("businessType", instance.getBusinessType());
        vars.putIfAbsent("businessId", instance.getBusinessId());
        return vars;
    }

    private int approvalMode(DiagramModel diagram, String nodeKey) {
        DiagramModel.NodeModel node = nodeOf(diagram, nodeKey);
        return node == null || node.getApprovalMode() == null ? 1 : node.getApprovalMode();
    }

    private DiagramModel loadSnapshot(BpmInstance instance) {
        try {
            return JsonUtils.fromJson(instance.getGraphSnapshot(), DiagramModel.class);
        } catch (Exception e) {
            throw new IllegalStateException("流程图快照解析失败：" + e.getMessage());
        }
    }

    private DiagramModel.NodeModel nodeOf(DiagramModel diagram, String key) {
        return diagram.getNodes().stream().filter(n -> key.equals(n.getKey())).findFirst().orElse(null);
    }

    private List<DiagramModel.FlowModel> flowsFrom(DiagramModel diagram, String nodeKey) {
        return diagram.getFlows().stream().filter(f -> nodeKey.equals(f.getFrom())).toList();
    }

    private int countInFlows(DiagramModel diagram, String nodeKey) {
        return (int) diagram.getFlows().stream().filter(f -> nodeKey.equals(f.getTo())).count();
    }

    private BpmTask requireTask(String taskId) {
        BpmTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("审批任务不存在！");
        }
        return task;
    }

    private BpmInstance requireInstance(String instanceId) {
        BpmInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在！");
        }
        return instance;
    }

    private void addRecord(String instanceId, String nodeKey, String nodeName,
                           String operator, int action, String opinion) {
        BpmRecord record = new BpmRecord();
        record.setInstanceId(instanceId);
        record.setNodeKey(nodeKey);
        record.setNodeName(nodeName);
        record.setOperator(operator);
        record.setAction(action);
        record.setOpinion(opinion);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
    }
}
