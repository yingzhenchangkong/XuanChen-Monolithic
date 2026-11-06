package com.xuanchen.businessprocess.definition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.businessprocess.definition.entity.BpmDefinition;
import com.xuanchen.businessprocess.definition.service.IBpmDefinitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->审批流程定义（管理端，仅 admin）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/businessprocess/definition")
@RequiresRoles("admin")
public class BpmDefinitionController {

    @Autowired
    private IBpmDefinitionService definitionService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<IPage<BpmDefinition>> list(BpmDefinition query,
                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(definitionService.listPage(new Page<>(pageNo, pageSize), query));
    }

    /**
     * 新增（草稿状态）
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody BpmDefinition definition, HttpServletRequest request) {
        String error = validateBase(definition);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (definitionService.ifExistsCode(definition)) {
            return Result.badRequest("流程编码已存在！");
        }
        definition.setStatus(0);
        definition.setVersion(1);
        definition.setCreateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        definition.setCreateTime(LocalDateTime.now());
        definitionService.save(definition);
        return Result.success("添加成功!");
    }

    /**
     * 修改基本信息
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody BpmDefinition definition, HttpServletRequest request) {
        if (StringUtil.isEmpty(definition.getId())) {
            return Result.badRequest("缺少流程定义ID！");
        }
        String error = validateBase(definition);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (definitionService.ifExistsCode(definition)) {
            return Result.badRequest("流程编码已存在！");
        }
        BpmDefinition update = new BpmDefinition();
        update.setId(definition.getId());
        update.setName(definition.getName());
        update.setCode(definition.getCode());
        update.setBusinessType(definition.getBusinessType());
        update.setRemark(definition.getRemark());
        update.setOnlineFormId(definition.getOnlineFormId());
        update.setUpdateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        update.setUpdateTime(LocalDateTime.now());
        if (!definitionService.updateById(update)) {
            return Result.badRequest("流程定义不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 删除（已启用的先停用）
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        BpmDefinition definition = definitionService.getById(id);
        if (definition == null) {
            return Result.badRequest("流程定义不存在或已被删除！");
        }
        if (definition.getStatus() != null && definition.getStatus() == 1) {
            return Result.badRequest("流程已启用，请先停用再删除！");
        }
        definitionService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        for (String id : idList) {
            BpmDefinition definition = definitionService.getById(id);
            if (definition != null && definition.getStatus() != null && definition.getStatus() == 1) {
                return Result.badRequest("流程【" + definition.getName() + "】已启用，请先停用再删除！");
            }
        }
        definitionService.removeBatchByIds(idList);
        return Result.success("批量删除成功!");
    }

    /**
     * 启用/停用（启用前强制重新校验图模型）
     */
    @PostMapping("/changeStatus")
    public Result<String> changeStatus(@RequestBody BpmDefinition definition, HttpServletRequest request) {
        if (StringUtil.isEmpty(definition.getId()) || definition.getStatus() == null) {
            return Result.badRequest("缺少流程定义ID或目标状态！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = definitionService.changeStatus(definition.getId(), definition.getStatus(), userName);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success(definition.getStatus() == 1 ? "启用成功!" : "停用成功!");
    }

    /**
     * 保存设计器内容（BPMN XML），保存前服务端校验并解析落库
     */
    @PostMapping("/saveDiagram")
    public Result<String> saveDiagram(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String id = body.get("id");
        String bpmnXml = body.get("bpmnXml");
        if (StringUtil.isEmpty(id) || StringUtil.isEmpty(bpmnXml)) {
            return Result.badRequest("缺少流程定义ID或流程图内容！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        String error = definitionService.saveDiagram(id, bpmnXml, userName);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("保存成功!");
    }

    /**
     * 读取设计器内容（BPMN XML + 流程名称）
     */
    @GetMapping("/getDiagram")
    public Result<Map<String, String>> getDiagram(@RequestParam(name = "id") String id) {
        BpmDefinition definition = definitionService.getById(id);
        if (definition == null) {
            return Result.badRequest("流程定义不存在或已被删除！");
        }
        Map<String, String> data = new java.util.HashMap<>();
        data.put("bpmnXml", definition.getBpmnXml() == null ? "" : definition.getBpmnXml());
        data.put("name", definition.getName() == null ? "" : definition.getName());
        data.put("onlineFormId", definition.getOnlineFormId() == null ? "" : definition.getOnlineFormId());
        return Result.success(data);
    }

    /**
     * 绑定/解绑在线表单（流程设计器使用；onlineFormId 传空即解绑=不使用在线表单）
     */
    @PostMapping("/bindForm")
    public Result<String> bindForm(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String id = body.get("id");
        String onlineFormId = body.get("onlineFormId");
        if (StringUtil.isEmpty(id)) {
            return Result.badRequest("缺少流程定义ID！");
        }
        BpmDefinition update = new BpmDefinition();
        update.setId(id);
        update.setOnlineFormId(StringUtil.isEmpty(onlineFormId) ? "" : onlineFormId);
        update.setUpdateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        update.setUpdateTime(LocalDateTime.now());
        if (!definitionService.updateById(update)) {
            return Result.badRequest("流程定义不存在或已被删除！");
        }
        return Result.success(StringUtil.isEmpty(onlineFormId) ? "已解绑在线表单!" : "绑定在线表单成功!");
    }

    /**
     * 设计详情：节点/连线/规则列表
     */
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam(name = "id") String id) {
        return Result.success(definitionService.getDiagramDetail(id));
    }

    private String validateBase(BpmDefinition definition) {
        if (StringUtil.isEmpty(definition.getName())) {
            return "流程名称不能为空！";
        }
        if (StringUtil.isEmpty(definition.getCode())) {
            return "流程编码不能为空！";
        }
        if (StringUtil.isEmpty(definition.getBusinessType())) {
            return "业务类型不能为空！";
        }
        if (definition.getCode().length() > 64) {
            return "流程编码长度不能超过64！";
        }
        if (definition.getName().length() > 100) {
            return "流程名称长度不能超过100！";
        }
        return null;
    }
}
