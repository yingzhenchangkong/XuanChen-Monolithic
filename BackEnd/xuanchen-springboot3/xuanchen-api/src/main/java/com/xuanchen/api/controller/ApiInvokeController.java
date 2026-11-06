package com.xuanchen.api.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.RedisUtil;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.service.IApiInvokeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 控制器-->数据接口服务-公开分发器
 * 统一入口 /open/api/{apiCode}（GET/POST），匿名放行（安全配置白名单）；
 * 接口级鉴权：api_key 非空时须带 X-Api-Key 头或 apiKey 参数。
 * 手动调用不落采集数据（采集由 save 步骤或定时任务负责）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
public class ApiInvokeController {

    @Autowired
    private IApiInvokeService invokeService;
    @Autowired
    private RedisUtil redisUtil;

    /** 单个接口每分钟最大调用次数，0=不限（xuanchen.api.invoke-rate-limit） */
    @Value("${xuanchen.api.invoke-rate-limit:120}")
    private long rateLimit;

    @GetMapping("/open/api/{apiCode}")
    public Result<Object> invokeGet(@PathVariable("apiCode") String apiCode, HttpServletRequest request) {
        return doInvoke(apiCode, request);
    }

    @PostMapping("/open/api/{apiCode}")
    public Result<Object> invokePost(@PathVariable("apiCode") String apiCode, HttpServletRequest request) {
        return doInvoke(apiCode, request);
    }

    private Result<Object> doInvoke(String apiCode, HttpServletRequest request) {
        // 匿名入口的兜底限流：按接口编码 + 分钟固定窗口计数，防止被刷导致外部数据源被打爆
        if (rateLimit > 0) {
            long window = System.currentTimeMillis() / 60000L;
            long count = redisUtil.incrWithExpire("api:invoke:" + apiCode + ":" + window, 60);
            if (count > rateLimit) {
                return Result.error("接口调用过于频繁，请稍后再试！");
            }
        }
        Api api = invokeService.getPublishedByCode(apiCode);
        if (api == null) {
            return Result.badRequest("接口不存在或未发布：" + apiCode);
        }
        //接口级密钥校验
        if (api.getApiKey() != null && !api.getApiKey().isBlank()) {
            String provided = request.getHeader("X-Api-Key");
            if (provided == null || provided.isBlank()) {
                provided = request.getParameter("apiKey");
            }
            if (!api.getApiKey().equals(provided)) {
                return Result.error("接口密钥校验失败！");
            }
        }
        //入参：query/form 全量进上下文 params
        Map<String, Object> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0) {
                params.put(k, v[0]);
            }
        });
        try {
            Map<String, Object> out = invokeService.execute(api, params, IApiInvokeService.TRIGGER_MANUAL, false);
            return Result.success(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("接口调用失败 code={}", apiCode, e);
            return Result.error("接口调用失败，请稍后重试！");
        }
    }
}
