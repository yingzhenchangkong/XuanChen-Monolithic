package com.xuanchen.businessprocess.definition.parse;

import com.xuanchen.businessprocess.definition.model.DiagramModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * BPMN 2.0 XML 解析器（JDK 内置 DOM，零第三方依赖，离线构建友好）
 *
 * <p>仅识别白名单元素：startEvent / endEvent / userTask / sendTask / exclusiveGateway / sequenceFlow。
 * 自定义属性（xc 命名空间）：userTask 上 xc:approvalMode、xc:rules（JSON 数组）；
 * sendTask 上 xc:rules（抄送对象规则，仅指定用户/指定角色）；
 * 顺序流条件支持两种写法：xc:condition 属性，或标准 &lt;bpmn:conditionExpression&gt; 子元素。</p>
 *
 * <p>安全：禁用 DTD 与外部实体（XXE 防护）。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public class BpmnDiagramParser {

    /** 自定义扩展命名空间（与前端 bpmn-js moddle 扩展描述符一致） */
    public static final String XC_NS = "http://xuanchen.workbuddy.cn/schema/bpmn/xc";

    /**
     * 解析 BPMN XML → 图模型
     *
     * @param bpmnXml BPMN XML 字符串
     * @return 图模型
     * @throws Exception XML 格式错误或包含不支持的元素时抛出，异常消息面向用户
     */
    public static DiagramModel parse(String bpmnXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE 防护：禁 DTD、禁外部实体
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        Element process = findProcessElement(doc);
        DiagramModel model = new DiagramModel();
        List<String> defaultFlowIds = new ArrayList<>();
        NodeList children = process.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node item = children.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) item;
            String localName = el.getLocalName();
            String id = el.getAttribute("id");
            String name = el.getAttribute("name");
            switch (localName) {
                case "startEvent" -> {
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(isBlank(name) ? "开始" : name);
                    node.setType(2);
                    model.getNodes().add(node);
                }
                case "endEvent" -> {
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(isBlank(name) ? "结束" : name);
                    node.setType(3);
                    model.getNodes().add(node);
                }
                case "userTask" -> {
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(name);
                    node.setType(1);
                    String mode = attrNS(el, "approvalMode");
                    node.setApprovalMode("2".equals(mode) ? 2 : 1);
                    model.getNodes().add(node);
                    String rulesJson = attrNS(el, "rules");
                    if (!isBlank(rulesJson)) {
                        parseRules(model, id, rulesJson);
                    }
                }
                case "sendTask" -> {
                    // 抄送节点：仅知会不审批，规则仅允许 指定用户/指定角色
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(name);
                    node.setType(5);
                    model.getNodes().add(node);
                    String rulesJson = attrNS(el, "rules");
                    if (!isBlank(rulesJson)) {
                        parseRules(model, id, rulesJson);
                    }
                }
                case "exclusiveGateway" -> {
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(isBlank(name) ? "网关" : name);
                    node.setType(4);
                    model.getNodes().add(node);
                    String def = el.getAttribute("default");
                    if (!isBlank(def)) {
                        defaultFlowIds.add(def);
                    }
                }
                case "parallelGateway" -> {
                    DiagramModel.NodeModel node = new DiagramModel.NodeModel();
                    node.setKey(id);
                    node.setName(isBlank(name) ? "并行网关" : name);
                    node.setType(6);
                    model.getNodes().add(node);
                }
                case "sequenceFlow" -> {
                    DiagramModel.FlowModel flow = new DiagramModel.FlowModel();
                    flow.setKey(id);
                    flow.setFrom(el.getAttribute("sourceRef"));
                    flow.setTo(el.getAttribute("targetRef"));
                    flow.setName(name);
                    // 条件：优先标准 conditionExpression 子元素，其次 xc:condition 属性
                    String cond = readConditionExpression(el);
                    if (isBlank(cond)) {
                        cond = attrNS(el, "condition");
                    }
                    flow.setCondition(isBlank(cond) ? null : cond.trim());
                    model.getFlows().add(flow);
                }
                case "incoming", "outgoing", "extensionElements", "documentation",
                     "multiInstanceLoopCharacteristics", "laneSet" -> {
                    // 任务/网关的子元素或忽略项，跳过
                }
                default -> throw new IllegalArgumentException(
                        "流程包含不支持的元素：<" + localName + ">，仅允许 开始/结束/审批节点/抄送节点/排他网关/并行网关/连线");
            }
        }
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            flow.setDefaultFlow(defaultFlowIds.contains(flow.getKey()));
        }
        return model;
    }

    private static Element findProcessElement(Document doc) {
        NodeList list = doc.getElementsByTagNameNS("*", "process");
        if (list.getLength() == 0) {
            throw new IllegalArgumentException("XML 中未找到 BPMN process 元素！");
        }
        return (Element) list.item(0);
    }

    private static void parseRules(DiagramModel model, String nodeKey, String rulesJson) throws Exception {
        // 轻量解析 JSON 数组：[{"ruleType":1,"ruleValue":"a,b"},{"ruleType":2,"ruleValue":"roleCode"}]
        // 统一走 common JsonUtils（sb3/sb4 双端同源，避免直接依赖具体 databind 包名）
        var arr = com.xuanchen.common.utils.JsonUtils.parseToJsonNode(rulesJson);
        if (arr == null || !arr.isArray()) {
            throw new IllegalArgumentException("审批人规则格式错误（应为 JSON 数组）");
        }
        for (var item : arr) {
            var type = item.get("ruleType");
            var value = item.get("ruleValue");
            if (type == null || type.isNull()) {
                continue;
            }
            DiagramModel.RuleModel rule = new DiagramModel.RuleModel();
            rule.setNodeKey(nodeKey);
            rule.setRuleType(type.asInt());
            rule.setRuleValue(value == null ? null : value.asText());
            model.getRules().add(rule);
        }
    }

    private static String readConditionExpression(Element flowEl) {
        NodeList list = flowEl.getElementsByTagNameNS("*", "conditionExpression");
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return null;
    }

    /** 读取 xc 命名空间属性；带命名空间读不到时回退无命名空间（兼容手写 XML） */
    private static String attrNS(Element el, String localName) {
        String v = el.getAttributeNS(XC_NS, localName);
        return isBlank(v) ? el.getAttribute("xc:" + localName) : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
