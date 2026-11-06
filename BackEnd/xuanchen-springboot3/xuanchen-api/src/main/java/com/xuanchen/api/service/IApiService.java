package com.xuanchen.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.ApiData;
import com.xuanchen.api.entity.ApiSchedule;

/**
 * 服务-->数据接口服务-接口管理
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IApiService extends IService<Api> {

    /**
     * 分页查询（接口定义，按更新时间倒序）
     */
    Page<Api> pageList(long pageNo, long pageSize, String keyword);

    /**
     * 详情
     */
    Api getDetail(String id);

    /**
     * 保存（新建/更新；api_code 唯一、steps JSON 合法性校验）
     */
    Api saveDoc(String id, String apiCode, String apiName, String apiKey, String steps, String remark, String username);

    /**
     * 删除（逻辑删，同时清理定时配置与路由缓存）
     */
    void deleteDoc(String id, String username);

    /**
     * 发布（草稿/停用 → 已发布；发布前校验步骤链）
     */
    void publish(String id, String username);

    /**
     * 停用（已发布 → 已停用）
     */
    void stop(String id, String username);

    // ---------------- 定时采集配置 ----------------

    /**
     * 定时配置分页（可按 apiCode 筛选）
     */
    Page<ApiSchedule> schedulePage(long pageNo, long pageSize, String apiCode);

    /**
     * 保存定时配置（cron 校验 + 推算下次执行时间）
     */
    ApiSchedule scheduleSave(String id, String apiCode, String cron, Integer enabled, String remark, String username);

    /**
     * 删除定时配置
     */
    void scheduleDelete(String id, String username);

    // ---------------- 采集数据 ----------------

    /**
     * 采集数据分页（可按 apiCode 筛选，按采集时间倒序）
     */
    Page<ApiData> dataPage(long pageNo, long pageSize, String apiCode);

    /**
     * 删除单条采集数据
     */
    void dataDelete(String id, String username);
}
