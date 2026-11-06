package com.xuanchen.businessprocess.definition.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 图模型-->BPMN 解析结果（引擎执行模型）
 *
 * <p>设计保存时解析 XML 得到；发起申请时序列化为 JSON 冻结进 bpm_instance.graph_snapshot，
 * 引擎按快照推进，在途实例不受定义后续修改影响。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class DiagramModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点集合
     */
    private List<NodeModel> nodes = new ArrayList<>();

    /**
     * 连线集合（顺序流）
     */
    private List<FlowModel> flows = new ArrayList<>();

    /**
     * 审批人规则集合
     */
    private List<RuleModel> rules = new ArrayList<>();

    /**
     * 节点
     */
    @Data
    public static class NodeModel implements Serializable {
        private static final long serialVersionUID = 1L;

        /** BPMN 元素 id */
        private String key;
        /** 名称 */
        private String name;
        /** 节点类型：1用户任务 2开始 3结束 4排他网关 5抄送 6并行网关 */
        private Integer type;
        /** 审批方式：1或签 2会签（仅用户任务） */
        private Integer approvalMode;
    }

    /**
     * 连线
     */
    @Data
    public static class FlowModel implements Serializable {
        private static final long serialVersionUID = 1L;

        /** BPMN 元素 id */
        private String key;
        /** 源节点 id */
        private String from;
        /** 目标节点 id */
        private String to;
        /** 名称 */
        private String name;
        /** 条件表达式（网关出边） */
        private String condition;
        /** 是否网关默认流 */
        private boolean defaultFlow;
    }

    /**
     * 审批人规则
     */
    @Data
    public static class RuleModel implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 所属节点 id */
        private String nodeKey;
        /** 规则类型：1指定用户 2指定角色 3发起人自选 4部门负责人 5直属上级(第N级主管) 6表单字段指定 */
        private Integer ruleType;
        /** 规则值（用户名逗号分隔/角色编码/部门编码(可空=发起人所在部门)/级别数字/表单字段名） */
        private String ruleValue;
    }
}
