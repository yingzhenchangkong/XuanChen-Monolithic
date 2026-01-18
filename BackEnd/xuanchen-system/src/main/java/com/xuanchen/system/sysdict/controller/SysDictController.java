package com.xuanchen.system.sysdict.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysdict.entity.SysDict;
import com.xuanchen.system.sysdict.entity.SysDictItem;
import com.xuanchen.system.sysdict.service.ISysDictItemService;
import com.xuanchen.system.sysdict.service.ISysDictService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制器-->字典
 *
 * @author XuanChen
 * @date 2025-06-03
 */
@RestController
@RequestMapping("/system/dict")
public class SysDictController {
    @Autowired
    private ISysDictService sysDictService;
    @Autowired
    private ISysDictItemService sysDictItemService;

    /**
     * 分页列表查询
     *
     * @param sysDict
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result list(SysDict sysDict,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<SysDict> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(sysDict.getDictName())) {
            queryWrapper.like("dict_name", sysDict.getDictName());
        }
        queryWrapper.orderByAsc("order_no");
        Page<SysDict> page = new Page<>(pageNo, pageSize);
        IPage<SysDict> pageList = sysDictService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    public Result select(@RequestParam(name = "dictCode") String dictCode) {
        List<SysDictItem> list = new ArrayList<>();
        if (StringUtil.isNotEmpty(dictCode)) {
            QueryWrapper<SysDictItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("dict_code", dictCode);
            queryWrapper.orderByAsc("order_no");
            list = sysDictItemService.list(queryWrapper);
        }
        return Result.success(list);
    }

    /**
     * 添加
     *
     * @param sysDict
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody SysDict sysDict) {
        sysDictService.save(sysDict);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysDict
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysDict sysDict) {
        sysDictService.updateById(sysDict);
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
        SysDict sysDict = sysDictService.getById(id);
        QueryWrapper<SysDictItem> queryWrapperItem = new QueryWrapper<>();
        queryWrapperItem.eq("dict_code", sysDict.getDictCode());
        sysDictItemService.remove(queryWrapperItem);
        sysDictService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 校验 参数 是否已存在
     *
     * @param sysDict
     * @return
     */
    @GetMapping("/validate")
    public Result validate(SysDict sysDict) {
        boolean exists = StringUtil.isEmpty(sysDict.getId())
                ? sysDictService.ifExistsNoId(sysDict)
                : sysDictService.ifExistsId(sysDict);
        return exists
                ? Result.error()
                : Result.success();
    }

    /**
     * 子项分页列表查询
     *
     * @param sysDictItem
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listItem")
    public Result listItem(SysDictItem sysDictItem,
                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                           HttpServletRequest req) {
        QueryWrapper<SysDictItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dict_code", sysDictItem.getDictCode());
        Page<SysDictItem> page = new Page<>(pageNo, pageSize);
        IPage<SysDictItem> pageList = sysDictItemService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 添加
     *
     * @param sysDictItem
     * @return
     */
    @PostMapping("/addItem")
    public Result addItem(@RequestBody SysDictItem sysDictItem) {
        sysDictItemService.save(sysDictItem);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysDictItem
     * @return
     */
    @RequestMapping(value = "/editItem", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result editItem(@RequestBody SysDictItem sysDictItem) {
        sysDictItemService.updateById(sysDictItem);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/deleteItem")
    public Result deleteItem(@RequestParam(name = "id", required = true) String id) {
        sysDictItemService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }
}