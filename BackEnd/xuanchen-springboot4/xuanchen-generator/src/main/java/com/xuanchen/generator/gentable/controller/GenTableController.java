package com.xuanchen.generator.gentable.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gendatabase.entity.GenDatasource;
import com.xuanchen.generator.gendatabase.service.GenDatasourceResolver;
import com.xuanchen.generator.gentable.dto.GenTableColumnSaveDTO;
import com.xuanchen.generator.gentable.dto.GenTableSaveDTO;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;
import com.xuanchen.generator.gentable.service.IGenTableColumnService;
import com.xuanchen.generator.gentable.service.IGenTableService;
import com.xuanchen.generator.gentable.service.IGenTableSubService;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import com.xuanchen.generator.utils.DBUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 控制器 --> 数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@RestController
@PreAuthorize("hasRole('admin')")
@RequestMapping("/tool/generator/table")
public class GenTableController {
    @Autowired
    private IGenTableService genTableService;
    @Autowired
    private IGenTableColumnService genTableColumnService;
    @Autowired
    private IGenTableSubService genTableSubService;
    @Autowired
    private GenDatasourceResolver genDatasourceResolver;
    @Autowired
    private GeneratorSafetyValidator generatorSafetyValidator;
    @Autowired
    private com.xuanchen.generator.utils.DBUtil dbUtil;

    /**
     * 列表查询（表名/表注释模糊匹配）
     *
     * @param genTable 查询条件
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param req      请求
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<GenTable>> list(GenTable genTable,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<GenTable> queryWrapper = new QueryWrapper<>();
        // 之前只做了排序、忽略入参，导致页面查询区形同虚设
        if (genTable.getTableName() != null && !genTable.getTableName().isBlank()) {
            queryWrapper.like("table_name", genTable.getTableName().trim());
        }
        if (genTable.getTableComment() != null && !genTable.getTableComment().isBlank()) {
            queryWrapper.like("table_comment", genTable.getTableComment().trim());
        }
        queryWrapper.orderByAsc("order_no").orderByDesc("create_time");
        Page<GenTable> page = new Page<>(pageNo, pageSize);
        IPage<GenTable> pageList = genTableService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 列表 获取数据库表字段
     *
     * @param dataBaseId 数据源 id
     * @param tableName  物理表名
     * @return 已保存的字段配置（含主/子表）
     */
    @GetMapping("/listTableColumn")
    public Result<List<GenTableColumn>> listTableColumn(@RequestParam(name = "dataBaseId") String dataBaseId,
                                                        @RequestParam(name = "tableName") String tableName) {
        GenTable genTable = genTableService.getOne(new QueryWrapper<GenTable>()
                .eq("database_id", dataBaseId)
                .eq("table_name", tableName)
                .last("limit 1"));
        List<GenTableColumn> list = new ArrayList<>();
        if (genTable != null) {
            list = genTableColumnService.list(new QueryWrapper<GenTableColumn>()
                    .eq("table_id", genTable.getId()).orderByAsc("order_no"));
        }
        return Result.success(list);
    }

    /**
     * 按 gen_table 主键查询已保存的字段及界面配置（编辑回显用，含主表与子表）。
     *
     * @param id gen_table 主键
     * @return 字段配置列表
     */
    @GetMapping("/listTableColumnByTableId")
    public Result<List<GenTableColumn>> listTableColumnByTableId(@RequestParam(name = "id", required = true) String id) {
        return Result.success(genTableColumnService.list(new QueryWrapper<GenTableColumn>()
                .eq("table_id", id).orderByAsc("order_no")));
    }

