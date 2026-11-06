package com.xuanchen.masterdata.mdpost.controller;

import org.apache.fesod.sheet.FesodSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetListener;
import com.xuanchen.common.excel.FesodSheetExportUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.masterdata.mdpost.entity.MdPost;
import com.xuanchen.masterdata.mdpost.service.IMdPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
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
@RequiresRoles("admin")
@RequestMapping("/system/post")
public class MdPostController {
    @Autowired
    private IMdPostService mdPostService;

    /**
     * 分页列表查询
     *
     * @param mdPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result<IPage<MdPost>> list(MdPost mdPost,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<MdPost> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(mdPost.getPostName())) {
            queryWrapper.like("post_name", mdPost.getPostName());
        }
        if (StringUtil.isNotEmpty(mdPost.getPostDescription())) {
            queryWrapper.like("post_description", mdPost.getPostDescription());
        }
        queryWrapper.orderByAsc("order_no");
        Page<MdPost> page = new Page<>(pageNo, pageSize);
        IPage<MdPost> pageList = mdPostService.page(page, queryWrapper);
        return Result.success(pageList);
    }

    /**
     * 添加
     *
     * @param mdPost
     * @return
     */
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody MdPost mdPost) {
        mdPostService.save(mdPost);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param mdPost
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody MdPost mdPost) {
        if (StringUtil.isEmpty(mdPost.getId())) {
            return Result.badRequest("缺少岗位ID！");
        }
        // 影响行数为 0（id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!mdPostService.updateById(mdPost)) {
            return Result.badRequest("岗位不存在或已被删除！");
        }
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        mdPostService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 通过id批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        mdPostService.removeByIds(Arrays.asList(ids.split(",")));
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
        // 主键游标分批查询 + 分批写入，避免 list() 全量加载导致 OOM
        FesodSheetExportUtil.export(response, "岗位管理", MdPost.class, MdPost::getId, (lastId, batchSize) -> {
            QueryWrapper<MdPost> queryWrapper = new QueryWrapper<>();
            if (StringUtil.isNotEmpty(lastId)) {
                queryWrapper.gt("id", lastId);
            }
            queryWrapper.orderByAsc("id");
            return mdPostService.page(FesodSheetExportUtil.cursorPage(batchSize), queryWrapper).getRecords();
        });
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<String> importExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String message = "";
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile multipartFile = entity.getValue();
            InputStream inputStream = multipartFile.getInputStream();
            FesodSheetListener<MdPost> listener = new FesodSheetListener<>();
            FesodSheet.read(inputStream, MdPost.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<MdPost> dataList = listener.getDataList();
            List<MdPost> listMdPost = new ArrayList<>();
            for (MdPost mdPost : dataList) {
                MdPost mdPostTemp = mdPostService.getOne(new QueryWrapper<MdPost>().eq("post_code", mdPost.getPostCode()));
                if (mdPostTemp != null) {
                    continue;
                }
                listMdPost.add(mdPost);
            }
            mdPostService.saveBatch(listMdPost);
            Integer total = dataList.size();
            Integer success = listMdPost.size();
            Integer fail = total - success;
            message = "共" + total + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param mdPost
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listRecycleBin")
    public Result<IPage<MdPost>> listRecycleBin(MdPost mdPost,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<MdPost> page = new Page<>(pageNo, pageSize);
        IPage<MdPost> pageList = mdPostService.listRecycleBin(page, mdPost);
        return Result.success(pageList);
    }

    /**
     * 回收站 彻底删除 单条
     *
     * @param id
     * @return
     */
    @DeleteMapping("/deleteRecycleBin")
    public Result<String> deleteRecycleBin(@RequestParam("id") String id) {
        mdPostService.deleteRecycleBin(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 彻底删除 多条
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/deleteRecycleBinBatch")
    public Result<String> deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        mdPostService.deleteRecycleBin(ids);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 回收站 还原 单条
     *
     * @param map
     * @return
     */
    @PutMapping("/revertRecycleBin")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        mdPostService.revertRecycleBin(id);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 回收站 还原 多条
     *
     * @param map
     * @return
     */
    @PutMapping("/revertRecycleBinBatch")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        String ids = map.get("ids");
        mdPostService.revertRecycleBin(ids);
        return Result.success(TipConst.REVERT_SUCC);
    }

    /**
     * 下拉框
     *
     * @return
     */
    @GetMapping("/select")
    public Result<List<MdPost>> select() {
        QueryWrapper<MdPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", CommonConst.DEL_FLAG_NORMAL).eq("status", CommonConst.STATUS_ENABLED).orderByAsc("order_no");
        List<MdPost> list = mdPostService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 状态修改
     *
     * @param mdPost
     * @param request
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> changeStatus(@RequestBody MdPost mdPost, HttpServletRequest request) {
        if (StringUtil.isEmpty(mdPost.getId())) {
            return Result.badRequest("缺少岗位ID！");
        }
        UpdateWrapper<MdPost> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", mdPost.getStatus()).eq("id", mdPost.getId());
        // 影响行数为 0（id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!mdPostService.update(updateWrapper)) {
            return Result.badRequest("岗位不存在或已被删除！");
        }
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 参数 是否已存在
     *
     * @param mdPost
     * @return
     */
    @GetMapping("/validate")
    public Result<Void> validate(MdPost mdPost) {
        boolean exists = StringUtil.isEmpty(mdPost.getId())
                ? mdPostService.ifExistsNoId(mdPost)
                : mdPostService.ifExistsId(mdPost);
        return exists
                ? Result.error()
                : Result.success();
    }
}
