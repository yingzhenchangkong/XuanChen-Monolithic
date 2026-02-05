package com.xuanchen.system.sysconfig.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.aspect.annotation.LogOperation;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.fastexcel.FastExcelListener;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysconfig.entity.SysConfig;
import com.xuanchen.system.sysconfig.service.ISysConfigService;
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
 * 控制器-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/system/config")
public class SysConfigController {
    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 分页列表查询
     *
     * @param sysConfig
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @LogOperation(value = "配置信息分页列表", module = "SYSTEM")
    @GetMapping("/list")
    public Result list(SysConfig sysConfig,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(sysConfig.getConfigName())) {
            queryWrapper.like("config_name", sysConfig.getConfigName());
        }
        if (StringUtil.isNotEmpty(sysConfig.getConfigDescription())) {
            queryWrapper.like("config_description", sysConfig.getConfigDescription());
        }
        queryWrapper.orderByAsc("order_no");
        Page<SysConfig> page = new Page<>(pageNo, pageSize);
        IPage<SysConfig> pageList = sysConfigService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 添加
     *
     * @param sysConfig
     * @return
     */
    @PostMapping(value = "/add")
    public Result add(@RequestBody SysConfig sysConfig) {
        sysConfigService.save(sysConfig);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysConfig
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysConfig sysConfig) {
        sysConfigService.updateById(sysConfig);
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
        sysConfigService.removeById(id);
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
        sysConfigService.removeByIds(Arrays.asList(ids.split(",")));
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
        List<SysConfig> list = sysConfigService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "参数配置";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(SysConfig.class)
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
            FastExcelListener<SysConfig> listenter = new FastExcelListener<>();
            FastExcel.read(inputStream, SysConfig.class, listenter)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<SysConfig> dataList = listenter.getDataList();
            List<SysConfig> listSysConfig = new ArrayList<>();
            for (SysConfig sysConfig : dataList) {
                SysConfig sysConfigTemp = sysConfigService.getOne(new QueryWrapper<SysConfig>().eq("config_key", sysConfig.getConfigKey()));
                if (sysConfigTemp != null) {
                    continue;
                }
                listSysConfig.add(sysConfigTemp);
            }
            sysConfigService.saveBatch(listSysConfig);
            Integer totcal = dataList.size();
            Integer success = listSysConfig.size();
            Integer fail = totcal - success;
            message = "共" + totcal + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param sysConfig
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listRecycleBin")
    public Result listRecycleBin(SysConfig sysConfig,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<SysConfig> page = new Page<>(pageNo, pageSize);
        IPage<SysConfig> pageList = sysConfigService.listRecycleBin(page, sysConfig);
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
        sysConfigService.deleteRecycleBin(id);
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
        sysConfigService.deleteRecycleBin(ids);
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
        sysConfigService.revertRecycleBin(id);
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
        sysConfigService.revertRecycleBin(ids);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    public Result select() {
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", false).eq("status", true).orderByAsc("order_no");
        List<SysConfig> list = sysConfigService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 状态修改
     *
     * @param sysConfig
     * @param request
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result changeStatus(@RequestBody SysConfig sysConfig, HttpServletRequest request) {
        UpdateWrapper<SysConfig> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysConfig.getStatus()).eq("id", sysConfig.getId());
        sysConfigService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 参数 是否已存在
     *
     * @param sysConfig
     * @return
     */
    @GetMapping("/validate")
    public Result validate(SysConfig sysConfig) {
        boolean exists = StringUtil.isEmpty(sysConfig.getId())
                ? sysConfigService.ifExistsNoId(sysConfig)
                : sysConfigService.ifExistsId(sysConfig);
        return exists
                ? Result.error()
                : Result.success();
    }

    /**
     * 通过键获取值
     *
     * @param sysConfig
     * @return
     */
    @GetMapping("/getConfigKeyValue")
    public Result getConfigKeyValue(SysConfig sysConfig) {
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", sysConfig.getConfigKey());
        SysConfig sysConfigResult = sysConfigService.getOne(queryWrapper);
        return Result.success(sysConfigResult);
    }

    /**
     * 通过键设置值
     *
     * @param sysConfig
     * @return
     */
    @PostMapping("setConfigKeyValue")
    public Result setConfigKeyValue(@RequestBody SysConfig sysConfig) {
        QueryWrapper<SysConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", sysConfig.getConfigKey());
        SysConfig sysConfigResult = sysConfigService.getOne(queryWrapper);
        if (sysConfigResult == null) {
            return Result.error("参数不存在");
        }
        sysConfigResult.setConfigValue(sysConfig.getConfigValue());
        sysConfigService.updateById(sysConfigResult);
        return Result.success("修改成功");
    }
}
