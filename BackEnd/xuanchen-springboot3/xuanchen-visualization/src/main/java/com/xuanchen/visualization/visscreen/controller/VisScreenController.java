package com.xuanchen.visualization.visscreen.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.visualization.visscreen.entity.VisDataset;
import com.xuanchen.visualization.visscreen.entity.VisScreen;
import com.xuanchen.visualization.visscreen.service.IVisDatasetService;
import com.xuanchen.visualization.visscreen.service.IVisScreenService;
import com.xuanchen.visualization.util.ScreenDataExecutor;
import com.xuanchen.common.entity.Result;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->数据大屏
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequestMapping("/visualization")
public class VisScreenController {
    @Autowired
    private IVisScreenService visScreenService;
    @Autowired
    private IVisDatasetService visDatasetService;
    @Autowired
    private ScreenDataExecutor screenDataExecutor;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页列表（管理/画廊共用，列表不返回 configJson 大字段）
     */
    @GetMapping("/list")
    @RequiresRoles("admin")
    public Result<IPage<VisScreen>> list(VisScreen visScreen,
                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<VisScreen> page = new Page<>(pageNo, pageSize);
        return Result.success(visScreenService.listScreen(page, visScreen));
    }

    /**
     * 新增大屏（基本信息，设计配置在设计器中保存）
     */
    @PostMapping("/add")
    @RequiresRoles("admin")
    public Result<String> add(@RequestBody VisScreen visScreen, HttpServletRequest request) {
        String error = validateBase(visScreen);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (visScreenService.ifExistsCode(visScreen)) {
            return Result.badRequest("大屏编码已存在！");
        }
        visScreen.setStatus(0);
        visScreen.setCreateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        visScreen.setCreateTime(LocalDateTime.now());
        visScreenService.save(visScreen);
        return Result.success("添加成功!");
    }

