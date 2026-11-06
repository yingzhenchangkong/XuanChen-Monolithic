package com.xuanchen.businessprocess.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.businessprocess.definition.model.DiagramModel;
import com.xuanchen.businessprocess.support.entity.BpmDelegate;
import com.xuanchen.businessprocess.support.mapper.BpmDelegateMapper;
import com.xuanchen.businessprocess.support.mapper.BpmSupportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审批人解析器-->按节点规则解析审批人集合
 *
 * <p>多规则取并集；规则类型：1指定用户 2指定角色 3发起人自选 4部门负责人 5直属上级(第N级主管) 6表单字段指定；
 * ruleType=3 的审批人在发起时由前端传入 chosenAssignees；ruleType=6 从业务表单快照字段读取用户名（支持逗号分隔/数组）；
 * ruleType=4/5 依赖 md_dept.leader_user（部门负责人），5 沿部门树（parent_dept_code）向上找第 N 级部门负责人；
 * 解析结果按启用中的委托规则扩展：委托人名下任务同步派给被委托人（或签语义，双方均可处理）；
 * 负责人类/表单字段规则解析出的账号会被校验（不存在或停用则丢弃）；
 * 全部规则解析为空时由引擎兜底 admin 并记录告警日志。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Component
public class BpmAssigneeResolver {

    private static final Logger log = LoggerFactory.getLogger(BpmAssigneeResolver.class);

    @Autowired
    private BpmSupportMapper bpmSupportMapper;
    @Autowired
    private BpmDelegateMapper bpmDelegateMapper;

    /**
     * 解析节点审批人
     *
     * @param diagram         图模型（含规则）
     * @param nodeKey         节点标识
     * @param applyUser       发起人
     * @param chosenAssignees 发起人自选审批人（nodeKey -> 用户名列表），可为 null
     * @param formVars        业务表单快照变量（规则 6 用），可为 null
     * @return 审批人集合（去重、保序）；空集合表示需兜底
     */
    public List<String> resolve(DiagramModel diagram, String nodeKey, String applyUser,
                                Map<String, List<String>> chosenAssignees, Map<String, Object> formVars) {
        Set<String> result = new LinkedHashSet<>();
        for (DiagramModel.RuleModel rule : diagram.getRules()) {
            if (!nodeKey.equals(rule.getNodeKey()) || rule.getRuleType() == null) {
                continue;
            }
            switch (rule.getRuleType()) {
                case 1 -> {
                    // 指定用户：逗号分隔用户名
                    if (rule.getRuleValue() != null && !rule.getRuleValue().isBlank()) {
                        Arrays.stream(rule.getRuleValue().split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).forEach(result::add);
                    }
                }
                case 2 -> {
                    // 指定角色：角色编码 → 启用账号
                    if (rule.getRuleValue() != null && !rule.getRuleValue().isBlank()) {
                        List<String> users = bpmSupportMapper.listUserNamesByRoleCode(rule.getRuleValue().trim());
                        if (users != null) {
                            result.addAll(users);
                        }
                    }
                }
                case 3 -> {
                    // 发起人自选
                    if (chosenAssignees != null && chosenAssignees.get(nodeKey) != null) {
                        chosenAssignees.get(nodeKey).stream()
                                .filter(s -> s != null && !s.isBlank())
                                .map(String::trim).forEach(result::add);
                    }
                }
                case 4 -> {
                    // 部门负责人：规则值=部门编码（可空=发起人所在部门）
                    String deptCode = rule.getRuleValue() == null ? "" : rule.getRuleValue().trim();
                    if (deptCode.isEmpty()) {
                        resolveLeadersByLevel(applyUser, 1, result);
                    } else {
                        addEnabledLeader(bpmSupportMapper.findDeptLeaderUser(deptCode), result);
                    }
                }
                case 5 -> {
                    // 直属上级（第 N 级主管）：1=发起人所在部门负责人，2=上级部门负责人...
                    int level = 1;
                    if (rule.getRuleValue() != null && !rule.getRuleValue().isBlank()) {
                        try {
                            level = Integer.parseInt(rule.getRuleValue().trim());
                        } catch (NumberFormatException e) {
                            level = 1;
                        }
                    }
                    resolveLeadersByLevel(applyUser, Math.max(1, level), result);
                }
                case 6 -> {
                    // 表单字段指定审批人：规则值=formData 字段名，值支持 字符串(逗号分隔)/数组
                    if (rule.getRuleValue() != null && !rule.getRuleValue().isBlank()
                            && formVars != null && formVars.containsKey(rule.getRuleValue().trim())) {
                        Object v = formVars.get(rule.getRuleValue().trim());
                        for (String name : asUserNames(v)) {
                            if (bpmSupportMapper.countEnabledAccount(name) > 0) {
                                result.add(name);
                            }
                        }
                    }
                }
                default -> {
                    // 未知类型忽略
                }
            }
        }
        expandDelegates(result);
        return new ArrayList<>(result);
    }

