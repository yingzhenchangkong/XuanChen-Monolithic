package com.xuanchen.generator.gentable.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.service.IGenTableColumnService;
import com.xuanchen.generator.gentable.service.IGenTableService;
import com.xuanchen.generator.utils.DBUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/tool/generator/table")
public class GenTableController {
    @Autowired
    private IGenTableService genTableService;
    @Autowired
    private IGenTableColumnService genTableColumnService;

    /**
     * 列表查询
     *
     * @param genTable
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result list(GenTable genTable,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<GenTable> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order_no");
        Page<GenTable> page = new Page<>(pageNo, pageSize);
        IPage<GenTable> pageList = genTableService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 列表查询
     *
     * @param tableId
     * @return
     */
    @GetMapping("/listByTableId")
    public Result listByTableId(String tableId) {
        QueryWrapper<GenTableColumn> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_id", tableId);
        List<GenTableColumn> list = genTableColumnService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 添加
     *
     * @param genTable
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody GenTable genTable) {
        genTableService.save(genTable);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param genTable
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody GenTable genTable) {
        genTableService.updateById(genTable);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result delete(@RequestParam(name = "id", required = true) String id) {
        genTableService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 通过id批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        genTableService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 获取数据库表列表
     *
     * @param genDatabase
     * @return
     */
    @GetMapping("/getTableList")
    public Result getTableList(GenDatabase genDatabase) {
        List<Map<String, Object>> list = DBUtil.getTableList(genDatabase);
        return Result.success(list);
    }

    /**
     * 获取数据库表信息
     *
     * @param genDatabase
     * @return
     */
    @GetMapping("/getTableInfo")
    public Result getTableInfo(GenDatabase genDatabase, @RequestParam(name = "tableName") String tableName) {
        List<Map<String, Object>> list = DBUtil.getTableInfo(genDatabase, tableName);
        return Result.success(list);
    }
}
