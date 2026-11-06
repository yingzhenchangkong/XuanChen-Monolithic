package com.xuanchen.common.datasource;

import com.xuanchen.common.entity.DatasourceInfo;
import com.xuanchen.common.entity.DatasourceOption;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 统一数据源读取 SPI（sys_datasource）。
 * <p>
 * 设计背景：实体、Mapper、加密与解密都归 xuanchen-system 独占，其余业务模块
 * （report / api / visualization / generator）只声明本接口的依赖，由 system 模块
 * 提供实现（{@code SysDatasourceProvider}），同进程内直接方法调用。
 * <p>
 * 为什么不走「HTTP 调自己的 /internal 端点」：单体应用里自调用会多占一倍线程、
 * 依赖 server.port 与协议、把明文口令放到网络上，还要为内部端点开口子绕过鉴权。
 * 用接口注入可以全部规避，且将来真要拆服务时只需替换实现。
 * <p>
 * 返回的 {@link DatasourceInfo#getPassword()} 仅 {@link #get(String)} 会填充明文，
 * 调用方禁止回传前端或落日志；{@link #listEnabled()} / {@link #getBatch(java.util.Collection)}
 * 用于下拉与名称回填，password 恒为 null，避免为了展示名称而解密全表口令。
 *
 * @author XuanChen
 * @date 2026-11-05
 */
public interface DatasourceProvider {

    /**
     * 所有启用状态的数据源（不含密码），按创建时间倒序。
     */
    List<DatasourceInfo> listEnabled();

    /**
     * 数据源下拉选项（不含密码、连接串）。四个业务模块统一调用本方法，不再各自 map。
     */
    List<DatasourceOption> listOptions();

    /**
     * 按 id 取单个已启用的数据源（含解密后的明文密码）。
     *
     * @throws com.xuanchen.common.exception.BusinessException 不存在、已删除或已停用时抛出
     */
    DatasourceInfo get(String id);

    /**
     * 批量取（只返回存在的，key 为 id）。列表页回填数据源名称用，避免逐行查询。
     */
    Map<String, DatasourceInfo> getBatch(Collection<String> ids);
}