    /**
     * 沿部门树向上找第 level 级部门的负责人（level=1 为发起人所在部门；多部门归属取并集）
     */
    private void resolveLeadersByLevel(String applyUser, int level, Set<String> result) {
        List<String> deptCodes = bpmSupportMapper.listPersonDeptCodes(applyUser);
        if (deptCodes == null || deptCodes.isEmpty()) {
            log.debug("审批人解析：账号[{}]未绑定人员或未分配部门，负责人类规则跳过", applyUser);
            return;
        }
        List<Map<String, Object>> all = bpmSupportMapper.listAllDepts();
        if (all == null || all.isEmpty()) {
            return;
        }
        Map<String, String> parentOf = new HashMap<>();
        Map<String, String> leaderOf = new HashMap<>();
        for (Map<String, Object> dept : all) {
            Object code = dept.get("deptCode");
            if (code == null) {
                continue;
            }
            parentOf.put(code.toString(), dept.get("parentDeptCode") == null ? null : dept.get("parentDeptCode").toString());
            leaderOf.put(code.toString(), dept.get("leaderUser") == null ? null : dept.get("leaderUser").toString());
        }
        for (String code : deptCodes) {
            String cur = code;
            for (int i = 1; i < level && cur != null; i++) {
                cur = parentOf.get(cur);
            }
            if (cur != null) {
                addEnabledLeader(leaderOf.get(cur), result);
            }
        }
    }

    /** 负责人加入结果集前校验账号存在且启用 */
    private void addEnabledLeader(String leader, Set<String> result) {
        if (leader == null || leader.isBlank()) {
            return;
        }
        String name = leader.trim();
        if (bpmSupportMapper.countEnabledAccount(name) > 0) {
            result.add(name);
        }
    }

    /** 表单字段值转用户名列表：String 按逗号拆分；List/数组逐项取 String */
    private List<String> asUserNames(Object value) {
        List<String> names = new ArrayList<>();
        if (value == null) {
            return names;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && !item.toString().isBlank()) {
                    names.add(item.toString().trim());
                }
            }
        } else {
            Arrays.stream(value.toString().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).forEach(names::add);
        }
        return names;
    }

    /**
     * 按启用中的委托规则扩展审批人：委托人名下任务同步派给被委托人（双方均可处理，或签语义）
     */
    private void expandDelegates(Set<String> assignees) {
        if (assignees.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<BpmDelegate> actives = bpmDelegateMapper.selectList(new LambdaQueryWrapper<BpmDelegate>()
                .eq(BpmDelegate::getStatus, 1)
                .in(BpmDelegate::getUserName, assignees)
                .and(w -> w.isNull(BpmDelegate::getStartTime).or().le(BpmDelegate::getStartTime, now))
                .and(w -> w.isNull(BpmDelegate::getEndTime).or().ge(BpmDelegate::getEndTime, now)));
        if (actives != null) {
            actives.forEach(d -> assignees.add(d.getDelegateUserName()));
        }
    }
}
