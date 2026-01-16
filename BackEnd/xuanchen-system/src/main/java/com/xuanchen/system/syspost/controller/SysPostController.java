package com.xuanchen.system.syspost.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.fastexcel.FastExcelListener;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.syspost.entity.SysPost;
import com.xuanchen.system.syspost.service.ISysPostService;
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
 * 控制器-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/system/post")
public class SysPostController {
    @Autowired
    private ISysPostService sysPostService;

    /**
     * 分页列表查询
     *
     * @param sysPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result list(SysPost sysPost,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<SysPost> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(sysPost.getPostName())) {
            queryWrapper.like("post_name", sysPost.getPostName());
        }
        if (StringUtil.isNotEmpty(sysPost.getPostDescription())) {
            queryWrapper.like("post_description", sysPost.getPostDescription());
        }
        queryWrapper.orderByAsc("order_no");
        Page<SysPost> page = new Page<>(pageNo, pageSize);
        IPage<SysPost> pageList = sysPostService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    public Result select() {
        QueryWrapper<SysPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", false).eq("status", true).orderByAsc("order_no");
        List<SysPost> list = sysPostService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 添加
     *
     * @param sysPost
     * @return
     */
    @PostMapping(value = "/add")
    public Result add(@RequestBody SysPost sysPost) {
        sysPostService.save(sysPost);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysPost
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysPost sysPost) {
        sysPostService.updateById(sysPost);
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
        sysPostService.removeById(id);
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
        sysPostService.removeByIds(Arrays.asList(ids.split(",")));
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
        List<SysPost> list = sysPostService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "岗位管理";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(SysPost.class)
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
            FastExcelListener<SysPost> listenter = new FastExcelListener<>();
            FastExcel.read(inputStream, SysPost.class, listenter)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<SysPost> dataList = listenter.getDataList();
            List<SysPost> listSysPost = new ArrayList<>();
            for (SysPost sysPost : dataList) {
                SysPost sysPostTemp = sysPostService.getOne(new QueryWrapper<SysPost>().eq("post_code", sysPost.getPostCode()));
                if (sysPostTemp != null) {
                    continue;
                }
                listSysPost.add(sysPostTemp);
            }
            sysPostService.saveBatch(listSysPost);
            Integer totcal = dataList.size();
            Integer success = listSysPost.size();
            Integer fail = totcal - success;
            message = "共" + totcal + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param sysPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listRecycleBin")
    public Result listRecycleBin(SysPost sysPost,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<SysPost> page = new Page<>(pageNo, pageSize);
        IPage<SysPost> pageList = sysPostService.listRecycleBin(page, sysPost);
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
        sysPostService.deleteRecycleBin(id);
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
        sysPostService.deleteRecycleBin(ids);
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
        sysPostService.revertRecycleBin(id);
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
        sysPostService.revertRecycleBin(ids);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 校验 岗位编码 是否已存在
     *
     * @param sysPost
     * @return
     */
    @GetMapping("/validatePostCode")
    public Result validatePostCode(SysPost sysPost) {
        boolean exists = StringUtil.isEmpty(sysPost.getId())
                ? sysPostService.exists(new LambdaQueryWrapper<SysPost>().eq(SysPost::getPostCode, sysPost.getPostCode()))
                : sysPostService.validate(sysPost);

        return exists
                ? Result.error("岗位编码已存在！")
                : Result.success("岗位编码可用！");
    }

    /**
     * 校验 岗位名称 是否已存在
     *
     * @param sysPost
     * @return
     */
    @GetMapping("/validatePostName")
    public Result validatePostName(SysPost sysPost) {
        boolean exists = StringUtil.isEmpty(sysPost.getId())
                ? sysPostService.exists(new LambdaQueryWrapper<SysPost>().eq(SysPost::getPostName, sysPost.getPostName()))
                : sysPostService.validate(sysPost);

        return exists
                ? Result.error("岗位名称已存在！")
                : Result.success("岗位名称可用！");
    }
}
