package com.xuanchen.generator.gentable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;
import com.xuanchen.generator.gentable.mapper.GenTableMapper;
import com.xuanchen.generator.gentable.service.IGenTableColumnService;
import com.xuanchen.generator.gentable.service.IGenTableService;
import com.xuanchen.generator.gentable.service.IGenTableSubService;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service接口实现类-->数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Service
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements IGenTableService {

    /** 主表字段标记 */
    public static final String SCOPE_MAIN = "main";
    /** 子表字段标记 */
    public static final String SCOPE_SUB = "sub";

    @Resource
    private GeneratorSafetyValidator generatorSafetyValidator;
    @Resource
    private IGenTableColumnService genTableColumnService;
    @Resource
    private IGenTableSubService genTableSubService;
    @Resource
    private GenTableMapper genTableMapper;

    @Override
    public String saveTableConfig(GenTable table) {
        if (table == null) {
            throw new BusinessException(400, "表配置不能为空");
        }
        validateConfig(table);
        // 前端新增时 id 传空字符串，必须归一为 null，否则 MP 不会生成雪花主键、会插入空串
        if (table.getId() == null || table.getId().isBlank()) {
            table.setId(null);
            save(table);
        } else if (!updateById(table)) {
            throw new BusinessException(400, "生成表不存在或已被删除！");
        }
        return table.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveWithColumns(GenTable table, List<GenTableColumn> mainColumns, List<GenTableSub> subTables) {
        String tableId = saveTableConfig(table);
        if (mainColumns != null) {
            replaceColumns(tableId, mainColumns, SCOPE_MAIN);
        }
        if (subTables != null) {
            // 旧的两个单值列已由 gen_table_sub 取代：这里必须清空，否则用户删光子表后，
            // listByTable 的兼容回落逻辑会拿旧值把子表重新"复活"
            update(new LambdaUpdateWrapper<GenTable>()
                    .eq(GenTable::getId, tableId)
                    .set(GenTable::getSubTableName, null)
                    .set(GenTable::getSubTableFkName, null));
            // 子表配置与子表字段由子表服务整体重建（含 column 的 sub_id 回填）
            genTableSubService.replaceSubTables(tableId, subTables);
        }
        return tableId;
    }

    /**
     * 全量替换主表字段（先删旧配置再批量插入）；子表字段带 sub_id，不会受影响。
     */
    private void replaceColumns(String tableId, List<GenTableColumn> columns, String scope) {
        List<GenTableColumn> normalized = normalize(tableId, columns, scope);
        genTableColumnService.remove(new QueryWrapper<GenTableColumn>()
                .eq("table_id", tableId).eq("column_scope", scope).isNull("sub_id"));
        if (!normalized.isEmpty()) {
            genTableColumnService.saveBatch(normalized);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumns(String tableId, List<GenTableColumn> columns) {
        if (tableId == null || tableId.isBlank()) {
            throw new BusinessException(400, "缺少生成表ID！");
        }
        if (getById(tableId) == null) {
            throw new BusinessException(400, "生成表不存在或已被删除！");
        }
        // 只替换主表字段：早期的全量 remove 会连带清掉子表字段（column_scope='sub'）
        replaceColumns(tableId, columns == null ? List.of() : columns, SCOPE_MAIN);
    }

    @Override
    public IPage<GenTable> listRecycleBin(Page<GenTable> page, GenTable query) {
        return page.setRecords(genTableMapper.listRecycleBin(page, query == null ? new GenTable() : query));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        genTableMapper.deleteRecycleBin(ids.split(","));
    }

    @Override
    public void revertRecycleBin(String ids) {
        genTableMapper.revertRecycleBin(ids.split(","));
    }

    /**
     * 保存前的安全校验：输出目录必须在服务器白名单根目录内，模块名/包名/表名必须是合法标识符，
     * 前后端相对输出路径必须位于输出目录之内且不含 ".."。
     */
    private void validateConfig(GenTable table) {
        generatorSafetyValidator.validateOutputConfig(
                table.getOutputDir(), table.getModuleName(), table.getPackageName());
        generatorSafetyValidator.validateTableName(table.getTableName());
        generatorSafetyValidator.validateOptionalIdentifier(table.getSubTableName(), "子表名");
        generatorSafetyValidator.validateOptionalIdentifier(table.getSubTableFkName(), "子表外键名");
        generatorSafetyValidator.validateRelativeOutputPath(table.getBackendRelPath(), "后端输出相对路径");
        generatorSafetyValidator.validateRelativeOutputPath(table.getFrontendRelPath(), "前端输出相对路径");
        if (table.getParentMenuId() != null && !table.getParentMenuId().isBlank()
                && !table.getParentMenuId().matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(400, "上级菜单ID仅允许字母、数字、下划线、连字符（最长 64 位）");
        }
    }

    /**
     * 列配置归一：校验列名、强制使用服务端 tableId、清空前端主键、写入归属标记。
     * 这里只处理主表字段，sub_id 必须显式清空，避免前端回传旧值把字段挂到某张子表上。
     */
    private List<GenTableColumn> normalize(String tableId, List<GenTableColumn> columns, String scope) {
        List<GenTableColumn> result = new ArrayList<>();
        for (GenTableColumn column : columns) {
            if (column == null) {
                continue;
            }
            generatorSafetyValidator.validateDbIdentifier(column.getColumnName(), "字段名");
            column.setTableId(tableId);
            column.setId(null);
            column.setColumnScope(scope);
            column.setSubId(null);
            result.add(column);
        }
        return result;
    }
}
