package com.xuanchen.generator.gentable.dto;

import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 请求体 --> 表配置 + 主表字段 + 多子表（含各自字段）一次性保存（同一事务）。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
@Data
public class GenTableSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 表配置 */
    private GenTable table;

    /** 主表字段及界面配置（null 表示不改动，空数组表示清空） */
    private List<GenTableColumn> columns;

    /** 子表配置 + 各子表字段（null 表示不改动，空数组表示清空全部子表） */
    private List<GenTableSub> subTables;

    /**
     * 旧版单子表字段（兼容老前端）：subTables 为空且本字段非空时，按单个子表处理
     */
    private List<GenTableColumn> subColumns;
}