    /**
     * 全量覆盖保存表的字段及界面配置（先删旧再批量插入，全部按主表字段处理）。
     *
     * @param dto tableId + columns
     * @return 保存结果
     */
    @PostMapping("/saveTableColumn")
    public Result<String> saveTableColumn(@RequestBody GenTableColumnSaveDTO dto) {
        genTableService.saveColumns(dto.getTableId(), dto.getColumns());
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 查询某主表配置下的全部子表（含各子表字段配置），编辑回显用。
     *
     * @param tableId gen_table 主键
     * @return 子表配置列表（按 order_no 升序）
     */
    @GetMapping("/listSubTable")
    public Result<List<GenTableSub>> listSubTable(@RequestParam(name = "tableId", required = true) String tableId) {
        GenTable table = genTableService.getById(tableId);
        if (table == null) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(genTableSubService.listByTable(table));
    }

    /**
     * 表配置 + 主表字段 + 多子表（含字段）一次性保存（同一事务）。
     * <p>
     * 取代原先「先 add/edit、再 saveTableColumn」的两次请求，避免第二步失败留下半成品。
     *
     * @param dto 表配置 + 字段
     * @return 表配置主键
     */
    @PostMapping("/saveWithColumns")
    public Result<String> saveWithColumns(@RequestBody GenTableSaveDTO dto) {
        if (dto == null || dto.getTable() == null) {
            throw new BusinessException(400, "缺少表配置");
        }
        // 注意：Result.success(id) 会命中 success(String msg) 重载，把主键当成提示语返回，
        // 前端就会弹出一串数字；这里必须显式给出 (msg, data)。
        boolean isAdd = dto.getTable().getId() == null || dto.getTable().getId().isBlank();
        String id = genTableService.saveWithColumns(dto.getTable(), dto.getColumns(),
                resolveSubTables(dto));
        return Result.success(isAdd ? TipConst.ADD_SUCC : TipConst.EDIT_SUCC, id);
    }

    /**
     * 子表入参归一：优先用 subTables；老前端只传 subColumns 时按单个子表包装。
     * 两者都为 null 表示「不改动子表配置」，直接返回 null。
     */
    private List<GenTableSub> resolveSubTables(GenTableSaveDTO dto) {
        if (dto.getSubTables() != null) {
            return dto.getSubTables();
        }
        if (dto.getSubColumns() == null) {
            return null;
        }
        GenTableSub sub = new GenTableSub();
        sub.setSubTableName(dto.getTable().getSubTableName());
        sub.setSubFkName(dto.getTable().getSubTableFkName());
        sub.setColumns(dto.getSubColumns());
        return List.of(sub);
    }

    /**
     * 添加（仅表配置，字段另行保存）
     *
     * @param genTable 表配置
     * @return 新表配置 id：前端保存字段界面配置时需要 tableId
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody GenTable genTable) {
        return Result.success(TipConst.ADD_SUCC, genTableService.saveTableConfig(genTable));
    }

    /**
     * 修改（仅表配置，字段另行保存）
     *
     * @param genTable 表配置
     * @return 表配置 id
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody GenTable genTable) {
        if (genTable.getId() == null || genTable.getId().isBlank()) {
            return Result.badRequest("缺少生成表ID！");
        }
        return Result.success(TipConst.EDIT_SUCC, genTableService.saveTableConfig(genTable));
    }

    /**
     * 通过id删除
     *
     * @param id 主键
     * @return 结果
     */
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        genTableService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 通过id批量删除
     *
     * @param ids 逗号分隔主键
     * @return 结果
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        genTableService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 回收站 列表
     *
     * @param genTable 查询条件
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param req      请求
     * @return 分页结果
     */
    @GetMapping("/listRecycleBin")
    public Result<IPage<GenTable>> listRecycleBin(GenTable genTable,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<GenTable> page = new Page<>(pageNo, pageSize);
        return Result.success(genTableService.listRecycleBin(page, genTable));
    }

    /**
     * 回收站 彻底删除 单条
     *
     * @param id 主键
     * @return 结果
     */
    @DeleteMapping("/deleteRecycleBin")
    public Result<String> deleteRecycleBin(@RequestParam("id") String id) {
        genTableService.deleteRecycleBin(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 彻底删除 多条
     *
     * @param ids 逗号分隔主键
     * @return 结果
     */
    @DeleteMapping("/deleteRecycleBinBatch")
    public Result<String> deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        genTableService.deleteRecycleBin(ids);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 还原 单条
     *
     * @param map id
     * @return 结果
     */
    @PutMapping("/revertRecycleBin")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
        genTableService.revertRecycleBin(map.get("id"));
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 回收站 还原 多条
     *
     * @param map ids
     * @return 结果
     */
    @PutMapping("/revertRecycleBinBatch")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        genTableService.revertRecycleBin(map.get("ids"));
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 统一数据源下拉：返回 sys_datasource 中方言支持（mysql/postgresql）且启用的数据源。
     */
    @GetMapping("/selectDatasource")
    public Result<List<GenDatasource>> selectDatasource() {
        return Result.success(genDatasourceResolver.select());
    }

    /**
     * 获取数据库表列表。连接信息由 GenDatasourceResolver 从 sys_datasource 完整读取，
     * 请求参数中的连接信息一律不采信，防止 SSRF。
     */
    @GetMapping("/listDBTable")
    public Result<List<Map<String, Object>>> listDBTable(@RequestParam(name = "id", required = false) String id) {
        GenDatasource genDatabase = requireSavedDatabase(id);
        return Result.success(dbUtil.getTableList(genDatabase));
    }

    /**
     * 获取数据库表字段配置信息。连接信息同样只按已保存数据源 id 读取。
     */
    @GetMapping("/listDBTableColumn")
    public Result<List<Map<String, Object>>> listDBTableColumn(@RequestParam(name = "id", required = true) String id,
                                    @RequestParam(name = "tableName", required = true) String tableName) {
        GenDatasource genDatabase = requireSavedDatabase(id);
        generatorSafetyValidator.validateTableName(tableName);
        return Result.success(dbUtil.getTableInfo(genDatabase, tableName));
    }

    /**
     * 按主键加载数据源（即 sys_datasource.id）；不存在/已停用/方言不支持时拒绝。
     */
    private GenDatasource requireSavedDatabase(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(400, "缺少数据源 id，仅允许读取已保存的数据源");
        }
        return genDatasourceResolver.resolve(id);
    }
}
