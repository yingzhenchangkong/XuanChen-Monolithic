package com.xuanchen.generator.gentable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;
import com.xuanchen.generator.gentable.mapper.GenTableSubMapper;
import com.xuanchen.generator.gentable.service.IGenTableColumnService;
import com.xuanchen.generator.gentable.service.IGenTableSubService;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service接口实现类-->代码生成子表配置
 *
 * @author XuanChen
 * @date 2026-10-20
 */
@Service
public class GenTableSubServiceImpl extends ServiceImpl<GenTableSubMapper, GenTableSub>
        implements IGenTableSubService {

    /** 子表字段标记 */
    public static final String SCOPE_SUB = "sub";

    @Resource
    private IGenTableColumnService genTableColumnService;
    @Resource
    private GeneratorSafetyValidator generatorSafetyValidator;

    @Override
    public List<GenTableSub> listByTable(GenTable table) {
        if (table == null || table.getId() == null || table.getId().isBlank()) {
            return List.of();
        }
        List<GenTableSub> subs = list(new QueryWrapper<GenTableSub>()
                .eq("table_id", table.getId()).orderByAsc("order_no"));
        if (subs.isEmpty() && table.getSubTableName() != null && !table.getSubTableName().isBlank()) {
            // 历史配置：还没有 gen_table_sub 记录时，按旧的两个单值列构造一个虚拟子表
            GenTableSub legacy = new GenTableSub();
            legacy.setTableId(table.getId());
            legacy.setSubTableName(table.getSubTableName());
            legacy.setSubFkName(table.getSubTableFkName());
            legacy.setOrderNo(0);
            subs = List.of(legacy);
        }
        for (GenTableSub sub : subs) {
            sub.setColumns(loadColumns(table.getId(), sub.getId()));
        }
        return subs;
    }

    /**
     * 读某子表的字段配置。subId 为空（历史单子表）时按 column_scope=sub 且 sub_id 为空兜底。
     */
    private List<GenTableColumn> loadColumns(String tableId, String subId) {
        QueryWrapper<GenTableColumn> wrapper = new QueryWrapper<GenTableColumn>()
                .eq("table_id", tableId).orderByAsc("order_no");
        if (subId == null || subId.isBlank()) {
            wrapper.eq("column_scope", SCOPE_SUB).isNull("sub_id");
        } else {
            wrapper.eq("sub_id", subId);
        }
        return genTableColumnService.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceSubTables(String tableId, List<GenTableSub> subTables) {
        if (tableId == null || tableId.isBlank()) {
            throw new BusinessException(400, "缺少生成表ID！");
        }
        if (subTables == null) {
            return;
        }
        validate(subTables);
        // 先清掉旧子表字段（sub_id 非空）与旧子表配置，再按提交顺序重建：
        // 子表 id 会重新生成，字段的 sub_id 必须跟着新 id 走，所以不能只改子表不动字段
        genTableColumnService.remove(new QueryWrapper<GenTableColumn>()
                .eq("table_id", tableId).isNotNull("sub_id"));
        remove(new QueryWrapper<GenTableSub>().eq("table_id", tableId));

        int order = 0;
        for (GenTableSub sub : subTables) {
            sub.setId(null);
            sub.setTableId(tableId);
            sub.setOrderNo(order++);
            if (sub.getStatus() == null) {
                sub.setStatus(1);
            }
            sub.setDelFlag(0);
            save(sub);

            List<GenTableColumn> columns = sub.getColumns();
            if (columns == null) {
                continue;
            }
            List<GenTableColumn> normalized = new ArrayList<>();
            for (GenTableColumn column : columns) {
                if (column == null) {
                    continue;
                }
                generatorSafetyValidator.validateDbIdentifier(column.getColumnName(), "字段名");
                column.setTableId(tableId);
                column.setSubId(sub.getId());
                column.setColumnScope(SCOPE_SUB);
                column.setId(null);
                normalized.add(column);
            }
            if (!normalized.isEmpty()) {
                genTableColumnService.saveBatch(normalized);
            }
        }
    }

    /**
     * 保存前校验：子表名/外键名必填且为合法标识符，同一主表内子表名不允许重复。
     */
    private void validate(List<GenTableSub> subTables) {
        Set<String> seen = new HashSet<>();
        for (GenTableSub sub : subTables) {
            if (sub == null) {
                continue;
            }
            if (sub.getSubTableName() == null || sub.getSubTableName().isBlank()) {
                throw new BusinessException(400, "子表名不能为空（不需要子表请删除该行）");
            }
            if (sub.getSubFkName() == null || sub.getSubFkName().isBlank()) {
                throw new BusinessException(400, "子表「" + sub.getSubTableName() + "」的外键列不能为空");
            }
            generatorSafetyValidator.validateDbIdentifier(sub.getSubTableName(), "子表名");
            generatorSafetyValidator.validateDbIdentifier(sub.getSubFkName(), "子表外键名");
            String key = sub.getSubTableName().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw new BusinessException(400, "子表「" + sub.getSubTableName() + "」重复配置，请合并为一条");
            }
        }
    }
}
