package com.xuanchen.report.rptreport.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rptdataset.entity.RptDataset;
import com.xuanchen.report.rptdataset.service.IRptDatasetService;
import com.xuanchen.report.rptreport.entity.RptReport;
import com.xuanchen.report.rptreport.service.IRptReportService;
import com.xuanchen.report.rpttemplate.entity.RptTemplate;
import com.xuanchen.report.rpttemplate.service.IRptTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->报表
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequestMapping("/report")
public class RptReportController {
    @Autowired
    private IRptReportService rptReportService;
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
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<RptReport>> list(RptReport rptReport,
                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<RptReport> page = new Page<>(pageNo, pageSize);
        return Result.success(rptReportService.listReport(page, rptReport));
    }

    /**
     * 新增报表（基本信息 + 复制所选模板的设计快照）
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('admin')")
    public Result<String> add(@RequestBody RptReport rptReport, HttpServletRequest request) {
        String error = validateBase(rptReport);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (StringUtil.isEmpty(rptReport.getTemplateId())) {
            return Result.badRequest("请选择报表模板！");
        }
        RptTemplate selected = rptTemplateService.getById(rptReport.getTemplateId());
        if (selected == null) {
            return Result.badRequest("所选模板不存在或已被删除！");
        }
        if (selected.getStatus() != null && selected.getStatus() == 0) {
            return Result.badRequest("所选模板已停用，请重新选择！");
        }
        // 模板快照在新增时复制进报表配置，之后报表与模板解耦（改模板不影响已建报表）
        rptReport.setConfigJson(selected.getConfigJson());
        if (rptReportService.ifExistsCode(rptReport)) {
            return Result.badRequest("报表编码已存在！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        rptReport.setCreateBy(userName);
        rptReport.setCreateTime(LocalDateTime.now());
        rptReportService.save(rptReport);
        return Result.success("添加成功!");
    }

    /**
     * 修改报表（基本信息）
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('admin')")
    public Result<String> edit(@RequestBody RptReport rptReport) {
        if (StringUtil.isEmpty(rptReport.getId())) {
            return Result.badRequest("缺少报表ID！");
        }
        String error = validateBase(rptReport);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (rptReportService.ifExistsCode(rptReport)) {
            return Result.badRequest("报表编码已存在！");
        }
        // 编码/类型修改不影响已发布的配置；状态只能通过发布/停用接口流转
        // 模板仅在新增时决定（快照已复制），编辑不换模板，避免「换了模板但版式没变」的误解
        RptReport update = new RptReport();
        update.setId(rptReport.getId());
        update.setReportCode(rptReport.getReportCode());
        update.setReportName(rptReport.getReportName());
        update.setReportType(rptReport.getReportType());
        update.setRemark(rptReport.getRemark());
        if (!rptReportService.updateById(update)) {
            return Result.badRequest("报表不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 通过id删除（逻辑删除，数据集一并逻辑删除由查询侧隔离展示）
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('admin')")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        rptReportService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 通过ids批量删除
     */
    @DeleteMapping("/deleteBatch")
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        rptReportService.removeBatchByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.success("批量删除成功!");
    }

    /**
     * 复制报表（含数据集与配置 JSON）
     */
    @PostMapping("/copy")
    @PreAuthorize("hasRole('admin')")
    public Result<String> copy(@RequestBody RptReport rptReport, HttpServletRequest request) {
        if (StringUtil.isEmpty(rptReport.getId())) {
            return Result.badRequest("缺少源报表ID！");
        }
        if (StringUtil.isEmpty(rptReport.getReportCode()) || StringUtil.isEmpty(rptReport.getReportName())) {
            return Result.badRequest("新报表编码/名称不能为空！");
        }
        if (rptReportService.ifExistsCode(rptReport)) {
            return Result.badRequest("新报表编码已存在！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        rptReportService.copyReport(rptReport.getId(), rptReport.getReportCode(), rptReport.getReportName(), userName);
        return Result.success("复制成功!");
    }

    /**
     * 查看页加载：按编码取报表配置（登录即可，不要求 admin）。
     * 只返回渲染必需内容：snapshot + 参数定义 + 绑定关系 + 数据集元信息（不返回 SQL 文本）。
     */
    @GetMapping("/viewConfig")
    public Result<Map<String, Object>> viewConfig(@RequestParam(name = "code") String code) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RptReport> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("report_code", code);
        RptReport report = rptReportService.getOne(queryWrapper, false);
        if (report == null) {
            return Result.badRequest("报表不存在或已被删除！");
        }
        // 数据集挂在模板上（设计整体在「模板设计」）：按报表所选模板返回，未关联模板时为空
        RptTemplate template = StringUtil.isEmpty(report.getTemplateId())
                ? null : rptTemplateService.getById(report.getTemplateId());
        // 报表快照为空（建报表时模板尚未设计）时回退到模板当前设计，保证查看页仍可渲染
        String configJson = StringUtil.isEmpty(report.getConfigJson())
                ? (template == null ? null : template.getConfigJson())
                : report.getConfigJson();
        Map<String, Object> data = new HashMap<>();
        data.put("id", report.getId());
        data.put("reportCode", report.getReportCode());
        data.put("reportName", report.getReportName());
        data.put("configJson", configJson);
        List<RptDataset> templateDatasets = template == null
                ? java.util.Collections.emptyList()
                : rptDatasetService.listByTemplate(report.getTemplateId());
        data.put("datasets", templateDatasets.stream()
                .map(ds -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("datasetCode", ds.getDatasetCode());
                    item.put("datasetName", ds.getDatasetName());
                    item.put("paramsJson", ds.getParamsJson());
                    return item;
                })
                .toList());
        return Result.success(data);
    }

    private String validateBase(RptReport rptReport) {
        if (StringUtil.isEmpty(rptReport.getReportCode())) {
            return "报表编码不能为空！";
        }
        if (StringUtil.isEmpty(rptReport.getReportName())) {
            return "报表名称不能为空！";
        }
        if (rptReport.getReportCode().length() > 64) {
            return "报表编码长度不能超过64！";
        }
        return null;
    }
}
