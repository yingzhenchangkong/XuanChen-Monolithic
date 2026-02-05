package com.xuanchen.system.sysuser.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.fastexcel.FastExcelListener;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.SaltUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysuser.entity.SysUser;
import com.xuanchen.system.sysuser.service.ISysUserService;
import com.xuanchen.system.sysuserdept.entity.SysUserDept;
import com.xuanchen.system.sysuserdept.service.ISysUserDeptService;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;
import com.xuanchen.system.sysuserpost.service.ISysUserPostService;
import com.xuanchen.system.sysuserrole.entity.SysUserRole;
import com.xuanchen.system.sysuserrole.service.ISysUserRoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->用户
 *
 * @author XuanChen
 * @date 2025-03-31
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private ISysUserRoleService sysUserRoleService;
    @Autowired
    private ISysUserDeptService sysUserDeptService;
    @Autowired
    private ISysUserPostService sysUserPostService;
    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    /**
     * 分页列表查询
     *
     * @param sysUser
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/list")
    public Result list(SysUser sysUser,
                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                       HttpServletRequest req) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(sysUser.getUserName())) {
            queryWrapper.like("user_name", sysUser.getUserName());
        }
        if (StringUtil.isNotEmpty(sysUser.getNickName())) {
            queryWrapper.like("nick_name", sysUser.getNickName());
        }
        if (StringUtil.isNotEmpty(sysUser.getMobile())) {
            queryWrapper.like("mobile", sysUser.getMobile());
        }
        Page<SysUser> page = new Page<>(pageNo, pageSize);
        IPage<SysUser> pageList = sysUserService.page(page, queryWrapper);
        //加入角色、部门、岗位数据 开始
        List<SysUser> listSysUser = pageList.getRecords();
        for (int i = 0; i < listSysUser.size(); i++) {
            SysUser item = listSysUser.get(i);
            //角色
            QueryWrapper<SysUserRole> qwUserRole = new QueryWrapper<>();
            qwUserRole.eq("user_id", item.getId());
            List<SysUserRole> listUserRole = sysUserRoleService.list(qwUserRole);
            if (listUserRole.size() > 0) {
                String[] userRoleArr = new String[listUserRole.size()];
                for (int j = 0; j < listUserRole.size(); j++) {
                    userRoleArr[j] = listUserRole.get(j).getRoleId();
                }
                item.setRoleIds(userRoleArr);
                listSysUser.set(i, item);
            }
            //部门
            QueryWrapper<SysUserDept> qwUserDept = new QueryWrapper<>();
            qwUserDept.eq("user_id", item.getId());
            List<SysUserDept> listUserDept = sysUserDeptService.list(qwUserDept);
            if (listUserDept.size() > 0) {
                String[] userDeptArr = new String[listUserDept.size()];
                for (int j = 0; j < listUserDept.size(); j++) {
                    userDeptArr[j] = listUserDept.get(j).getDeptId();
                }
                item.setDeptIds(userDeptArr);
                listSysUser.set(i, item);
            }
            //岗位
            QueryWrapper<SysUserPost> qwUserPost = new QueryWrapper<>();
            qwUserPost.eq("user_id", item.getId());
            List<SysUserPost> listUserPost = sysUserPostService.list(qwUserPost);
            if (listUserPost.size() > 0) {
                String[] userPostArr = new String[listUserPost.size()];
                for (int j = 0; j < listUserPost.size(); j++) {
                    userPostArr[j] = listUserPost.get(j).getPostId();
                }
                item.setPostIds(userPostArr);
                listSysUser.set(i, item);
            }
        }
        pageList.setRecords(listSysUser);
        //加入角色、部门、岗位数据 结束
        return Result.success(pageList);
    }

    /**
     * 添加
     *
     * @param sysUser
     * @return
     */
    @PostMapping(value = "/add")
    public Result add(@RequestBody SysUser sysUser) {
        //生成盐
        String salt = SaltUtil.getSalt(8);
        sysUser.setSalt(salt);
        //密码加密
        String password = sysUser.getPassword();
        if (StringUtil.isEmpty(password)) {
            password = "XuanChen@888888";
        }
        sysUser.setPassword(authServiceCommon.encryptPassword(password, salt));
        //保存密码
        sysUserService.save(sysUser);
        // 添加 用户、角色关系
        sysUserRoleService.add(sysUser.getId(), sysUser.getRoleIds());
        // 添加 用户、部门关系
        sysUserDeptService.add(sysUser.getId(), sysUser.getDeptIds());
        // 添加 用户、岗位关系
        sysUserPostService.add(sysUser.getId(), sysUser.getPostIds());

        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysUser sysUser) {
        sysUserService.updateById(sysUser);
        // 根据用户编码 删除 用户、角色关系
        QueryWrapper<SysUserRole> qwUserRole = new QueryWrapper<>();
        qwUserRole.eq("user_id", sysUser.getId());
        sysUserRoleService.remove(qwUserRole);
        // 添加 用户、角色关系
        sysUserRoleService.add(sysUser.getId(), sysUser.getRoleIds());
        // 根据用户编码 删除 用户、部门关系
        QueryWrapper<SysUserDept> qwUserDept = new QueryWrapper<>();
        qwUserDept.eq("user_id", sysUser.getId());
        sysUserDeptService.remove(qwUserDept);
        // 添加 用户、部门关系
        sysUserDeptService.add(sysUser.getId(), sysUser.getDeptIds());

        // 根据用户编码 删除 用户、岗位关系
        QueryWrapper<SysUserPost> qwUserPost = new QueryWrapper<>();
        qwUserPost.eq("user_id", sysUser.getId());
        sysUserPostService.remove(qwUserPost);
        // 添加 用户、岗位关系
        sysUserPostService.add(sysUser.getId(), sysUser.getPostIds());
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 管理员 重置密码
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/resetPassword", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result resetPassword(@RequestBody SysUser sysUser) {
        String salt = SaltUtil.getSalt(8);
        String password = authServiceCommon.encryptPassword(sysUser.getPassword(), salt);
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("salt", salt)
                .set("password", password)
                .eq("id", sysUser.getId());
        sysUserService.update(updateWrapper);
        return Result.success("密码重置成功！");
    }

    /**
     * 用户 修改密码
     *
     * @param request
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/changePassword", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result changePassword(HttpServletRequest request) throws IOException {
        String paramStr = request.getReader().readLine();
        JSONObject paramJsonObject = JSON.parseObject(paramStr);
        String token = paramJsonObject.getString("token");
        String username = authServiceCommon.getUserNameByToken(token);
        String passwordTemp = paramJsonObject.getString("password");
        String salt = SaltUtil.getSalt(8);
        String password = authServiceCommon.encryptPassword(passwordTemp, salt);
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("password", password)
                .set("salt", salt)
                .eq("user_name", username);
        sysUserService.update(updateWrapper);
        return Result.success("修改成功！");
    }

    /**
     * 用户中心 更新信息
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/userCenterEdit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result userCenterEdit(@RequestBody SysUser sysUser) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("nick_name", sysUser.getNickName())
                .set("mobile", sysUser.getMobile())
                .set("email", sysUser.getEmail())
                .eq("user_name", sysUser.getUserName());
        sysUserService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 用户中心 更新头像
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/userCenterUpdateAvatar", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result userCenterUpdateAvatar(@RequestBody SysUser sysUser) {
        String userName = sysUser.getUserName();
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        SysUser sysUserDB = sysUserService.getOne(queryWrapper);
        File file = new File(uploadPath + File.separator + sysUserDB.getAvatar() + File.separator);
        if (file.exists()) {
            file.delete();
        }
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("avatar", sysUser.getAvatar())
                .eq("user_name", userName);
        sysUserService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result delete(@RequestParam(name = "id", required = true) String id) {
        sysUserService.removeById(id);
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
        sysUserService.removeByIds(Arrays.asList(ids.split(",")));
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
        //TODO:导入时需实现对已经选择的数据导出、对进行搜索后的结果进行导出
        List<SysUser> list = sysUserService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "用户管理";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FastExcel.write(response.getOutputStream())
                .head(SysUser.class)
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
            FastExcelListener<SysUser> listenter = new FastExcelListener<>();
            FastExcel.read(inputStream, SysUser.class, listenter)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<SysUser> dataList = listenter.getDataList();
            List<SysUser> listSysUser = new ArrayList<>();
            for (SysUser sysUser : dataList) {
                SysUser sysUser1 = sysUserService.getOne(new QueryWrapper<SysUser>().eq("user_name", sysUser.getUserName()));
                if (sysUser1 != null) {
                    continue;
                }
                SysUser sysUser2 = sysUserService.getOne(new QueryWrapper<SysUser>().eq("mobile", sysUser.getMobile()));
                if (sysUser2 != null) {
                    continue;
                }
                SysUser sysUser3 = sysUserService.getOne(new QueryWrapper<SysUser>().eq("email", sysUser.getEmail()));
                if (sysUser3 != null) {
                    continue;
                }
                String salt = SaltUtil.getSalt(8);
                String encodePassword = authServiceCommon.encryptPassword("XuanChen@888888", salt);
                sysUser.setPassword(encodePassword);
                listSysUser.add(sysUser);
            }
            sysUserService.saveBatch(listSysUser);
            Integer totcal = dataList.size();
            Integer success = listSysUser.size();
            Integer fail = totcal - success;
            message = "共" + totcal + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param sysUser
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listRecycleBin")
    public Result listRecycleBin(SysUser sysUser,
                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                 HttpServletRequest req) {
        Page<SysUser> page = new Page<>(pageNo, pageSize);
        IPage<SysUser> pageList = sysUserService.listRecycleBin(page, sysUser);
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
        sysUserService.deleteRecycleBin(id);
        return Result.success("彻底删除成功！");
    }

    /**
     * 回收站 彻底删除 多条
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/deleteRecycleBinBatch")
    public Result deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        sysUserService.deleteRecycleBin(ids);
        return Result.success("彻底删除成功！");
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
        sysUserService.revertRecycleBin(id);
        return Result.success("还原成功！");
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
        sysUserService.revertRecycleBin(ids);
        return Result.success("还原成功！");
    }

    /**
     * 下拉框
     *
     * @param roleNames
     * @return
     */
    @GetMapping("/select")
    public Result select(@RequestParam(name = "roleNames", required = false) String roleNames) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(roleNames)) {
            queryWrapper.like("role_names", roleNames);
        }
        queryWrapper.eq("status", true).orderByDesc("create_time");
        List<SysUser> list = sysUserService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 状态修改
     *
     * @param sysUser
     * @param request
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result changeStatus(@RequestBody SysUser sysUser, HttpServletRequest request) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysUser.getStatus()).eq("id", sysUser.getId());
        sysUserService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 参数 是否已存在
     *
     * @param sysUser
     * @return
     */
    @GetMapping("/validate")
    public Result validate(SysUser sysUser) {
        boolean exists = StringUtil.isEmpty(sysUser.getId())
                ? sysUserService.ifExistsNoId(sysUser)
                : sysUserService.ifExistsId(sysUser);
        return exists
                ? Result.error()
                : Result.success();
    }
}
