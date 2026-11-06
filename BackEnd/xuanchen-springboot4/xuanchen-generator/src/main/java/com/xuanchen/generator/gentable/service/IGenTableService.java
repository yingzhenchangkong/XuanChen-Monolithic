package com.xuanchen.generator.gentable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;

import java.util.List;

/**
 * Service接口-->数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
public interface IGenTableService extends IService<GenTable> {

    /**
     * 保存表配置（新增/修改），含输出目录与标识符校验。
     *
     * @param table 表配置
     * @return 表配置主键（新增时由 MP 生成）
     */
    String saveTableConfig(GenTable table);

    /**
     * 保存表配置 + 主表字段 + 多个子表（含各自字段），同一事务，
     * 避免「表配置已改、字段未改」的半成品。
     *
     * @param table       表配置
     * @param mainColumns 主表字段（null 表示不改动）
     * @param subTables   子表配置及字段（null 表示不改动，空数组表示清空全部子表）
     * @return 表配置主键
     */
    String saveWithColumns(GenTable table, List<GenTableColumn> mainColumns, List<GenTableSub> subTables);

    /**
     * 全量覆盖保存表的字段及界面配置（先删旧再批量插入），全部按主表字段处理。
     *
     * @param tableId gen_table 主键
     * @param columns 字段配置列表（可为空列表，表示清空配置）
     */
    void saveColumns(String tableId, List<GenTableColumn> columns);

    /**
     * 回收站 列表
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<GenTable> listRecycleBin(Page<GenTable> page, GenTable query);

    /**
     * 回收站 彻底删除（单条/多条，逗号分隔）
     *
     * @param ids 主键串
     */
    void deleteRecycleBin(String ids);

    /**
     * 回收站 还原（单条/多条，逗号分隔）
     *
     * @param ids 主键串
     */
    void revertRecycleBin(String ids);
}
