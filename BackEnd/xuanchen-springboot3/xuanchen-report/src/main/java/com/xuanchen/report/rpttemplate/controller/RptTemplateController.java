package com.xuanchen.report.rpttemplate.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rptdataset.entity.RptDataset;
import com.xuanchen.report.rptdataset.service.IRptDatasetService;
import com.xuanchen.report.rpttemplate.entity.RptTemplate;
import com.xuanchen.report.rpttemplate.service.IRptTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->报表模板（模板设计）
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequiresRoles("admin")
@RequestMapping("/report/template")
public class RptTemplateController {
    @Autowired
    private IRptTemplateService rptTemplateService;
    @Autowired
    private IRptDatasetService rptDatasetService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页列表（管理）
     */
    @GetMapping("/list")
    public Result<IPage<RptTemplate>> list(RptTemplate rptTemplate,
                                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<RptTemplate> page = new Page<>(pageNo, pageSize);
        return Result.success(rptTemplateService.listTemplate(page, rptTemplate));
    }

    /**
     * 启用中的模板（报表新增时的「模板」下拉，不返回 configJson 避免大字段传输）
     */
    @GetMapping("/all")
    public Result<List<RptTemplate>> all() {
        List<RptTemplate> list = rptTemplateService.listEnabled();
        list.forEach(item -> item.setConfigJson(null));
        return Result.success(list);
    }

    /**
     * 新增模板（基本信息；设计内容由设计器单独保存）
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody RptTemplate rptTemplate, HttpServletRequest request) {
        String error = validateBase(rptTemplate);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (rptTemplateService.ifExistsCode(rptTemplate)) {
            return Result.badRequest("模板编码已存在！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        rptTemplate.setCreateBy(userName);
        rptTemplate.setCreateTime(LocalDateTime.now());
        rptTemplateService.save(rptTemplate);
        return Result.success("添加成功!");
    }

    /**
     * 修改模板（基本信息；编码不可改）
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody RptTemplate rptTemplate) {
        if (StringUtil.isEmpty(rptTemplate.getId())) {
            return Result.badRequest("缺少模板ID！");
        }
        String error = validateBase(rptTemplate);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (rptTemplateService.ifExistsCode(rptTemplate)) {
            return Result.badRequest("模板编码已存在！");
        }
        RptTemplate update = new RptTemplate();
        update.setId(rptTemplate.getId());
        update.setTemplateName(rptTemplate.getTemplateName());
        update.setTemplateType(rptTemplate.getTemplateType());
        update.setStatus(rptTemplate.getStatus());
        update.setRemark(rptTemplate.getRemark());
        if (!rptTemplateService.updateById(update)) {
            return Result.badRequest("模板不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 启用/停用模板（状态流转只在模板上做：报表列表已取消发布/停用）
     */
    @PostMapping("/changeStatus")
    public Result<String> changeStatus(@RequestBody RptTemplate rptTemplate) {
        if (StringUtil.isEmpty(rptTemplate.getId())) {
            return Result.badRequest("缺少模板ID！");
        }
        if (rptTemplate.getStatus() == null) {
            return Result.badRequest("缺少状态值！");
        }
        RptTemplate update = new RptTemplate();
        update.setId(rptTemplate.getId());
        update.setStatus(rptTemplate.getStatus());
        if (!rptTemplateService.updateById(update)) {
            return Result.badRequest("模板不存在或已被删除！");
        }
        return Result.success(rptTemplate.getStatus() == 1 ? "启用成功!" : "停用成功!");
    }

