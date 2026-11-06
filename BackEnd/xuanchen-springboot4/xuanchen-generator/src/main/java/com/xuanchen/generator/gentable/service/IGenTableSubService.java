package com.xuanchen.generator.gentable.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableSub;

import java.util.List;

/**
 * Service接口-->代码生成子表配置
 *
 * @author XuanChen
 * @date 2026-10-20
 */
public interface IGenTableSubService extends IService<GenTableSub> {

    /**
     * 查询某主表配置下的全部子表（按 order_no 升序，含各子表字段配置）。
     * <p>
     * 兼容历史数据：gen_table_sub 还没记录时会回落读取 gen_table.sub_table_name /
     * sub_table_fk_name，视为只有一个子表，保证老配置不改也能继续生成。
     *
     * @param table 主表配置
     * @return 子表配置列表（无子表时返回空列表）
     */
    List<GenTableSub> listByTable(GenTable table);

    /**
     * 全量覆盖保存某主表配置的子表（先删旧配置与旧子表字段，再按顺序重建）。
     *
     * @param tableId   主表配置ID
     * @param subTables 子表配置列表（null 表示不改动，空数组表示清空全部子表）
     */
    void replaceSubTables(String tableId, List<GenTableSub> subTables);
}
