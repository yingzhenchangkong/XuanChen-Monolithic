package com.xuanchen.visualization.visscreen.controller;

import com.xuanchen.visualization.visscreen.entity.VisDataset;
import com.xuanchen.visualization.visscreen.service.IVisDatasetService;
import com.xuanchen.visualization.visscreen.service.IVisScreenService;
import com.xuanchen.visualization.util.ScreenDataExecutor;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.entity.DatasourceOption;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->大屏数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequestMapping("/visualization/dataset")
public class VisDatasetController {
    @Autowired
    private IVisDatasetService visDatasetService;
    @Autowired
    private IVisScreenService visScreenService;
    @Autowired
    private ScreenDataExecutor screenDataExecutor;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private DatasourceProvider datasourceProvider;

    /**
     * 按大屏ID查询数据集
     */
    @GetMapping("/list")
    @RequiresRoles("admin")
    public Result<List<VisDataset>> list(@RequestParam(name = "screenId") String screenId) {
        return Result.success(visDatasetService.listByScreen(screenId));
    }

    /**
     * 外部数据源列表（兼容端点）：经 DatasourceProvider 读取，只返回 id/名称/类型/状态。
     * 前端已统一改用 /system/datasource/select，本端点保留供外部调用方兼容。
     */
    @GetMapping("/datasource/list")
    public Result<List<DatasourceOption>> datasourceList() {
        return Result.success(datasourceProvider.listOptions());
    }

    /**
     * 新增数据集
     */
    @PostMapping("/add")
    @RequiresRoles("admin")
    public Result<String> add(@RequestBody VisDataset visDataset, HttpServletRequest request) {
        String error = validateBase(visDataset);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (visDatasetService.ifExistsKey(visDataset)) {
            return Result.badRequest("数据集编码在该大屏内已存在！");
        }
        visDataset.setCreateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        visDataset.setCreateTime(LocalDateTime.now());
        visDatasetService.save(visDataset);
        return Result.success("添加成功!");
    }

    /**
     * 修改数据集
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> edit(@RequestBody VisDataset visDataset, HttpServletRequest request) {
        if (StringUtil.isEmpty(visDataset.getId())) {
            return Result.badRequest("缺少数据集ID！");
        }
        String error = validateBase(visDataset);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (visDatasetService.ifExistsKey(visDataset)) {
            return Result.badRequest("数据集编码在该大屏内已存在！");
        }
        VisDataset update = new VisDataset();
        update.setId(visDataset.getId());
        update.setDatasetKey(visDataset.getDatasetKey());
        update.setDatasetName(visDataset.getDatasetName());
        update.setSqlText(visDataset.getSqlText());
        update.setDsId(StringUtil.isEmpty(visDataset.getDsId()) ? null : visDataset.getDsId());
        update.setRemark(visDataset.getRemark());
        update.setUpdateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        update.setUpdateTime(LocalDateTime.now());
        if (!visDatasetService.updateById(update)) {
            return Result.badRequest("数据集不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 删除数据集
     */
    @DeleteMapping("/delete")
    @RequiresRoles("admin")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        visDatasetService.removeById(id);
        return Result.success("删除成功!");
    }

    /**
     * 设计器试执行：传 sql/dsId（未保存）或 datasetId（已保存）
     */
    @PostMapping("/preview")
    @RequiresRoles("admin")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, Object> params) {
        Object sqlParam = params == null ? null : params.get("sql");
        Object idParam = params == null ? null : params.get("datasetId");
        Object dsIdParam = params == null ? null : params.get("dsId");
        String sql = sqlParam == null ? null : String.valueOf(sqlParam);
        String dsId = dsIdParam == null || String.valueOf(dsIdParam).isBlank() ? null : String.valueOf(dsIdParam);
        if (StringUtil.isEmpty(sql) && idParam != null) {
            VisDataset dataset = visDatasetService.getById(String.valueOf(idParam));
            if (dataset == null) {
                return Result.badRequest("数据集不存在或已被删除！");
            }
            sql = dataset.getSqlText();
            if (StringUtil.isEmpty(dsId)) dsId = StringUtil.isEmpty(dataset.getDsId()) ? null : dataset.getDsId();
        }
        try {
            ScreenDataExecutor.ScreenData result;
            if (StringUtil.isEmpty(dsId)) {
                result = screenDataExecutor.execute(sql, null);
            } else {
                result = screenDataExecutor.executeExternal(dsId, sql, null);
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

    private String validateBase(VisDataset visDataset) {
        if (StringUtil.isEmpty(visDataset.getScreenId())) {
            return "缺少所属大屏ID！";
        }
        if (visScreenService.getById(visDataset.getScreenId()) == null) {
            return "所属大屏不存在或已被删除！";
        }
        if (StringUtil.isEmpty(visDataset.getDatasetKey())) {
            return "数据集编码不能为空！";
        }
        if (StringUtil.isEmpty(visDataset.getDatasetName())) {
            return "数据集名称不能为空！";
        }
        if (StringUtil.isEmpty(visDataset.getSqlText())) {
            return "查询 SQL 不能为空！";
        }
        if (visDataset.getDatasetKey().length() > 64) {
            return "数据集编码长度不能超过64！";
        }
        return null;
    }
}
