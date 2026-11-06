package com.xuanchen.businessprocess.definition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.businessprocess.definition.entity.BpmDefinition;

import java.util.Map;

/**
 * Service-->审批流程定义
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface IBpmDefinitionService extends IService<BpmDefinition> {

    /**
     * 分页列表
     */
    IPage<BpmDefinition> listPage(IPage<BpmDefinition> page, BpmDefinition query);

    /**
     * 编码是否已存在（排除自身）
     */
    boolean ifExistsCode(BpmDefinition definition);

    /**
     * 保存设计器内容：校验 + 解析落库图模型 + 更新 bpmn_xml
     *
     * @return 错误原因；null 成功
     */
    String saveDiagram(String id, String bpmnXml, String userName);

    /**
     * 启用/停用；启用前强制重新校验图模型
     *
     * @return 错误原因；null 成功
     */
    String changeStatus(String id, Integer targetStatus, String userName);

    /**
     * 设计详情：节点/连线/规则列表（管理端查看）
     */
    Map<String, Object> getDiagramDetail(String definitionId);
}
