package com.xuanchen.businessprocess.definition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.businessprocess.definition.entity.BpmDefinition;
import com.xuanchen.businessprocess.definition.entity.BpmFlow;
import com.xuanchen.businessprocess.definition.entity.BpmNode;
import com.xuanchen.businessprocess.definition.entity.BpmNodeRule;
import com.xuanchen.businessprocess.definition.mapper.BpmDefinitionMapper;
import com.xuanchen.businessprocess.definition.mapper.BpmFlowMapper;
import com.xuanchen.businessprocess.definition.mapper.BpmNodeMapper;
import com.xuanchen.businessprocess.definition.mapper.BpmNodeRuleMapper;
import com.xuanchen.businessprocess.definition.model.DiagramModel;
import com.xuanchen.businessprocess.definition.parse.BpmnDiagramParser;
import com.xuanchen.businessprocess.definition.parse.BusinessProcessDiagramValidator;
import com.xuanchen.businessprocess.definition.service.IBpmDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service-->审批流程定义
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class BpmDefinitionServiceImpl extends ServiceImpl<BpmDefinitionMapper, BpmDefinition>
        implements IBpmDefinitionService {

    @Autowired
    private BpmNodeMapper nodeMapper;
    @Autowired
    private BpmFlowMapper flowMapper;
    @Autowired
    private BpmNodeRuleMapper ruleMapper;

    @Override
    public IPage<BpmDefinition> listPage(IPage<BpmDefinition> page, BpmDefinition query) {
        LambdaQueryWrapper<BpmDefinition> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtil.isNotEmpty(query.getName())) {
                wrapper.like(BpmDefinition::getName, query.getName());
            }
            if (StringUtil.isNotEmpty(query.getCode())) {
                wrapper.like(BpmDefinition::getCode, query.getCode());
            }
            if (StringUtil.isNotEmpty(query.getBusinessType())) {
                wrapper.eq(BpmDefinition::getBusinessType, query.getBusinessType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(BpmDefinition::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(BpmDefinition::getCreateTime);
        // 列表不返回 bpmn_xml 大字段
        wrapper.select(BpmDefinition.class, i -> !"bpmn_xml".equals(i.getColumn()));
        return this.page(page, wrapper);
    }

    @Override
    public boolean ifExistsCode(BpmDefinition definition) {
        LambdaQueryWrapper<BpmDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmDefinition::getCode, definition.getCode());
        if (StringUtil.isNotEmpty(definition.getId())) {
            wrapper.ne(BpmDefinition::getId, definition.getId());
        }
        return this.count(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDiagram(String id, String bpmnXml, String userName) {
        BpmDefinition definition = this.getById(id);
        if (definition == null) {
            return "流程定义不存在或已被删除！";
        }
        DiagramModel model;
        try {
            model = BpmnDiagramParser.parse(bpmnXml);
            String error = BusinessProcessDiagramValidator.validate(model);
            if (error != null) {
                return error;
            }
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "流程图解析失败：" + e.getMessage();
        }
        // 更新 XML
        BpmDefinition update = new BpmDefinition();
        update.setId(id);
        update.setBpmnXml(bpmnXml);
        update.setUpdateBy(userName);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);
        // 重建图模型表（配置数据，物理重建保证一致）
        nodeMapper.delete(new LambdaQueryWrapper<BpmNode>().eq(BpmNode::getDefinitionId, id));
        flowMapper.delete(new LambdaQueryWrapper<BpmFlow>().eq(BpmFlow::getDefinitionId, id));
        ruleMapper.delete(new LambdaQueryWrapper<BpmNodeRule>().eq(BpmNodeRule::getDefinitionId, id));
        int sort = 0;
        for (DiagramModel.NodeModel node : model.getNodes()) {
            BpmNode entity = new BpmNode();
            entity.setDefinitionId(id);
            entity.setNodeKey(node.getKey());
            entity.setNodeName(node.getName());
            entity.setNodeType(node.getType());
            entity.setApprovalMode(node.getApprovalMode());
            entity.setSortNo(sort++);
            nodeMapper.insert(entity);
        }
        sort = 0;
        for (DiagramModel.FlowModel flow : model.getFlows()) {
            BpmFlow entity = new BpmFlow();
            entity.setDefinitionId(id);
            entity.setFlowKey(flow.getKey());
            entity.setFromNodeKey(flow.getFrom());
            entity.setToNodeKey(flow.getTo());
            entity.setFlowName(flow.getName());
            entity.setConditionExpr(flow.getCondition());
            entity.setIsDefault(flow.isDefaultFlow() ? 1 : 0);
            entity.setSortNo(sort++);
            flowMapper.insert(entity);
        }
        sort = 0;
        for (DiagramModel.RuleModel rule : model.getRules()) {
            BpmNodeRule entity = new BpmNodeRule();
            entity.setDefinitionId(id);
            entity.setNodeKey(rule.getNodeKey());
            entity.setRuleType(rule.getRuleType());
            entity.setRuleValue(rule.getRuleValue());
            entity.setSortNo(sort++);
            ruleMapper.insert(entity);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String changeStatus(String id, Integer targetStatus, String userName) {
        BpmDefinition definition = this.getById(id);
        if (definition == null) {
            return "流程定义不存在或已被删除！";
        }
        if (targetStatus != null && targetStatus == 1) {
            // 启用前强制校验图模型
            if (StringUtil.isEmpty(definition.getBpmnXml())) {
                return "流程尚未设计（无流程图），不能启用！";
            }
            try {
                DiagramModel model = BpmnDiagramParser.parse(definition.getBpmnXml());
                String error = BusinessProcessDiagramValidator.validate(model);
                if (error != null) {
                    return error;
                }
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            } catch (Exception e) {
                return "流程图解析失败：" + e.getMessage();
            }
            BpmDefinition update = new BpmDefinition();
            update.setId(id);
            update.setStatus(1);
            update.setUpdateBy(userName);
            update.setUpdateTime(LocalDateTime.now());
            this.updateById(update);
            return null;
        }
        BpmDefinition update = new BpmDefinition();
        update.setId(id);
        update.setStatus(2);
        update.setUpdateBy(userName);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);
        return null;
    }

    @Override
    public Map<String, Object> getDiagramDetail(String definitionId) {
        Map<String, Object> data = new HashMap<>();
        List<BpmNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<BpmNode>()
                .eq(BpmNode::getDefinitionId, definitionId).orderByAsc(BpmNode::getSortNo));
        List<BpmFlow> flows = flowMapper.selectList(new LambdaQueryWrapper<BpmFlow>()
                .eq(BpmFlow::getDefinitionId, definitionId).orderByAsc(BpmFlow::getSortNo));
        List<BpmNodeRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<BpmNodeRule>()
                .eq(BpmNodeRule::getDefinitionId, definitionId).orderByAsc(BpmNodeRule::getSortNo));
        data.put("nodes", nodes == null ? new ArrayList<>() : nodes);
        data.put("flows", flows == null ? new ArrayList<>() : flows);
        data.put("rules", rules == null ? new ArrayList<>() : rules);
        return data;
    }
}
