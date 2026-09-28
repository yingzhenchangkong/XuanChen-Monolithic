package com.xuanchen.system.sysdict.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysdict.entity.SysDict;
import com.xuanchen.system.sysdict.entity.SysDictItem;
import com.xuanchen.system.sysdict.service.ISysDictItemService;
import com.xuanchen.system.sysdict.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class SysDictController {
    private final ISysDictService sysDictService;
    private final ISysDictItemService sysDictItemService;

    /**
     * 分页列表查询
     *
     * @param sysDict
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    public Result<IPage<SysDict>> list(SysDict sysDict,
                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
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
     * 添加
     *
     * @param sysDict
     * @return
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody SysDict sysDict) {
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
    public Result<String> edit(@RequestBody SysDict sysDict) {
        sysDictService.updateById(sysDict);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     * 事务约束：字典项与字典主表必须同一事务删除，字典项已删而主表删除失败会留下
     * "查不到字典但字典项残留"的不一致数据。
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        SysDict sysDict = sysDictService.getById(id);
        if (sysDict == null) {
            return Result.badRequest("字典不存在或已被删除！");
        }
        QueryWrapper<SysDictItem> queryWrapperItem = new QueryWrapper<>();
        queryWrapperItem.eq("dict_code", sysDict.getDictCode());
        sysDictItemService.remove(queryWrapperItem);
        sysDictService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 子项分页列表查询
     *
     * @param sysDictItem
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listItem")
    public Result<IPage<SysDictItem>> listItem(SysDictItem sysDictItem,
                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
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
    public Result<String> addItem(@RequestBody SysDictItem sysDictItem) {
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
    public Result<String> editItem(@RequestBody SysDictItem sysDictItem) {
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
    public Result<String> deleteItem(@RequestParam(name = "id", required = true) String id) {
        sysDictItemService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    @PreAuthorize("isAuthenticated()")
    public Result<List<SysDictItem>> select(@RequestParam(name = "dictCode") String dictCode) {
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
     * 状态修改
     *
     * @param sysDict
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> changeStatus(@RequestBody SysDict sysDict) {
        UpdateWrapper<SysDict> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysDict.getStatus()).eq("id", sysDict.getId());
        sysDictService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }

    /**
     * 状态修改
     *
     * @param sysDictItem
     * @return
     */
    @RequestMapping(value = "/changeStatusItem", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> changeStatusItem(@RequestBody SysDictItem sysDictItem) {
        UpdateWrapper<SysDictItem> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysDictItem.getStatus()).eq("id", sysDictItem.getId());
        sysDictItemService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 DICT参数 是否已存在
     *
     * @param sysDict
     * @return
     */
    @GetMapping("/validate")
    public Result<String> validate(SysDict sysDict) {
        boolean exists = StringUtil.isEmpty(sysDict.getId())
                ? sysDictService.ifExistsNoId(sysDict)
                : sysDictService.ifExistsId(sysDict);
        return exists
                ? Result.conflict(TipConst.PARAM_EXISTS)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }

    /**
     * 校验 ITEM参数 是否已存在
     *
     * @param sysDictItem
     * @return
     */
    @GetMapping("/validateItem")
    public Result<String> validateItem(SysDictItem sysDictItem) {
        boolean exists = StringUtil.isEmpty(sysDictItem.getId())
                ? sysDictItemService.ifExistsNoId(sysDictItem)
                : sysDictItemService.ifExistsId(sysDictItem);
        return exists
                ? Result.conflict(TipConst.PARAM_EXISTS)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }
}