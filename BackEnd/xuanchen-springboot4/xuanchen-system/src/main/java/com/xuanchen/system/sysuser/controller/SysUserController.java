package com.xuanchen.system.sysuser.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetListener;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.FileSecurityUtil;
import com.xuanchen.common.utils.DeviceTypeUtil;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.common.utils.PasswordUtil;
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
import lombok.RequiredArgsConstructor;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 控制器-->用户
 *
 * @author XuanChen
 * @date 2025-03-31
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {
    /**
     * 单页最大条数（与 MyBatisPlusConfig 中分页插件 maxLimit 保持一致，控制器侧提前归一化，双重保护）
     */
    private static final int MAX_PAGE_SIZE = 100;
    /**
     * 批量操作（批量删除等）允许的最大 id 数量
     */
    private static final int MAX_BATCH_IDS = 100;

    /**
     * 邮箱格式校验（用户中心更新资料使用，规则与注册/找回密码链路一致）
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(AuthConst.EMAIL_REGEX);
    /**
     * 主键格式：雪花算法数字 id（当前体系为 19 位数字，放宽到 32 位数字以兼容）
     */
    private static final java.util.regex.Pattern ID_PATTERN = java.util.regex.Pattern.compile("^\\d{1,32}$");

    private final ISysUserService sysUserService;
    private final IAuthServiceCommon authServiceCommon;
    private final ISysUserRoleService sysUserRoleService;
    private final ISysUserDeptService sysUserDeptService;
    private final ISysUserPostService sysUserPostService;
    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    /**
     * 分页参数归一化：pageNo 最小为 1；pageSize 限制在 1..100，防止超大分页拖库
     */
    private int normalizePageNo(Integer pageNo) {
        return (pageNo == null || pageNo < 1) ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 解析并校验逗号分隔的 id 串：
     * 拒绝空串/空段/空白/非数字 id/超量批次，避免非法值直接 split 拼入 IN 条件或造成超大批操作。
     *
     * @return 校验失败返回 null（调用方返回 badRequest）；成功返回去空白后的 id 列表
     */
    private List<String> parseIds(String ids) {
        if (StringUtil.isEmpty(ids)) {
            return null;
        }
        String[] parts = ids.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String id = part == null ? "" : part.trim();
            if (id.isEmpty() || !ID_PATTERN.matcher(id).matches()) {
                return null;
            }
            result.add(id);
        }
        if (result.isEmpty() || result.size() > MAX_BATCH_IDS) {
            return null;
        }
        return result;
    }

    /**
     * 分页列表查询
     *
     * @param sysUser
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<SysUser>> list(SysUser sysUser,
                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
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
        Page<SysUser> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        IPage<SysUser> pageList = sysUserService.page(page, queryWrapper);
        //加入角色、部门、岗位数据（批量查询，避免每个用户 3 次 N+1 查询）
        fillUserRelations(pageList.getRecords());
        return Result.success(pageList);
    }

    /**
     * 批量填充用户的角色/部门/岗位 ID 数组。
     * 无论分页多少用户，关系表固定只查 3 次（IN 批量），替代原来每用户 3 次共 3N 次查询
     */
    private void fillUserRelations(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> userIds = new ArrayList<>(users.size());
        for (SysUser user : users) {
            if (StringUtil.isNotEmpty(user.getId())) {
                userIds.add(user.getId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        //按 userId 分组三种关系
        Map<String, List<String>> roleMap = new java.util.HashMap<>();
        Map<String, List<String>> deptMap = new java.util.HashMap<>();
        Map<String, List<String>> postMap = new java.util.HashMap<>();
        List<SysUserRole> userRoles = sysUserRoleService.list(new QueryWrapper<SysUserRole>().in("user_id", userIds));
        for (SysUserRole rel : userRoles) {
            roleMap.computeIfAbsent(rel.getUserId(), k -> new ArrayList<>()).add(rel.getRoleId());
        }
        List<SysUserDept> userDepts = sysUserDeptService.list(new QueryWrapper<SysUserDept>().in("user_id", userIds));
        for (SysUserDept rel : userDepts) {
            deptMap.computeIfAbsent(rel.getUserId(), k -> new ArrayList<>()).add(rel.getDeptId());
        }
        List<SysUserPost> userPosts = sysUserPostService.list(new QueryWrapper<SysUserPost>().in("user_id", userIds));
        for (SysUserPost rel : userPosts) {
            postMap.computeIfAbsent(rel.getUserId(), k -> new ArrayList<>()).add(rel.getPostId());
        }
        for (SysUser user : users) {
            List<String> roleIds = roleMap.get(user.getId());
            if (roleIds != null && !roleIds.isEmpty()) {
                user.setRoleIds(roleIds.toArray(new String[0]));
            }
            List<String> deptIds = deptMap.get(user.getId());
            if (deptIds != null && !deptIds.isEmpty()) {
                user.setDeptIds(deptIds.toArray(new String[0]));
            }
            List<String> postIds = postMap.get(user.getId());
            if (postIds != null && !postIds.isEmpty()) {
                user.setPostIds(postIds.toArray(new String[0]));
            }
        }
    }

    /**
     * 查询指定列中已存在的值集合（单列 IN 批量查询，用于 Excel 导入前查重）。
     * 列名由服务端代码固定传入，不接受用户输入
     *
     * @param values Excel 中待校验的去重前值
     * @param column 数据库列名（user_name/mobile/email）
     * @return 库中已存在的值集合
     */
    private java.util.Set<String> loadExistingColumn(List<String> values, String column) {
        java.util.Set<String> result = new java.util.HashSet<>();
        if (values == null || values.isEmpty()) {
            return result;
        }
        List<String> distinct = new ArrayList<>(new java.util.HashSet<>(values));
        List<SysUser> existed = sysUserService.list(
                new QueryWrapper<SysUser>().select(column).in(column, distinct));
        for (SysUser user : existed) {
            switch (column) {
                case "user_name" -> {
                    if (StringUtil.isNotEmpty(user.getUserName())) {
                        result.add(user.getUserName());
                    }
                }
                case "mobile" -> {
                    if (StringUtil.isNotEmpty(user.getMobile())) {
                        result.add(user.getMobile());
                    }
                }
                case "email" -> {
                    if (StringUtil.isNotEmpty(user.getEmail())) {
                        result.add(user.getEmail());
                    }
                }
                default -> throw new IllegalArgumentException("不支持的查重列: " + column);
            }
        }
        return result;
    }

    /**
     * 添加
     *
     * @param sysUser
     * @return
     */
    @PostMapping(value = "/add")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody SysUser sysUser) {
        //头像路径必须是上传目录内的合法图片相对路径，防止写入恶意库存值
        if (StringUtil.isNotEmpty(sysUser.getAvatar())) {
            try {
                sysUser.setAvatar(FileSecurityUtil.validateImageRelativePath(sysUser.getAvatar()));
            } catch (IllegalArgumentException e) {
                return Result.badRequest("头像路径非法！");
            }
        }
        //密码处理：管理员显式提供密码时直接使用；未提供则为该用户生成一次性随机临时密码
        //（不再使用全系统统一的硬编码默认密码），并标记首次登录必须改密
        String rawPassword = sysUser.getPassword();
        boolean useTempPassword = StringUtil.isEmpty(rawPassword);
        if (useTempPassword) {
            rawPassword = PasswordUtil.generateTempPassword();
        } else if (!PasswordUtil.isValidPassword(rawPassword)) {
            //显式指定的口令必须满足复杂度策略，不能只靠前端校验
            return Result.badRequest(AuthConst.PASSWORD_POLICY_TIP);
        }
        sysUser.setPassword(authServiceCommon.encryptPassword(rawPassword));
        sysUser.setPwdResetRequired(useTempPassword ? CommonConst.YES : CommonConst.NO);
        //保存密码
        sysUserService.save(sysUser);
        // 添加 用户、角色关系
        sysUserRoleService.add(sysUser.getId(), sysUser.getRoleIds());
        // 添加 用户、部门关系
        sysUserDeptService.add(sysUser.getId(), sysUser.getDeptIds());
        // 添加 用户、岗位关系
        sysUserPostService.add(sysUser.getId(), sysUser.getPostIds());

        //临时密码仅此一次随响应返回给操作者转交用户，数据库只存哈希
        if (useTempPassword) {
            return Result.success(TipConst.ADD_SUCC + "，初始密码：" + rawPassword + "（首次登录需修改密码）");
        }
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody SysUser sysUser) {
        //主键必填：禁止空 id 更新，防止 where 条件缺失波及其他记录
        if (StringUtil.isEmpty(sysUser.getId())) {
            return Result.badRequest("缺少用户ID！");
        }
        //头像路径必须是上传目录内的合法图片相对路径，防止写入恶意库存值
        if (StringUtil.isNotEmpty(sysUser.getAvatar())) {
            try {
                sysUser.setAvatar(FileSecurityUtil.validateImageRelativePath(sysUser.getAvatar()));
            } catch (IllegalArgumentException e) {
                return Result.badRequest("头像路径非法！");
            }
        }
        //账号状态只允许 1正常/2冻结，拒绝客户端写入任意值
        if (sysUser.getStatus() != null
                && (sysUser.getStatus() < 1 || sysUser.getStatus() > 2)) {
            return Result.badRequest("账号状态非法！");
        }
        //目标用户必须存在（未删除），避免向不存在用户写入关系数据
        long exists = sysUserService.count(new QueryWrapper<SysUser>().eq("id", sysUser.getId()));
        if (exists == 0) {
            return Result.badRequest("用户不存在！");
        }
        //防批量赋值（Mass Assignment）：主表仅更新编辑表单允许的业务字段。
        //password/salt/delFlag/createBy/createTime/updateBy/updateTime 等服务端字段
        //一律不接受客户端赋值（密码走 resetPassword，删除/还原走回收站接口，状态虽允许改但值域已校验）。
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", sysUser.getId());
        boolean dirty = false;
        if (StringUtil.isNotEmpty(sysUser.getUserName())) {
            updateWrapper.set("user_name", sysUser.getUserName());
            dirty = true;
        }
        if (StringUtil.isNotEmpty(sysUser.getNickName())) {
            updateWrapper.set("nick_name", sysUser.getNickName());
            dirty = true;
        }
        if (StringUtil.isNotEmpty(sysUser.getMobile())) {
            updateWrapper.set("mobile", sysUser.getMobile());
            dirty = true;
        }
        if (sysUser.getEmail() != null) {
            updateWrapper.set("email", sysUser.getEmail());
            dirty = true;
        }
        if (sysUser.getAvatar() != null) {
            updateWrapper.set("avatar", sysUser.getAvatar());
            dirty = true;
        }
        if (sysUser.getStatus() != null) {
            updateWrapper.set("status", sysUser.getStatus());
            dirty = true;
        }
        if (dirty) {
            sysUserService.update(updateWrapper);
        }
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> resetPassword(@RequestBody SysUser sysUser) {
        if (StringUtil.isEmpty(sysUser.getId()) || StringUtil.isEmpty(sysUser.getPassword())) {
            return Result.badRequest("用户ID与新密码不能为空！");
        }
        if (!PasswordUtil.isValidPassword(sysUser.getPassword())) {
            return Result.badRequest(AuthConst.PASSWORD_POLICY_TIP);
        }
        String password = authServiceCommon.encryptPassword(sysUser.getPassword());
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        //管理员重置的口令对用户而言是临时口令，重置后必须本人修改一次
        updateWrapper.set("password", password)
                .set("pwd_reset_required", CommonConst.YES)
                .eq("id", sysUser.getId());
        sysUserService.update(updateWrapper);
        return Result.success("密码重置成功！");
    }

    /**
     * 用户 修改密码
     * 安全约束：
     * 1. 修改人只取 SecurityContext 中过滤器已认证的主体，禁止从请求体接收 token（防止伪造 claim 越权重置他人密码）；
     * 2. 必须校验原密码；
     * 3. 修改成功后删除该用户全部在线会话（所有终端/IP，旧密码 token 立即失效），
     *    并为"当前请求"重新签发一套新会话随响应返回，前端凭新 token 免输入自动进入系统
     *    （不能让前端再调 /login：开启验证码时会被验证码拦住，且旧密码已失效）。
     *
     * @param request         用于取客户端 IP 与终端类型（新会话签发）
     * @param paramJsonObject 请求体，只允许携带原密码 oldPassword 与新密码 password
     * @return data 为与 /login 一致的用户信息（含新 token、pwdResetRequired=false）
     */
    @RequestMapping(value = "/changePassword", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<Map<String, Object>> changePassword(HttpServletRequest request,
                                                      @RequestBody JSONObject paramJsonObject) {
        //1. 只信任经过 JwtAuthenticationFilter 认证后的安全上下文
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String username = authentication.getName();

        //2. 请求体只允许携带原密码与新密码，token 一律不读取、不信任。
        //   由消息转换器解析整个 JSON 请求体，禁止再用 request.getReader().readLine() 手工读：
        //   readLine 只能读到第一个换行符，前端若发送多行/带换行的 JSON 会被截断解析失败
        String oldPassword = paramJsonObject.getString("oldPassword");
        String newPassword = paramJsonObject.getString("password");
        if (StringUtil.isEmpty(oldPassword) || StringUtil.isEmpty(newPassword)) {
            return Result.badRequest("原密码和新密码不能为空！");
        }
        //后端强制口令复杂度：长度 6-20 且含字母、数字、特殊字符，不能只依赖前端表单校验
        if (!PasswordUtil.isValidPassword(newPassword)) {
            return Result.badRequest(AuthConst.PASSWORD_POLICY_TIP);
        }

        //3. 校验原密码，防止冒用会话改密
        if (!authServiceCommon.checkPassword(username, oldPassword)) {
            return Result.badRequest("原密码错误！");
        }

        //4. 仅更新当前登录用户自身的密码，同时清除"必须改密"标志
        String password = authServiceCommon.encryptPassword(newPassword);
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("password", password)
                .set("pwd_reset_required", CommonConst.NO)
                .eq("user_name", username);
        sysUserService.update(updateWrapper);

        //5. 踢掉该用户全部旧会话（所有终端/IP，包含当前会话），防止旧密码会话继续可用
        authServiceCommon.kickOutUser(username);

        //6. 为当前客户端签发新会话（与 /login 同一套逻辑），实现改密后自动登录
        String clientIp = IPUtil.getClientIpAddress(request);
        String deviceType = DeviceTypeUtil.detectDeviceType(request);
        Map<String, Object> loginInfo = authServiceCommon.issueLoginSession(username, clientIp, deviceType);
        if (loginInfo == null) {
            return Result.error("密码修改成功，但自动登录失败，请使用新密码手动登录！");
        }
        return Result.success("密码修改成功！", loginInfo);
    }

    /**
     * 用户中心 更新信息
     * 安全约束：更新对象只取 SecurityContext 中的当前登录用户，禁止用请求体 userName 越权改他人资料
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/userCenterEdit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> userCenterEdit(@RequestBody SysUser sysUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String userName = authentication.getName();

        //邮箱格式校验（允许置空；非空时必须合法且不超长）
        String email = sysUser.getEmail();
        if (StringUtil.isNotEmpty(email)) {
            email = email.trim();
            if (email.length() > AuthConst.EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                return Result.badRequest("邮箱格式不正确！");
            }
        } else {
            email = null;
        }

        //邮箱唯一性校验：排除本人，防止改个资料就把邮箱改成他人已注册的
        if (email != null) {
            SysUser currentUser = sysUserService.getOne(new QueryWrapper<SysUser>().eq("user_name", userName));
            if (currentUser == null) {
                return Result.badRequest("用户不存在！");
            }
            long emailUsed = sysUserService.count(new QueryWrapper<SysUser>()
                    .eq("email", email)
                    .ne("id", currentUser.getId()));
            if (emailUsed > 0) {
                return Result.conflict("该邮箱已被其他账号使用！");
            }
        }

        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("nick_name", sysUser.getNickName())
                .set("mobile", sysUser.getMobile())
                .set("email", email)
                .eq("user_name", userName);
        sysUserService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 用户中心 更新头像
     * 安全约束：
     * 1. 更新对象只取 SecurityContext 中的当前登录用户，禁止用请求体 userName 越权改他人头像；
     * 2. 新头像路径必须是上传接口产出的合法图片相对路径（拒绝绝对路径/“..”/非图片后缀），杜绝库存恶意值；
     * 3. 删除旧头像时同样做根目录 containment 校验且必须是普通文件，防止库存字段被构造成任意文件删除。
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/userCenterUpdateAvatar", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> userCenterUpdateAvatar(@RequestBody SysUser sysUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String userName = authentication.getName();
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        SysUser sysUserDB = sysUserService.getOne(queryWrapper);
        if (sysUserDB == null) {
            return Result.badRequest("用户不存在！");
        }

        //1、校验新头像路径合法性
        String newAvatar = sysUser.getAvatar();
        if (StringUtil.isEmpty(newAvatar)) {
            return Result.badRequest("头像路径不能为空！");
        }
        try {
            newAvatar = FileSecurityUtil.validateImageRelativePath(newAvatar);
        } catch (IllegalArgumentException e) {
            return Result.badRequest("头像路径非法！");
        }

        //2、安全删除旧头像（旧值同样必须被约束在上传根目录内，且必须是普通文件）
        String oldAvatar = sysUserDB.getAvatar();
        if (StringUtil.isNotEmpty(oldAvatar)) {
            try {
                Path oldFile = FileSecurityUtil.resolveUnderRoot(Paths.get(uploadPath), oldAvatar);
                if (Files.isRegularFile(oldFile)) {
                    Files.deleteIfExists(oldFile);
                }
            } catch (IllegalArgumentException | IOException ignored) {
                //旧文件路径非法或删除失败不阻断头像更新
            }
        }

        //3、写入校验后的新头像路径
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("avatar", newAvatar)
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        sysUserService.removeByIds(idList);
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 导出excel
     *
     * @param response
     * @throws IOException
     */
    @GetMapping(value = "/exportExcel")
    @PreAuthorize("hasRole('admin')")
    public void exportExcel(HttpServletResponse response) throws IOException {
        //TODO:导入时需实现对已经选择的数据导出、对进行搜索后的结果进行导出
        List<SysUser> list = sysUserService.list();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String title = "用户管理";
        String fileName = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        FesodSheet.write(response.getOutputStream())
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> importExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String message = "";
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile multipartFile = entity.getValue();
            InputStream inputStream = multipartFile.getInputStream();
            FesodSheetListener<SysUser> listener = new FesodSheetListener<>();
            FesodSheet.read(inputStream, SysUser.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<SysUser> dataList = listener.getDataList();
            List<SysUser> listSysUser = new ArrayList<>();
            //本批导入用户共用一个本次随机生成的临时密码（不再使用硬编码默认密码）
            String batchTempPassword = null;
            if (!dataList.isEmpty()) {
                // 批量预载库中已存在的用户名/手机号/邮箱（3 次 IN 查询，替代每行 3 次 getOne 的 N+1）
                List<String> userNames = new ArrayList<>();
                List<String> mobiles = new ArrayList<>();
                List<String> emails = new ArrayList<>();
                for (SysUser row : dataList) {
                    if (StringUtil.isNotEmpty(row.getUserName())) {
                        userNames.add(row.getUserName());
                    }
                    if (StringUtil.isNotEmpty(row.getMobile())) {
                        mobiles.add(row.getMobile());
                    }
                    if (StringUtil.isNotEmpty(row.getEmail())) {
                        emails.add(row.getEmail());
                    }
                }
                java.util.Set<String> existUserNames = loadExistingColumn(userNames, "user_name");
                java.util.Set<String> existMobiles = loadExistingColumn(mobiles, "mobile");
                java.util.Set<String> existEmails = loadExistingColumn(emails, "email");
                // 文件内自身去重：Excel 中重复出现的同一用户名/手机/邮箱也只允许首行入库
                java.util.Set<String> seenUserNames = new java.util.HashSet<>();
                java.util.Set<String> seenMobiles = new java.util.HashSet<>();
                java.util.Set<String> seenEmails = new java.util.HashSet<>();
                //本批导入用户共用一个本次随机生成的临时密码（不再使用硬编码默认密码）
                batchTempPassword = PasswordUtil.generateTempPassword();
                String encodePassword = authServiceCommon.encryptPassword(batchTempPassword);
                for (SysUser sysUser : dataList) {
                    String userName = sysUser.getUserName();
                    String mobile = sysUser.getMobile();
                    String email = sysUser.getEmail();
                    if (StringUtil.isNotEmpty(userName)
                            && (existUserNames.contains(userName) || !seenUserNames.add(userName))) {
                        continue;
                    }
                    if (StringUtil.isNotEmpty(mobile)
                            && (existMobiles.contains(mobile) || !seenMobiles.add(mobile))) {
                        continue;
                    }
                    if (StringUtil.isNotEmpty(email)
                            && (existEmails.contains(email) || !seenEmails.add(email))) {
                        continue;
                    }
                    sysUser.setPassword(encodePassword);
                    //导入用户使用本次随机生成的临时密码，首次登录必须修改
                    sysUser.setPwdResetRequired(CommonConst.YES);
                    listSysUser.add(sysUser);
                }
            }
            sysUserService.saveBatch(listSysUser);
            Integer total = dataList.size();
            Integer success = listSysUser.size();
            Integer fail = total - success;
            message = "共" + total + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
            if (success > 0) {
                //临时密码仅此一次随响应返回给操作者；同一批导入用户共用本次随机口令，首登后各自改密
                message += "；初始密码：" + batchTempPassword + "（首次登录需修改密码）";
            }
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     *
     * @param sysUser
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/listRecycleBin")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<SysUser>> listRecycleBin(SysUser sysUser,
                                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysUser> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteRecycleBin(@RequestParam("id") String id) {
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        sysUserService.deleteRecycleBin(String.join(",", idList));
        return Result.success("彻底删除成功！");
    }

    /**
     * 回收站 还原 单条
     *
     * @param map
     * @return
     */
    @PutMapping("/revertRecycleBin")
    @PreAuthorize("hasRole('admin')")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
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
    @PreAuthorize("hasRole('admin')")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        String ids = map.get("ids");
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        sysUserService.revertRecycleBin(String.join(",", idList));
        return Result.success("还原成功！");
    }

    /**
     * 下拉框
     *
     * @param roleNames
     * @return
     */
    @GetMapping("/select")
    @PreAuthorize("hasRole('admin')")
    public Result<List<SysUser>> select(@RequestParam(name = "roleNames", required = false) String roleNames) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(roleNames)) {
            queryWrapper.like("role_names", roleNames);
        }
        queryWrapper.eq("status", CommonConst.STATUS_ENABLED).eq("del_flag", CommonConst.DEL_FLAG_NORMAL).orderByDesc("create_time");
        List<SysUser> list = sysUserService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 状态修改
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('admin')")
    public Result<String> changeStatus(@RequestBody SysUser sysUser) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysUser.getStatus()).eq("id", sysUser.getId());
        sysUserService.update(updateWrapper);
        return Result.success("状态修改成功！");
    }

    /**
     * 校验 参数 是否已存在
     * 鉴权：用户管理为管理员功能，必须限制 admin 角色；
     * 否则任意登录用户（甚至未登录）可据此枚举用户名/手机号/邮箱是否已注册
     *
     * @param sysUser
     * @return
     */
    @GetMapping("/validate")
    @PreAuthorize("hasRole('admin')")
    public Result<String> validate(SysUser sysUser) {
        boolean exists = StringUtil.isEmpty(sysUser.getId())
                ? sysUserService.ifExistsNoId(sysUser)
                : sysUserService.ifExistsId(sysUser);
        return exists
                ? Result.conflict(TipConst.PARAM_EXISTS)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }
}
