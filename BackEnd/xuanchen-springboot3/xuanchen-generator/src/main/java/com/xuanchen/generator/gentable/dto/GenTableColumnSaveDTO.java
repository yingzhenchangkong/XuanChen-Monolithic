package com.xuanchen.generator.gentable.dto;

import com.xuanchen.generator.gentable.entity.GenTableColumn;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 请求体 --> 批量保存表字段及界面配置
 *
 * @author XuanChen
 * @date 2026-10-18
 */
@Data
public class GenTableColumnSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** gen_table 主键 */
    private String tableId;

    /** 字段及界面配置（全量覆盖保存） */
    private List<GenTableColumn> columns;
}
