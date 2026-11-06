package com.xuanchen.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.datasource.DatasourceProvider;
import com.xuanchen.common.entity.DatasourceOption;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.ApiData;
import com.xuanchen.api.entity.ApiSchedule;
import com.xuanchen.api.service.IApiInvokeService;
import com.xuanchen.api.service.IApiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->数据接口服务-接口管理
 * 查询/测试：登录用户；写操作（保存/删除/发布/停用/定时配置）：仅 admin。
 * POST 一律 FormData（与后端 @RequestParam 绑定约定一致）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private IApiService apiService;
    @Autowired
    private IApiInvokeService invokeService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private DatasourceProvider datasourceProvider;

    // ---------------- 外部数据源（sql 步骤可选用） ----------------

    /**
     * 外部数据源列表（供前端 sql 步骤表单下拉）。
     * 只返回 id/名称/类型/状态，连接串与口令不出服务端。
     */
    @GetMapping("/datasource/list")
    public Result<List<DatasourceOption>> datasourceList() {
        return Result.success(datasourceProvider.listOptions());
    }

    // ---------------- 接口定义 ----------------

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<Page<Api>> list(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                      @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                      @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(apiService.pageList(pageNo, pageSize, keyword));
    }

    /**
     * 详情
     */
    @GetMapping("/detail")
    public Result<Api> detail(@RequestParam(name = "id") String id) {
        try {
            return Result.success(apiService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 保存（FormData：id 可空、apiCode、apiName、apiKey 可空、steps JSON、remark 可空）
     */
    @PostMapping("/save")
    public Result<Api> save(HttpServletRequest request) {
        String username = currentUsername(request);
        if (!isAdmin()) {
            return Result.error("仅管理员可维护接口配置！");
        }
        try {
            Api api = apiService.saveDoc(request.getParameter("id"), request.getParameter("apiCode"),
                    request.getParameter("apiName"), request.getParameter("apiKey"),
                    request.getParameter("steps"), request.getParameter("remark"), username);
            return Result.success("保存成功!", api);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("接口配置保存失败", e);
            return Result.error("保存失败，请重试！");
        }
    }

    /**
     * 删除（逻辑删，同时清理定时配置与路由缓存）
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = currentUsername(request);
        if (!isAdmin()) {
            return Result.error("仅管理员可维护接口配置！");
        }
        try {
            apiService.deleteDoc(id, username);
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("接口删除失败 id={}", id, e);
            return Result.error("删除失败，请重试！");
        }
    }

    /**
     * 发布（草稿/停用 → 已发布）
     */
    @PostMapping("/publish")
    public Result<Void> publish(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = currentUsername(request);
        if (!isAdmin()) {
            return Result.error("仅管理员可发布接口！");
        }
        try {
            apiService.publish(id, username);
            return Result.success("发布成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("接口发布失败 id={}", id, e);
            return Result.error("发布失败，请重试！");
        }
    }

    /**
     * 停用（已发布 → 已停用）
     */
    @PostMapping("/stop")
    public Result<Void> stop(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = currentUsername(request);
        if (!isAdmin()) {
            return Result.error("仅管理员可停用接口！");
        }
        try {
            apiService.stop(id, username);
            return Result.success("停用成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("接口停用失败 id={}", id, e);
            return Result.error("停用失败，请重试！");
        }
    }

    /**
     * 测试运行（登录用户；触发方式=测试，不落采集数据）
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(HttpServletRequest request) {
        String username = currentUsername(request);
        try {
            Api api = apiService.getDetail(request.getParameter("id"));
            Map<String, Object> params = new HashMap<>();
            String paramsJson = request.getParameter("params");
            if (paramsJson != null && !paramsJson.isBlank()) {
                params = com.xuanchen.common.utils.JsonUtils.parseToMap(paramsJson);
            }
            return Result.success(invokeService.execute(api, params, IApiInvokeService.TRIGGER_TEST, false));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error("测试运行失败：" + e.getMessage());
        }
    }

    // ---------------- 定时采集配置 ----------------

    /**
     * 定时配置分页（可按 apiCode 筛选）
     */
    @GetMapping("/schedule/list")
    public Result<Page<ApiSchedule>> scheduleList(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                                      @RequestParam(name = "apiCode", required = false) String apiCode) {
        return Result.success(apiService.schedulePage(pageNo, pageSize, apiCode));
    }

    /**
     * 保存定时配置（FormData：id 可空、apiCode、cron、enabled、remark 可空）
     */
    @PostMapping("/schedule/save")
    public Result<ApiSchedule> scheduleSave(HttpServletRequest request) {
        String username = currentUsername(request);
        if (!isAdmin()) {
            return Result.error("仅管理员可维护定时采集配置！");
        }
        try {
            String enabledStr = request.getParameter("enabled");
            ApiSchedule row = apiService.scheduleSave(request.getParameter("id"), request.getParameter("apiCode"),
                    request.getParameter("cron"), enabledStr == null ? null : Integer.valueOf(enabledStr),
                    request.getParameter("remark"), username);
            return Result.success("保存成功!", row);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("定时配置保存失败", e);
            return Result.error("保存失败，请重试！");
        }
    }

    /**
     * 删除定时配置
     */
    @DeleteMapping("/schedule/delete")
    public Result<Void> scheduleDelete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        if (!isAdmin()) {
            return Result.error("仅管理员可维护定时采集配置！");
        }
        try {
            apiService.scheduleDelete(id, currentUsername(request));
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("定时配置删除失败 id={}", id, e);
            return Result.error("删除失败，请重试！");
        }
    }

    // ---------------- 采集数据 / 调用日志 ----------------

    /**
     * 采集数据分页（可按 apiCode 筛选）
     */
    @GetMapping("/data/list")
    public Result<Page<ApiData>> dataList(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                              @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                              @RequestParam(name = "apiCode", required = false) String apiCode) {
        return Result.success(apiService.dataPage(pageNo, pageSize, apiCode));
    }

    /**
     * 删除单条采集数据
     */
    @DeleteMapping("/data/delete")
    public Result<Void> dataDelete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        if (!isAdmin()) {
            return Result.error("仅管理员可删除采集数据！");
        }
        try {
            apiService.dataDelete(id, currentUsername(request));
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("采集数据删除失败 id={}", id, e);
            return Result.error("删除失败，请重试！");
        }
    }

    private String currentUsername(HttpServletRequest request) {
        return authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
    }

    /**
     * sb3（Shiro）判断当前用户是否 admin
     */
    private static boolean isAdmin() {
        return SecurityUtils.getSubject().hasRole("admin");
    }
}
