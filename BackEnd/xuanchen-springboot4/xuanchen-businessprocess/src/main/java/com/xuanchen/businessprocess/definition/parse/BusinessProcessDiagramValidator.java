package com.xuanchen.businessprocess.definition.parse;

import com.xuanchen.businessprocess.definition.model.DiagramModel;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 图模型校验器-->流程设计保存前的结构校验
 *
 * <p>校验项：唯一开始/可达结束、规则必配、排他网关条件覆盖、抄送节点恰 1 出边、
 * 并行网关分叉/汇聚形态（同一网关不得同时多入多出）。</p>

 * @author XuanChen
 * @date 2026-10-23
 */
public final class BusinessProcessDiagramValidator {

    private BusinessProcessDiagramValidator() {
    }

    /**
     * 校验图模型，不通过时返回错误原因；通过返回 null
     */
    public static String validate(DiagramModel model) {
        if (model.getNodes().isEmpty()) {
            return "流程为空，请先绘制流程图！";
        }
        List<DiagramModel.NodeModel> starts = filterByType(model, 2);
        List<DiagramModel.NodeModel> ends = filterByType(model, 3);
        if (starts.size() != 1) {
            return "流程必须有且仅有 1 个开始节点！";
        }
        if (ends.isEmpty()) {
            return "流程至少需要 1 个结束节点！";
        }
        Map<String, DiagramModel.NodeModel> nodeMap = new HashMap<>();
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (nodeMap.containsKey(node.getKey())) {
                return "节点标识重复：" + node.getKey();
            }
            nodeMap.put(node.getKey(), node);
        }
        // 连线引用完整性
        Set<String> flowKeys = new HashSet<>();
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (flowKeys.contains(flow.getKey())) {
                return "连线标识重复：" + flow.getKey();
            }
            flowKeys.add(flow.getKey());
            if (!nodeMap.containsKey(flow.getFrom())) {
                return "连线 " + flow.getKey() + " 的源节点不存在！";
            }
            if (!nodeMap.containsKey(flow.getTo())) {
                return "连线 " + flow.getKey() + " 的目标节点不存在！";
            }
        }
        String startKey = starts.get(0).getKey();
        // 开始节点不能有入边
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (startKey.equals(flow.getTo())) {
                return "开始节点不能有入边！";
            }
        }
        // 审批节点校验：必须配审批方式 + 至少一条规则；抄送节点：至少一条规则（仅指定用户/角色）
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (node.getType() == 1) {
                if (node.getApprovalMode() == null) {
                    node.setApprovalMode(1);
                }
            }
            if (node.getType() != 1 && node.getType() != 5) {
                continue;
            }
            boolean hasRule = model.getRules().stream().anyMatch(r -> node.getKey().equals(r.getNodeKey()));
            if (!hasRule) {
                return (node.getType() == 1 ? "审批节点" : "抄送节点") + "【" + safeName(node) + "】未配置"
                        + (node.getType() == 1 ? "审批人" : "抄送对象") + "规则！";
            }
            for (DiagramModel.RuleModel rule : model.getRules()) {
                if (!node.getKey().equals(rule.getNodeKey())) {
                    continue;
                }
                if (node.getType() == 5) {
                    if (rule.getRuleType() == null || rule.getRuleType() < 1 || rule.getRuleType() > 2) {
                        return "抄送节点【" + safeName(node) + "】仅支持 指定用户/指定角色 规则！";
                    }
                } else if (rule.getRuleType() == null || rule.getRuleType() < 1 || rule.getRuleType() > 6) {
                    return "审批节点【" + safeName(node) + "】的审批人规则类型不合法！";
                }
                // 规则值：3发起人自选、4部门负责人（可空=发起人所在部门）可不填，其余必填
                boolean valueOptional = rule.getRuleType() != null
                        && (rule.getRuleType() == 3 || rule.getRuleType() == 4);
                if (!valueOptional && (rule.getRuleValue() == null || rule.getRuleValue().isBlank())) {
                    return (node.getType() == 1 ? "审批节点" : "抄送节点") + "【" + safeName(node)
                            + "】的规则值不能为空！";
                }
                if (rule.getRuleType() != null && rule.getRuleType() == 5) {
                    // 直属上级：规则值 = 级别数字（1=本部门负责人，2=上级部门负责人...），留空默认 1
                    if (rule.getRuleValue() != null && !rule.getRuleValue().isBlank()) {
                        try {
                            int level = Integer.parseInt(rule.getRuleValue().trim());
                            if (level < 1 || level > 10) {
                                return "审批节点【" + safeName(node) + "】的直属上级级别需为 1~10 的数字！";
                            }
                        } catch (NumberFormatException e) {
                            return "审批节点【" + safeName(node) + "】的直属上级级别需为数字！";
                        }
                    }
                }
            }
        }
        // 网关出边校验：有条件或标记默认；非网关节点不允许 default 流
        Set<String> gatewayKeys = new HashSet<>();
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (node.getType() == 4) {
                gatewayKeys.add(node.getKey());
            }
            if (node.getType() == 5) {
                List<DiagramModel.FlowModel> outs = outFlows(model, node.getKey());
                if (outs.size() != 1) {
                    return "抄送节点【" + safeName(node) + "】必须有且仅有 1 条出边！";
                }
            }
        }
        Set<String> declaredDefault = new HashSet<>();
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (flow.isDefaultFlow()) {
                if (!gatewayKeys.contains(flow.getFrom())) {
                    return "仅排他网关的出边可设为默认流！";
                }
                declaredDefault.add(flow.getFrom());
            }
        }
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (node.getType() != 4) {
                continue;
            }
            List<DiagramModel.FlowModel> outs = outFlows(model, node.getKey());
            if (outs.isEmpty()) {
                return "网关【" + safeName(node) + "】没有出边！";
            }
            int conditional = 0;
            for (DiagramModel.FlowModel out : outs) {
                if (out.getCondition() != null && !out.getCondition().isBlank()) {
                    conditional++;
                }
            }
            boolean hasDefault = declaredDefault.contains(node.getKey());
            if (conditional == 0 && !hasDefault) {
                return "网关【" + safeName(node) + "】的出边至少需要一条条件表达式或默认流！";
            }
        }
        // 并行网关：分叉（1入N出）或汇聚（N入1出），不得同时多入多出；出边不允许配条件/默认流
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (node.getType() != 6) {
                continue;
            }
            List<DiagramModel.FlowModel> outs = outFlows(model, node.getKey());
            List<DiagramModel.FlowModel> ins = inFlows(model, node.getKey());
            if (outs.isEmpty()) {
                return "并行网关【" + safeName(node) + "】没有出边！";
            }
            if (ins.isEmpty()) {
                return "并行网关【" + safeName(node) + "】没有入边！";
            }
            if (ins.size() > 1 && outs.size() > 1) {
                return "并行网关【" + safeName(node) + "】不能同时作为分叉与汇聚，请拆成两个网关！";
            }
            for (DiagramModel.FlowModel out : outs) {
                if (out.getCondition() != null && !out.getCondition().isBlank()) {
                    return "并行网关【" + safeName(node) + "】的出边不需要条件表达式（所有分支同时执行）！";
                }
            }
            if (declaredDefault.contains(node.getKey())) {
                return "并行网关【" + safeName(node) + "】的出边不能设为默认流！";
            }
        }
        // 可达性：开始可达所有节点；所有节点可达结束（入边反向 BFS）
        Set<String> forward = reachable(model, startKey, true);
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (!forward.contains(node.getKey())) {
                return "节点【" + safeName(node) + "】无法从开始节点到达！";
            }
        }
        Set<String> backward = new HashSet<>();
        for (DiagramModel.NodeModel end : ends) {
            backward.addAll(reachable(model, end.getKey(), false));
        }
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (!backward.contains(node.getKey())) {
                return "节点【" + safeName(node) + "】无法到达任何结束节点！";
            }
        }
        // 条件表达式语法校验
        SpelExpressionParser parser = new SpelExpressionParser();
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (flow.getCondition() == null || flow.getCondition().isBlank()) {
                continue;
            }
            try {
                Expression expr = parser.parseExpression(flow.getCondition());
                // 用空表单上下文做一次只读求值测试（表达式引用的变量缺失时返回 null，属合法——运行时兜底 false）
                SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
                expr.getValue(context, new HashMap<>());
            } catch (Exception e) {
                return "连线【" + (flow.getName() == null || flow.getName().isBlank()
                        ? flow.getKey() : flow.getName()) + "】条件表达式语法错误：" + e.getMessage();
            }
        }
        return null;
    }

    /** BFS 可达集合；forward=true 沿出边，false 沿入边 */
    private static Set<String> reachable(DiagramModel model, String from, boolean forward) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(from);
        visited.add(from);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (DiagramModel.FlowModel flow : model.getFlows()) {
                String target;
                if (forward && cur.equals(flow.getFrom())) {
                    target = flow.getTo();
                } else if (!forward && cur.equals(flow.getTo())) {
                    target = flow.getFrom();
                } else {
                    continue;
                }
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }
        return visited;
    }

    private static List<DiagramModel.FlowModel> outFlows(DiagramModel model, String nodeKey) {
        List<DiagramModel.FlowModel> list = new ArrayList<>();
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (nodeKey.equals(flow.getFrom())) {
                list.add(flow);
            }
        }
        return list;
    }

    private static List<DiagramModel.FlowModel> inFlows(DiagramModel model, String nodeKey) {
        List<DiagramModel.FlowModel> list = new ArrayList<>();
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            if (nodeKey.equals(flow.getTo())) {
                list.add(flow);
            }
        }
        return list;
    }

    private static List<DiagramModel.NodeModel> filterByType(DiagramModel model, int type) {
        List<DiagramModel.NodeModel> list = new ArrayList<>();
        for (DiagramModel.NodeModel node : model.getNodes()) {
            if (node.getType() == type) {
                list.add(node);
            }
        }
        return list;
    }

    private static String safeName(DiagramModel.NodeModel node) {
        return node.getName() == null || node.getName().isBlank() ? node.getKey() : node.getName();
    }
}
