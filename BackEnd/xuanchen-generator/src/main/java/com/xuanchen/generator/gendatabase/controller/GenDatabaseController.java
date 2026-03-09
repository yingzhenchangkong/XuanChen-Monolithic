package com.xuanchen.generator.gendatabase.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.fastexcel.FastExcelListener;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;
import com.xuanchen.generator.gendatabase.service.IGenDatabaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 控制器 --> 数据库
 *
 * @author XuanChen
 * @date 2026-02-18
 */
@RestController
@RequestMapping("/tool/generator/database")
public class GenDatabaseController {
    @Autowired
    private IGenDatabaseService genDatabaseService;

    /**
     * 数据库连接列表查询
     *
     * @param genDatabase
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result list(GenDatabase genDatabase,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<GenDatabase> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("order_no");
        Page<GenDatabase> page = new Page<>(pageNo, pageSize);
        IPage<GenDatabase> pageList = genDatabaseService.page(page, queryWrapper);
        return Result.success(pageList);
    }
    /**
     * 获取单个数据库连接信息
     *
     * @param id
     * @return
     */
    @GetMapping("/getOneById")
    public Result getOneById(@RequestParam(name = "id", required = true) String id) {
        GenDatabase genDatabase = genDatabaseService.getById(id);
        return Result.success(genDatabase);
    }
    /**
     * 添加
     *
     * @param genDatabase
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody GenDatabase genDatabase) {
        genDatabaseService.save(genDatabase);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param genDatabase
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody GenDatabase genDatabase) {
        genDatabaseService.updateById(genDatabase);
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
        genDatabaseService.removeById(id);
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
        genDatabaseService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 导出excel
     *
     * @param response
     * @throws IOException
     */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<GenDatabase> list = genDatabaseService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "角色管理";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(GenDatabase.class)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet(title)
                .doWrite(list);
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result importExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String message = "";
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile multipartFile = entity.getValue();
            InputStream inputStream = multipartFile.getInputStream();
            FastExcelListener<GenDatabase> listenter = new FastExcelListener<>();
            FastExcel.read(inputStream, GenDatabase.class, listenter)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<GenDatabase> dataList = listenter.getDataList();
            List<GenDatabase> listGenDatabase = new ArrayList<>();
            for (GenDatabase genDatabase : dataList) {
                listGenDatabase.add(genDatabase);
            }
            genDatabaseService.saveBatch(listGenDatabase);
            Integer totcal = dataList.size();
            Integer success = listGenDatabase.size();
            Integer fail = totcal - success;
            message = "共" + totcal + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param genDatabase
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listRecycleBin")
    public Result listRecycleBin(GenDatabase genDatabase,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<GenDatabase> page = new Page<>(pageNo, pageSize);
        IPage<GenDatabase> pageList = genDatabaseService.listRecycleBin(page, genDatabase);
        return Result.success(pageList);
    }

    /**
     * 回收站 彻底删除 单条
     *
     * @param id
     * @return
     */
    @DeleteMapping("/deleteRecycleBin")
    public Result deleteRecycleBin(@RequestParam("id") String id) {
        genDatabaseService.deleteRecycleBin(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 彻底删除 多条
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/deleteRecycleBinBatch")
    public Result deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        genDatabaseService.deleteRecycleBin(ids);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 还原 单条
     *
     * @param map
     * @return
     */
    @PutMapping("/revertRecycleBin")
    public Result revertRecycleBin(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        genDatabaseService.revertRecycleBin(id);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 回收站 还原 多条
     *
     * @param map
     * @return
     */
    @PutMapping("/revertRecycleBinBatch")
    public Result revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        String ids = map.get("ids");
        genDatabaseService.revertRecycleBin(ids);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    public Result select() {
        QueryWrapper<GenDatabase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", false).eq("status", true).orderByAsc("order_no");
        List<GenDatabase> list = genDatabaseService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 状态修改
     *
     * @param genDatabase
     * @param request
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result changeStatus(@RequestBody GenDatabase genDatabase, HttpServletRequest request) {
        UpdateWrapper<GenDatabase> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", genDatabase.getStatus()).eq("id", genDatabase.getId());
        genDatabaseService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }
}
