package com.xuanchen.report.rptdataset.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rptdataset.entity.RptDataset;
import com.xuanchen.report.rptdataset.service.IRptDatasetService;
import com.xuanchen.report.rptreport.entity.RptReport;
import com.xuanchen.report.rptreport.service.IRptReportService;
import com.xuanchen.report.util.DatasetExecutor;
import com.xuanchen.common.utils.SqlGuard;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->报表数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequestMapping("/report/dataset")
public class RptDatasetController {

    /** 取数请求体：datasetId/designer 用，datasetCode+reportCode/viewer 用 */
    public record ExecuteRequest(String reportCode, String datasetId, String datasetCode,
                                 Map<String, Object> params) {
    }

    @Autowired
    private IRptDatasetService rptDatasetService;
    @Autowired
    private IRptReportService rptReportService;
    @Autowired
    private DatasetExecutor datasetExecutor;
    @Autowired
    private DatasourceProvider datasourceProvider;

    /**
     * 模板数据集列表（设计器/管理端，含 SQL 文本）。
     * 数据源名称按当页 dsId 批量回填（一次调用，不逐行查），不再依赖 SQL JOIN sys_datasource。
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<List<RptDataset>> list(@RequestParam(name = "templateId") String templateId) {
        List<RptDataset> list = rptDatasetService.listByTemplate(templateId);
        List<String> dsIds = list.stream()
                .map(RptDataset::getDsId)
                .filter(StringUtil::isNotEmpty)
                .distinct()
                .toList();
        if (!dsIds.isEmpty()) {
            Map<String, DatasourceInfo> dsMap = datasourceProvider.getBatch(dsIds);
            list.forEach(item -> {
                DatasourceInfo ds = dsMap.get(item.getDsId());
                if (ds != null) {
                    item.setDsName(ds.getDsName());
                }
            });
        }
        return Result.success(list);
    }

    /**
     * 新增数据集
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('admin')")
    public Result<String> add(@RequestBody RptDataset dataset) {
        String error = validate(dataset);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (rptDatasetService.ifExistsCode(dataset)) {
            return Result.badRequest("数据集编码在当前模板内已存在！");
        }
        SqlGuard.validateSelectSyntax(dataset.getSqlText());
        dataset.setCreateBy(null);
        dataset.setCreateTime(LocalDateTime.now());
        rptDatasetService.save(dataset);
        return Result.success("添加成功!");
    }

    /**
     * 修改数据集
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('admin')")
    public Result<String> edit(@RequestBody RptDataset dataset) {
        if (StringUtil.isEmpty(dataset.getId())) {
            return Result.badRequest("缺少数据集ID！");
        }
        String error = validate(dataset);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (rptDatasetService.ifExistsCode(dataset)) {
            return Result.badRequest("数据集编码在当前模板内已存在！");
        }
        SqlGuard.validateSelectSyntax(dataset.getSqlText());
        RptDataset update = new RptDataset();
        update.setId(dataset.getId());
        update.setDsId(dataset.getDsId());
        update.setDatasetCode(dataset.getDatasetCode());
        update.setDatasetName(dataset.getDatasetName());
        update.setSqlText(dataset.getSqlText());
        update.setParamsJson(dataset.getParamsJson());
        if (!rptDatasetService.updateById(update)) {
            return Result.badRequest("数据集不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 通过id删除
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('admin')")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        rptDatasetService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 执行数据集（设计器试执行：datasetId 直接定位）
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> execute(@RequestBody ExecuteRequest req) {
        if (req == null || StringUtil.isEmpty(req.datasetId())) {
            return Result.badRequest("缺少数据集ID！");
        }
        RptDataset dataset = rptDatasetService.getById(req.datasetId());
        if (dataset == null) {
            return Result.badRequest("数据集不存在或已被删除！");
        }
        return doExecute(dataset, req.params());
    }

    /**
     * 查看页取数（登录即可）：按报表编码 + 数据集编码定位，
     * 报表不再走发布流程（状态流转只在模板上做），SQL 不出服务端。
     */
    @PostMapping("/data")
    public Result<Map<String, Object>> data(@RequestBody ExecuteRequest req) {
        if (req == null || StringUtil.isEmpty(req.reportCode()) || StringUtil.isEmpty(req.datasetCode())) {
            return Result.badRequest("缺少报表编码或数据集编码！");
        }
        QueryWrapper<RptReport> reportWrapper = new QueryWrapper<>();
        reportWrapper.eq("report_code", req.reportCode());
        RptReport report = rptReportService.getOne(reportWrapper, false);
        if (report == null) {
            return Result.badRequest("报表不存在或已被删除！");
        }
        if (StringUtil.isEmpty(report.getTemplateId())) {
            return Result.badRequest("该报表未关联模板，无法取数！");
        }
        // 数据集挂在模板上（设计整体在「模板设计」），报表按所选模板取数据集
        QueryWrapper<RptDataset> datasetWrapper = new QueryWrapper<>();
        datasetWrapper.eq("template_id", report.getTemplateId()).eq("dataset_code", req.datasetCode());
        RptDataset dataset = rptDatasetService.getOne(datasetWrapper, false);
        if (dataset == null) {
            return Result.badRequest("数据集不存在！");
        }
        return doExecute(dataset, req.params());
    }

    private Result<Map<String, Object>> doExecute(RptDataset dataset, Map<String, Object> params) {
        try {
            // 参数按 SQL 占位符出现顺序绑定，未提供的一律 null（防注入：不拼接值）
            Object[] extracted = SqlGuard.extractParams(dataset.getSqlText());
            String[] paramNames = (String[]) extracted[1];
            List<Object> bindParams = new ArrayList<>();
            for (String name : paramNames) {
                bindParams.add(params == null ? null : params.get(name));
            }

            DatasetExecutor.DatasetResult result;
            if (StringUtil.isEmpty(dataset.getDsId())) {
                result = datasetExecutor.executeMain(dataset.getSqlText(), bindParams);
            } else {
                DatasourceInfo ds = datasourceProvider.get(dataset.getDsId());
                result = datasetExecutor.executeExternal(ds, dataset.getSqlText(), bindParams);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("columns", result.columns());
            data.put("rows", result.rows());
            data.put("total", result.rows().size());
            return Result.success(data);
        } catch (BusinessException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    private String validate(RptDataset dataset) {
        if (StringUtil.isEmpty(dataset.getTemplateId())) {
            return "缺少所属模板ID！";
        }
        if (StringUtil.isEmpty(dataset.getDatasetCode())) {
            return "数据集编码不能为空！";
        }
        if (StringUtil.isEmpty(dataset.getDatasetName())) {
            return "数据集名称不能为空！";
        }
        if (StringUtil.isEmpty(dataset.getSqlText())) {
            return "查询 SQL 不能为空！";
        }
        if (StringUtil.isNotEmpty(dataset.getParamsJson()) && !isValidJsonArray(dataset.getParamsJson())) {
            return "参数定义必须是 JSON 数组！";
        }
        return null;
    }

    private boolean isValidJsonArray(String text) {
        try {
            JsonUtils.fromJson(text, List.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
