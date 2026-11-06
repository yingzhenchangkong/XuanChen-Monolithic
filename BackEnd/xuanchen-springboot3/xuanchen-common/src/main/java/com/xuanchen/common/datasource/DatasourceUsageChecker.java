package com.xuanchen.common.datasource;

/**
 * 数据源引用检查 SPI：删除数据源前，由各业务模块上报自己是否仍引用该数据源。
 * <p>
 * 放在 common 是为了保持「业务模块只依赖 common、system 不反向依赖业务模块」的方向：
 * report / api / visualization / generator 各自提供实现，system 的删除接口注入
 * {@code List<DatasourceUsageChecker>} 汇总，无需跨模块读表或 JOIN。
 *
 * @author XuanChen
 * @date 2026-11-06
 */
public interface DatasourceUsageChecker {

    /** 模块显示名，用于拼装拒绝删除的提示 */
    String module();

    /** 返回该数据源在本模块中的引用条数；查询异常时应返回 0 并自行记录日志 */
    long count(String dsId);

    /** 提示文案 */
    default String describe(long count) {
        return module() + "（" + count + " 处引用）";
    }
}