    /**
     * 修改大屏（基本信息；画布/组件配置走 saveConfig）
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> edit(@RequestBody VisScreen visScreen, HttpServletRequest request) {
        if (StringUtil.isEmpty(visScreen.getId())) {
            return Result.badRequest("缺少大屏ID！");
        }
        String error = validateBase(visScreen);
        if (error != null) {
            return Result.badRequest(error);
        }
        if (visScreenService.ifExistsCode(visScreen)) {
            return Result.badRequest("大屏编码已存在！");
        }
        VisScreen update = new VisScreen();
        update.setId(visScreen.getId());
        update.setScreenCode(visScreen.getScreenCode());
        update.setScreenName(visScreen.getScreenName());
        update.setThumbnail(visScreen.getThumbnail());
        update.setRemark(visScreen.getRemark());
        update.setUpdateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        update.setUpdateTime(LocalDateTime.now());
        if (!visScreenService.updateById(update)) {
            return Result.badRequest("大屏不存在或已被删除！");
        }
        return Result.success("修改成功!");
    }

    /**
     * 通过id删除（逻辑删除大屏与其数据集）
     */
    @DeleteMapping("/delete")
    @RequiresRoles("admin")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        visScreenService.removeById(id);
        QueryWrapper<VisDataset> dataQuery = new QueryWrapper<>();
        dataQuery.eq("screen_id", id);
        visDatasetService.remove(dataQuery);
        return Result.success("删除成功!");
    }

    /**
     * 通过ids批量删除
     */
    @DeleteMapping("/deleteBatch")
    @RequiresRoles("admin")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        visScreenService.removeBatchByIds(idList);
        QueryWrapper<VisDataset> dataQuery = new QueryWrapper<>();
        dataQuery.in("screen_id", idList);
        visDatasetService.remove(dataQuery);
        return Result.success("批量删除成功!");
    }

    /**
     * 复制大屏（配置 JSON + 数据集）
     */
    @PostMapping("/copy")
    @RequiresRoles("admin")
    public Result<String> copy(@RequestBody VisScreen visScreen, HttpServletRequest request) {
        if (StringUtil.isEmpty(visScreen.getId())) {
            return Result.badRequest("缺少源大屏ID！");
        }
        if (StringUtil.isEmpty(visScreen.getScreenCode()) || StringUtil.isEmpty(visScreen.getScreenName())) {
            return Result.badRequest("新大屏编码/名称不能为空！");
        }
        if (visScreenService.ifExistsCode(visScreen)) {
            return Result.badRequest("新大屏编码已存在！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        visScreenService.copyScreen(visScreen.getId(), visScreen.getScreenCode(), visScreen.getScreenName(), userName);
        return Result.success("复制成功!");
    }

    /**
     * 发布大屏（草稿/停用 -> 已发布）
     */
    @PostMapping("/publish")
    @RequiresRoles("admin")
    public Result<String> publish(@RequestBody VisScreen visScreen) {
        if (StringUtil.isEmpty(visScreen.getId())) {
            return Result.badRequest("缺少大屏ID！");
        }
        VisScreen db = visScreenService.getById(visScreen.getId());
        if (db == null) {
            return Result.badRequest("大屏不存在或已被删除！");
        }
        if (StringUtil.isEmpty(db.getConfigJson())) {
            return Result.badRequest("大屏尚未设计（无画布配置），不能发布！");
        }
        VisScreen update = new VisScreen();
        update.setId(visScreen.getId());
        update.setStatus(1);
        update.setPublishTime(LocalDateTime.now());
        visScreenService.updateById(update);
        return Result.success("发布成功!");
    }

    /**
     * 停用大屏
     */
    @PostMapping("/unpublish")
    @RequiresRoles("admin")
    public Result<String> unpublish(@RequestBody VisScreen visScreen) {
        if (StringUtil.isEmpty(visScreen.getId())) {
            return Result.badRequest("缺少大屏ID！");
        }
        VisScreen update = new VisScreen();
        update.setId(visScreen.getId());
        update.setStatus(2);
        visScreenService.updateById(update);
        return Result.success("停用成功!");
    }

    /**
     * 设计器加载：大屏基本信息 + 全部配置 JSON + 数据集定义
     */
    @GetMapping("/config")
    @RequiresRoles("admin")
    public Result<Map<String, Object>> config(@RequestParam(name = "id") String id) {
        VisScreen screen = visScreenService.getById(id);
        if (screen == null) {
            return Result.badRequest("大屏不存在或已被删除！");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("screen", screen);
        data.put("datasets", visDatasetService.listByScreen(id));
        return Result.success(data);
    }

    /**
     * 保存设计配置（只更新 configJson）
     */
    @PostMapping("/saveConfig")
    @RequiresRoles("admin")
    public Result<String> saveConfig(@RequestBody VisScreen visScreen, HttpServletRequest request) {
        if (StringUtil.isEmpty(visScreen.getId())) {
            return Result.badRequest("缺少大屏ID！");
        }
        if (StringUtil.isEmpty(visScreen.getConfigJson())) {
            return Result.badRequest("大屏配置不能为空！");
        }
        VisScreen update = new VisScreen();
        update.setId(visScreen.getId());
        update.setConfigJson(visScreen.getConfigJson());
        update.setUpdateBy(authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN")));
        update.setUpdateTime(LocalDateTime.now());
        if (!visScreenService.updateById(update)) {
            return Result.badRequest("大屏不存在或已被删除！");
        }
        return Result.success("保存成功!");
    }

    /**
     * 大屏展示页加载：按编码取已发布大屏的配置，并在服务端执行其数据集后一并返回。
     * 登录即可访问（不要求 admin），SQL 不下发到前端。
     */
    @GetMapping("/viewConfig")
    public Result<Map<String, Object>> viewConfig(@RequestParam(name = "code") String code) {
        QueryWrapper<VisScreen> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("screen_code", code).eq("status", 1);
        VisScreen screen = visScreenService.getOne(queryWrapper, false);
        if (screen == null) {
            return Result.badRequest("大屏不存在、未发布或已停用！");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", screen.getId());
        data.put("screenCode", screen.getScreenCode());
        data.put("screenName", screen.getScreenName());
        data.put("configJson", screen.getConfigJson());

        // 数据集逐个执行：单个数据集失败只标记该数据集，不阻塞整屏渲染
        List<Map<String, Object>> datasets = new ArrayList<>();
        for (VisDataset ds : visDatasetService.listByScreen(screen.getId())) {
            Map<String, Object> item = new HashMap<>();
            item.put("datasetKey", ds.getDatasetKey());
            item.put("datasetName", ds.getDatasetName());
            try {
                ScreenDataExecutor.ScreenData result = screenDataExecutor.execute(ds.getSqlText(), null);
                item.put("columns", result.columns());
                item.put("rows", result.rows());
            } catch (Exception e) {
                item.put("columns", new ArrayList<>());
                item.put("rows", new ArrayList<>());
                item.put("error", e.getMessage());
            }
            datasets.add(item);
        }
        data.put("datasets", datasets);
        return Result.success(data);
    }

    private String validateBase(VisScreen visScreen) {
        if (StringUtil.isEmpty(visScreen.getScreenCode())) {
            return "大屏编码不能为空！";
        }
        if (StringUtil.isEmpty(visScreen.getScreenName())) {
            return "大屏名称不能为空！";
        }
        if (visScreen.getScreenCode().length() > 64) {
            return "大屏编码长度不能超过64！";
        }
        if (visScreen.getScreenName().length() > 100) {
            return "大屏名称长度不能超过100！";
        }
        return null;
    }
}
