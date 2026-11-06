package com.xuanchen.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xuanchen.api.engine.StepDef;
import com.xuanchen.api.entity.Api;
import com.xuanchen.common.utils.JsonUtils;

import java.util.List;
import java.util.Map;

/**
 * 服务-->数据接口服务-执行引擎（分发器）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IApiInvokeService {

    /** 触发方式：手动调用 */
    int TRIGGER_MANUAL = 1;
    /** 触发方式：定时采集 */
    int TRIGGER_SCHEDULE = 2;
    /** 触发方式：测试运行 */
    int TRIGGER_TEST = 3;

    /**
     * 执行接口步骤链
     *
     * @param api         接口定义（须已发布）
     * @param params      入参（来自 query/form）
     * @param triggerType 触发方式（1手动调用，2定时采集，3测试运行）
     * @param collect     是否将最终结果落采集数据表（定时采集为 true）
     * @return 执行结果：data=最终步骤输出，costMs=耗时毫秒
     */
    Map<String, Object> execute(Api api, Map<String, Object> params, int triggerType, boolean collect);

    /**
     * 按 api_code 取接口定义（优先 Redis 路由缓存，仅缓存已发布接口）
     *
     * @return null=不存在或未发布
     */
    Api getPublishedByCode(String apiCode);

    /**
     * 路由缓存失效（配置变更/停用/删除时调用）
     */
    void evictRouteCache(String apiCode);

    /**
     * 解析并校验步骤链 JSON（供保存接口时做前置校验，也供执行引擎复用）。
     */
    static List<StepDef> parseSteps(String stepsJson) {
        if (stepsJson == null || stepsJson.isBlank()) {
            throw new IllegalArgumentException("接口未配置任何步骤！");
        }
        List<StepDef> steps = JsonUtils.fromJson(stepsJson, new TypeReference<List<StepDef>>() {});
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("接口未配置任何步骤！");
        }
        steps.forEach(StepDef::validate);
        long distinct = steps.stream().map(StepDef::getName).distinct().count();
        if (distinct != steps.size()) {
            throw new IllegalArgumentException("步骤名重复！");
        }
        return steps;
    }

    /**
     * 步骤链中是否存在指向外部数据源（sys_datasource）的 sql 步骤。
     * 用于发布时强制要求 apiKey，避免匿名分发入口直连外部业务库。
     */
    static boolean usesExternalDatasource(String stepsJson) {
        for (StepDef step : parseSteps(stepsJson)) {
            if (!"sql".equals(step.getType()) || step.getConfig() == null) {
                continue;
            }
            Object ds = step.getConfig().get("datasource");
            if (ds != null && !String.valueOf(ds).isBlank()
                    && !"main".equalsIgnoreCase(String.valueOf(ds))) {
                return true;
            }
        }
        return false;
    }
}