    /**
     * 通过id删除（逻辑删除）
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        // 模板逻辑删除但保留其数据集：已建报表的 configJson 是按该模板复制的，
        // 查看页仍按 report.template_id 取数据集，删掉会让已发布报表取不到数
        rptTemplateService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 通过ids批量删除
     */
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        // 同单条删除：保留数据集，避免影响已建报表取数
        rptTemplateService.removeBatchByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.success("批量删除成功!");
    }

    /**
     * 复制模板（含设计配置）
     */
    @PostMapping("/copy")
    public Result<String> copy(@RequestBody RptTemplate rptTemplate, HttpServletRequest request) {
        if (StringUtil.isEmpty(rptTemplate.getId())) {
            return Result.badRequest("缺少源模板ID！");
        }
        if (StringUtil.isEmpty(rptTemplate.getTemplateCode()) || StringUtil.isEmpty(rptTemplate.getTemplateName())) {
            return Result.badRequest("新模板编码/名称不能为空！");
        }
        if (rptTemplateService.ifExistsCode(rptTemplate)) {
            return Result.badRequest("新模板编码已存在！");
        }
        RptTemplate source = rptTemplateService.getById(rptTemplate.getId());
        if (source == null) {
            return Result.badRequest("源模板不存在或已被删除！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        RptTemplate copy = new RptTemplate();
        copy.setTemplateCode(rptTemplate.getTemplateCode());
        copy.setTemplateName(rptTemplate.getTemplateName());
        copy.setTemplateType(source.getTemplateType());
        copy.setConfigJson(source.getConfigJson());
        copy.setStatus(source.getStatus());
        copy.setRemark(source.getRemark());
        copy.setCreateBy(userName);
        copy.setCreateTime(LocalDateTime.now());
        rptTemplateService.save(copy);

        // 数据集归属模板：一并复制，副本才可直接使用（绑定关系在 configJson 中按 datasetCode 引用）
        QueryWrapper<RptDataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", source.getId());
        for (RptDataset dataset : rptDatasetService.list(queryWrapper)) {
            RptDataset copyDataset = new RptDataset();
            copyDataset.setTemplateId(copy.getId());
            copyDataset.setDsId(dataset.getDsId());
            copyDataset.setDatasetCode(dataset.getDatasetCode());
            copyDataset.setDatasetName(dataset.getDatasetName());
            copyDataset.setSqlText(dataset.getSqlText());
            copyDataset.setParamsJson(dataset.getParamsJson());
            copyDataset.setCreateBy(userName);
            copyDataset.setCreateTime(LocalDateTime.now());
            rptDatasetService.save(copyDataset);
        }
        return Result.success("复制成功!");
    }

    /**
     * 设计器加载：模板基本信息 + 配置 JSON（snapshot + params + bindings）
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> config(@RequestParam(name = "id") String id) {
        RptTemplate template = rptTemplateService.getById(id);
        if (template == null) {
            return Result.badRequest("模板不存在或已被删除！");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("template", template);
        return Result.success(data);
    }

    /**
     * 设计器保存：仅更新 configJson（版式 + 参数 + 字段绑定）
     */
    @PostMapping("/saveConfig")
    public Result<String> saveConfig(@RequestBody RptTemplate rptTemplate) {
        if (StringUtil.isEmpty(rptTemplate.getId())) {
            return Result.badRequest("缺少模板ID！");
        }
        if (StringUtil.isEmpty(rptTemplate.getConfigJson())) {
            return Result.badRequest("模板配置不能为空！");
        }
        RptTemplate update = new RptTemplate();
        update.setId(rptTemplate.getId());
        update.setConfigJson(rptTemplate.getConfigJson());
        if (!rptTemplateService.updateById(update)) {
            return Result.badRequest("模板不存在或已被删除！");
        }
        return Result.success("保存成功!");
    }

    private String validateBase(RptTemplate rptTemplate) {
        if (StringUtil.isEmpty(rptTemplate.getTemplateCode())) {
            return "模板编码不能为空！";
        }
        if (StringUtil.isEmpty(rptTemplate.getTemplateName())) {
            return "模板名称不能为空！";
        }
        if (rptTemplate.getTemplateCode().length() > 64) {
            return "模板编码长度不能超过64！";
        }
        return null;
    }
}
