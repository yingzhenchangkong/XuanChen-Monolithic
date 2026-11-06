package com.xuanchen.system.sysaccount.controller;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.CommonConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetListener;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.DeviceTypeUtil;
import com.xuanchen.common.utils.FileSecurityUtil;
import com.xuanchen.common.utils.IPUtil;
import com.xuanchen.common.utils.PasswordUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysaccount.entity.SysAccountView;
import com.xuanchen.system.sysaccount.entity.SysAccount;
import com.xuanchen.masterdata.mdperson.entity.MdPerson;
import com.xuanchen.masterdata.mdperson.mapper.MdPersonMapper;
import com.xuanchen.system.sysaccount.service.ISysAccountService;
import com.xuanchen.system.sysaccountrole.entity.SysAccountRole;
import com.xuanchen.system.sysaccountrole.service.ISysAccountRoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 控制器-->账号管理（只维护 sys_account：登录名/密码/状态/绑定人员）
 * <p>
 * 边界约定：
 * 1. 本控制器只读写 sys_account 与 sys_account_role，不写 md_person / md_person_dept / md_person_post；
 * 2. 人员档案的字段维护走 {@link com.xuanchen.masterdata.mdperson.controller.MdPersonController}（含用户中心资料/头像）；
 * 3. 列表中的"绑定人员姓名"仅由 SQL 只读带出，不参与新增/编辑写入。
 *
 * @author XuanChen
 * @date 2026-10-17
 */
@RestController
@RequestMapping("/system/account")
public class SysAccountController {
    @Autowired
    private ISysAccountService sysAccountService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private ISysAccountRoleService sysAccountRoleService;
    /**
     * 只读依赖：仅用于校验"待绑定的人员是否存在"，不做任何人员表写入
     */
    @Autowired
    private MdPersonMapper mdPersonMapper;

    /**
     * 口令复杂度提示，与前端 ChangePassword/ForceChangePassword 的校验规则保持一致
     */
    private static final String PASSWORD_POLICY_TIP = "密码必须为6-20位，且包含字母、数字和特殊字符！";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(AuthConst.EMAIL_REGEX);
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    /**
     * 分页每页最大条数，防止超大分页拖库
     */
    private static final int MAX_PAGE_SIZE = 100;
    /**
     * 批量操作（批量删除等）允许的最大 id 数量
     */
    private static final int MAX_BATCH_IDS = 100;

    /**
     * 主键格式：雪花算法数字 id（当前体系为 19 位数字，放宽到 32 位数字以兼容）
     */
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,32}$");

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
     * 分页列表查询（sys_account 字段 + 只读的绑定人员姓名）
     */
    @GetMapping("/list")
    @RequiresRoles("admin")
    public Result<IPage<SysAccountView>> list(SysAccountView sysAccountView,
                                              @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysAccountView> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        IPage<SysAccountView> pageList = sysAccountService.listWithPerson(page, sysAccountView);
        fillRoleRelations(pageList.getRecords());
        return Result.success(pageList);
    }

    /**
     * 回填角色（账号维度，整页一次 IN 查询）；部门/岗位属于人员档案，不在账号列表返回
     */
    private void fillRoleRelations(List<SysAccountView> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        List<String> accountIds = new ArrayList<>(accounts.size());
        for (SysAccountView account : accounts) {
            if (StringUtil.isNotEmpty(account.getId())) {
                accountIds.add(account.getId());
            }
        }
        if (accountIds.isEmpty()) {
            return;
        }
        Map<String, List<String>> roleMap = new HashMap<>();
        List<SysAccountRole> relations = sysAccountRoleService.list(
                new QueryWrapper<SysAccountRole>().in("account_id", accountIds));
        for (SysAccountRole rel : relations) {
            roleMap.computeIfAbsent(rel.getAccountId(), k -> new ArrayList<>()).add(rel.getRoleId());
        }
        for (SysAccountView account : accounts) {
            List<String> roleIds = roleMap.get(account.getId());
            if (roleIds != null && !roleIds.isEmpty()) {
                account.setRoleIds(roleIds.toArray(new String[0]));
            }
        }
    }

    /**
     * 账号侧唯一性校验：user_name / email / mobile
     * 逻辑删除（回收站内）的账号不占用，与 user_name 的既有口径保持一致；
     * 不走唯一索引是因为逻辑删除 + 回收站还原会长期占住邮箱/手机号。
     */
    private String checkUnique(String id, String userName, String email, String mobile) {
        if (StringUtil.isNotEmpty(userName)) {
            QueryWrapper<SysAccount> qw = new QueryWrapper<SysAccount>().eq("user_name", userName.trim());
            if (StringUtil.isNotEmpty(id)) {
                qw.ne("id", id);
            }
            if (sysAccountService.count(qw) > 0) {
                return "用户名已存在，请更换用户名！";
            }
        }
        if (StringUtil.isNotEmpty(email)) {
            QueryWrapper<SysAccount> qw = new QueryWrapper<SysAccount>().eq("email", email);
            if (StringUtil.isNotEmpty(id)) {
                qw.ne("id", id);
            }
            if (sysAccountService.count(qw) > 0) {
                return "该邮箱已被其他账号绑定，请更换邮箱！";
            }
        }
        if (StringUtil.isNotEmpty(mobile)) {
            QueryWrapper<SysAccount> qw = new QueryWrapper<SysAccount>().eq("mobile", mobile);
            if (StringUtil.isNotEmpty(id)) {
                qw.ne("id", id);
            }
            if (sysAccountService.count(qw) > 0) {
                return "该手机号已被其他账号使用！";
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 邮箱归一化：去空格 + 转小写（与注册接口口径一致）
     */
    private String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    /**
     * 邮箱/手机号格式校验：两者都是账号登录凭证，格式非法直接拒绝
     */
    private String validateContact(String email, String mobile) {
        if (email != null
                && (email.length() > AuthConst.EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches())) {
            return "邮箱格式不正确！";
        }
        if (mobile != null && !MOBILE_PATTERN.matcher(mobile).matches()) {
            return "手机号格式不正确！";
        }
        return null;
    }

    /**
     * 头像路径入库前校验（限定在配置的上传目录内，防止路径穿越）
     */
    private String normalizeAvatar(String avatar) {
        String trimmed = trimToNull(avatar);
        if (trimmed == null) {
            return null;
        }
        return FileSecurityUtil.validateImageRelativePath(trimmed);
    }

    /**
     * 添加账号（可选绑定已有人员；人员档案本身在此不创建、不修改）
     * 防批量赋值：主键由服务端生成，salt/password 由服务端重算，delFlag 等审计字段一律忽略客户端入参；
     * status 仅允许 1正常/2冻结。
     */
    @PostMapping(value = "/add")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody SysAccountView sysAccountView) {
        //账号状态只允许 1正常/2冻结，拒绝客户端写入任意值
        if (sysAccountView.getStatus() != null && (sysAccountView.getStatus() < 1 || sysAccountView.getStatus() > 2)) {
            return Result.badRequest("账号状态非法！");
        }
        //口令：未提供则生成一次性随机临时密码（不再使用全系统统一硬编码默认口令），
        //并置"首次登录必须改密"；显式提供则强制复杂度校验，不强制改密
        String rawPassword = sysAccountView.getPassword();
        boolean tempPassword = StringUtil.isEmpty(rawPassword);
        if (tempPassword) {
            rawPassword = PasswordUtil.generateTempPassword();
        } else if (!PasswordUtil.isValidPassword(rawPassword)) {
            return Result.badRequest(PASSWORD_POLICY_TIP);
        }
        //构建账号实体（可选绑定已有人员档案）
        MdPerson person = null;
        if (StringUtil.isNotEmpty(sysAccountView.getPersonId())) {
            person = mdPersonMapper.selectById(sysAccountView.getPersonId());
            if (person == null) {
                return Result.badRequest("绑定的人员不存在！");
            }
        }
        //身份与凭证（昵称/头像/手机号/邮箱）在账号侧校验并落库
        String email = normalizeEmail(sysAccountView.getEmail());
        String mobile = trimToNull(sysAccountView.getMobile());
        String contactError = validateContact(email, mobile);
        if (contactError != null) {
            return Result.badRequest(contactError);
        }
        String uniqueError = checkUnique(null, sysAccountView.getUserName(), email, mobile);
        if (uniqueError != null) {
            return Result.conflict(uniqueError);
        }
        String nickName = trimToNull(sysAccountView.getNickName());
        SysAccount account = new SysAccount();
        //主键由 MyBatis-Plus 雪花算法生成，忽略客户端传入的 id（防批量赋值）
        account.setId(null);
        account.setPersonId(person != null ? person.getId() : null);
        account.setUserName(sysAccountView.getUserName());
        // 展示名默认取用户名，避免列表/下拉出现空白
        account.setNickName(nickName == null ? sysAccountView.getUserName() : nickName);
        try {
            account.setAvatar(normalizeAvatar(sysAccountView.getAvatar()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest("头像路径非法！");
        }
        account.setMobile(mobile);
        account.setEmail(email);
        account.setPassword(authServiceCommon.encryptPassword(rawPassword));
        account.setPwdResetRequired(tempPassword ? CommonConst.YES : CommonConst.NO);
        account.setStatus(sysAccountView.getStatus());
        account.setDelFlag(0);
        //BCrypt 自带随机盐，sys_account.salt 对新账号不再使用（保留列仅为兼容历史 SHA-512 账号）
        account.setSalt(null);
        sysAccountService.save(account);
        // 角色按账号维度关联（部门/岗位属于人员档案，请在人员管理维护）
        sysAccountRoleService.add(account.getId(), sysAccountView.getRoleIds());

        //临时密码仅此一次随响应返回给操作者（HTTPS 通道），数据库只存 BCrypt 哈希
        return tempPassword
                ? Result.success(TipConst.ADD_SUCC + "，初始密码：" + rawPassword + "（首次登录需修改密码）")
                : Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     * 防批量赋值（Mass Assignment）：主表仅更新编辑表单允许的业务字段；
     * password/salt/delFlag/createBy/createTime/updateBy/updateTime 一律不接受客户端赋值
     * （密码走 resetPassword，删除/还原走回收站接口）。
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody SysAccountView sysAccountView) {
        //主键必填：禁止空 id 更新，防止 where 条件缺失波及其他记录
        if (StringUtil.isEmpty(sysAccountView.getId())) {
            return Result.badRequest("缺少账号ID！");
        }
        //账号状态只允许 1正常/2冻结，拒绝客户端写入任意值
        if (sysAccountView.getStatus() != null && (sysAccountView.getStatus() < 1 || sysAccountView.getStatus() > 2)) {
            return Result.badRequest("账号状态非法！");
        }
        //目标账号必须存在（未删除），避免向不存在账号写入关系数据
        long exists = sysAccountService.count(new QueryWrapper<SysAccount>().eq("id", sysAccountView.getId()));
        if (exists == 0) {
            return Result.badRequest("账号不存在！");
        }
        //绑定人员校验：人员存在才允许绑定/换绑
        if (StringUtil.isNotEmpty(sysAccountView.getPersonId())
                && mdPersonMapper.selectById(sysAccountView.getPersonId()) == null) {
            return Result.badRequest("绑定的人员不存在！");
        }
        //身份与凭证字段校验（唯一性 + 格式）
        String email = normalizeEmail(sysAccountView.getEmail());
        String mobile = trimToNull(sysAccountView.getMobile());
        String contactError = validateContact(email, mobile);
        if (contactError != null) {
            return Result.badRequest(contactError);
        }
        String uniqueError = checkUnique(sysAccountView.getId(), sysAccountView.getUserName(),
                StringUtil.isEmpty(sysAccountView.getEmail()) ? null : email,
                sysAccountView.getMobile() == null ? null : mobile);
        if (uniqueError != null) {
            return Result.conflict(uniqueError);
        }
        String avatar;
        try {
            avatar = normalizeAvatar(sysAccountView.getAvatar());
        } catch (IllegalArgumentException e) {
            return Result.badRequest("头像路径非法！");
        }
        //更新账号表字段（user_name, nick_name, avatar, mobile, email, status, person_id）
        UpdateWrapper<SysAccount> accountUpdate = new UpdateWrapper<>();
        accountUpdate.eq("id", sysAccountView.getId());
        boolean accountDirty = false;
        if (StringUtil.isNotEmpty(sysAccountView.getUserName())) {
            accountUpdate.set("user_name", sysAccountView.getUserName());
            accountDirty = true;
        }
        if (sysAccountView.getStatus() != null) {
            accountUpdate.set("status", sysAccountView.getStatus());
            accountDirty = true;
        }
        if (sysAccountView.getPersonId() != null) {
            accountUpdate.set("person_id", StringUtil.isEmpty(sysAccountView.getPersonId()) ? null : sysAccountView.getPersonId());
            accountDirty = true;
        }
        if (sysAccountView.getNickName() != null) {
            String nickName = trimToNull(sysAccountView.getNickName());
            // 昵称清空时回退用户名，保证系统内展示名始终有值
            if (nickName == null && StringUtil.isNotEmpty(sysAccountView.getUserName())) {
                nickName = sysAccountView.getUserName();
            }
            if (nickName != null) {
                accountUpdate.set("nick_name", nickName);
                accountDirty = true;
            }
        }
        if (sysAccountView.getAvatar() != null) {
            accountUpdate.set("avatar", avatar);
            accountDirty = true;
        }
        if (sysAccountView.getMobile() != null) {
            accountUpdate.set("mobile", mobile);
            accountDirty = true;
        }
        if (sysAccountView.getEmail() != null) {
            accountUpdate.set("email", email);
            accountDirty = true;
        }
        if (accountDirty) {
            sysAccountService.update(accountUpdate);
        }
        // 角色重建（账号维度）；部门/岗位关系属人员档案，不在账号管理内维护
        QueryWrapper<SysAccountRole> qwUserRole = new QueryWrapper<>();
        qwUserRole.eq("account_id", sysAccountView.getId());
        sysAccountRoleService.remove(qwUserRole);
        sysAccountRoleService.add(sysAccountView.getId(), sysAccountView.getRoleIds());
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 管理员 重置密码
     * 安全约束：方法级 @RequiresRoles("admin") 强制管理员角色；用户ID与新密码必填且须满足口令复杂度。
     * 管理员重置的口令对用户而言是临时口令，重置后置 pwd_reset_required=1，本人首次登录必须修改。
     */
    @RequestMapping(value = "/resetPassword", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> resetPassword(@RequestBody SysAccountView sysAccountView) {
        if (StringUtil.isEmpty(sysAccountView.getId()) || StringUtil.isEmpty(sysAccountView.getPassword())) {
            return Result.badRequest("用户ID与新密码不能为空！");
        }
        if (!PasswordUtil.isValidPassword(sysAccountView.getPassword())) {
            return Result.badRequest(PASSWORD_POLICY_TIP);
        }
        String password = authServiceCommon.encryptPassword(sysAccountView.getPassword());
        UpdateWrapper<SysAccount> updateWrapper = new UpdateWrapper<>();
        //BCrypt 盐内嵌哈希串；同时清空历史 SHA-512 盐列，避免残留旧盐被误用
        updateWrapper.set("password", password)
                .set("salt", null)
                .set("pwd_reset_required", CommonConst.YES)
                .eq("id", sysAccountView.getId());
        // 影响行数为 0（用户 id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!sysAccountService.update(updateWrapper)) {
            return Result.badRequest("用户不存在或已被删除！");
        }
        return Result.success("密码重置成功！");
    }

    /**
     * 用户 修改密码
     * 安全约束：
     * 1. 修改人只取请求头 XC-ACCESS-TOKEN 解析出的登录主体，禁止从请求体接收 token（伪造 token 即可改他人密码）；
     * 2. 必须校验原密码（BCrypt/历史 SHA-512 双栈，服务端常量时间比较），防止冒用会话改密；
     * 3. 由消息转换器解析整个 JSON 请求体，禁止 request.getReader().readLine() 手工读
     * （readLine 只读到第一个换行符，多行 JSON 会被截断）；
     * 4. 仅允许更新当前登录用户自身的密码，成功后清除"必须改密"标志；
     * 5. 改密后该账号"除当前会话外"的全部在线会话立即失效（访问令牌、刷新令牌、长连接一并处理），
     *    并随响应为当前客户端补发一套新会话，前端凭新令牌免输入自动进入系统。
     */
    @RequestMapping(value = "/changePassword", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<Map<String, Object>> changePassword(HttpServletRequest request, @RequestBody JsonNode paramJsonObject) {
        String token = request.getHeader("XC-ACCESS-TOKEN");
        String username = authServiceCommon.getUserNameByToken(token);
        if (StringUtil.isEmpty(username)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        String oldPassword = paramJsonObject.get("oldPassword").asText();
        String newPassword = paramJsonObject.get("password").asText();
        if (StringUtil.isEmpty(oldPassword) || StringUtil.isEmpty(newPassword)) {
            return Result.badRequest("原密码和新密码不能为空！");
        }
        if (!PasswordUtil.isValidPassword(newPassword)) {
            return Result.badRequest(PASSWORD_POLICY_TIP);
        }
        //校验原密码：BCrypt 新口令或历史 SHA-512+盐均可，内部常量时间比较
        if (!authServiceCommon.checkPassword(username, oldPassword)) {
            return Result.badRequest("原密码错误！");
        }
        //仅更新当前登录用户自身的口令，BCrypt 哈希内嵌盐，清空旧盐列并解除"必须改密"
        String password = authServiceCommon.encryptPassword(newPassword);
        UpdateWrapper<SysAccount> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("password", password)
                .set("salt", null)
                .set("pwd_reset_required", CommonConst.NO)
                .eq("user_name", username);
        sysAccountService.update(updateWrapper);
        //旧令牌以旧口令哈希签名，验签必然失败：清理旧会话并以新哈希补发新会话
        String clientIp = IPUtil.getClientIpAddress(request);
        String deviceType = DeviceTypeUtil.detectDeviceType(request);
        Map<String, Object> loginInfo = authServiceCommon.rotateSession(username, clientIp, deviceType, token);
        if (loginInfo == null) {
            return Result.success("修改成功！请使用新密码重新登录！");
        }
        return Result.success("修改成功！", loginInfo);
    }

    /**
     * 通过id删除（逻辑删除账号，不影响人员档案）
     */
    @DeleteMapping(value = "/delete")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        SysAccount target = sysAccountService.getById(id);
        if (target == null) {
            return Result.badRequest("账号不存在！");
        }
        // 解除与人员档案的绑定（只写账号自己的 person_id 列），人员档案保留在人员管理中
        UpdateWrapper<SysAccount> unbind = new UpdateWrapper<>();
        unbind.set("person_id", null).eq("id", id);
        sysAccountService.update(unbind);
        sysAccountService.removeById(id);
        authServiceCommon.kickOutAccount(target.getUserName(), "账号已被删除，请联系管理员");
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 通过id批量删除（逻辑删除账号，不影响人员档案）
     */
    @DeleteMapping(value = "/deleteBatch")
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        List<SysAccount> targets = sysAccountService.listByIds(idList);
        // 解除绑定
        UpdateWrapper<SysAccount> unbind = new UpdateWrapper<>();
        unbind.set("person_id", null).in("id", idList);
        sysAccountService.update(unbind);
        sysAccountService.removeByIds(idList);
        if (targets != null) {
            for (SysAccount target : targets) {
                authServiceCommon.kickOutAccount(target.getUserName(), "账号已被删除，请联系管理员");
            }
        }
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 用户中心 更新本人资料（账号维度：昵称 / 手机号 / 邮箱）
     * 安全约束：目标账号只由请求头 XC-ACCESS-TOKEN 解析出的登录主体决定，忽略请求体 id，杜绝越权改他人账号。
     */
    @RequestMapping(value = "/userCenterEdit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> userCenterEdit(HttpServletRequest request, @RequestBody SysAccount account) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader(AuthConst.XC_ACCESS_TOKEN));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        SysAccount current = sysAccountService.getOne(new QueryWrapper<SysAccount>().eq("user_name", userName));
        if (current == null) {
            return Result.badRequest("用户不存在！");
        }
        String email = normalizeEmail(account.getEmail());
        String mobile = trimToNull(account.getMobile());
        String contactError = validateContact(email, mobile);
        if (contactError != null) {
            return Result.badRequest(contactError);
        }
        String uniqueError = checkUnique(current.getId(), null, email, mobile);
        if (uniqueError != null) {
            return Result.conflict(uniqueError);
        }
        UpdateWrapper<SysAccount> updateWrapper = new UpdateWrapper<>();
        boolean dirty = false;
        if (account.getNickName() != null) {
            String nickName = trimToNull(account.getNickName());
            updateWrapper.set("nick_name", nickName == null ? current.getUserName() : nickName);
            dirty = true;
        }
        if (account.getMobile() != null) {
            updateWrapper.set("mobile", mobile);
            dirty = true;
        }
        if (account.getEmail() != null) {
            updateWrapper.set("email", email);
            dirty = true;
        }
        if (!dirty) {
            return Result.badRequest("没有需要更新的字段！");
        }
        updateWrapper.eq("id", current.getId());
        sysAccountService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 用户中心 更新本人头像（账号维度：sys_account.avatar）
     */
    @RequestMapping(value = "/userCenterUpdateAvatar", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> userCenterUpdateAvatar(HttpServletRequest request, @RequestBody SysAccount account) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader(AuthConst.XC_ACCESS_TOKEN));
        if (StringUtil.isEmpty(userName)) {
            return Result.unauthorized("未登录或登录已过期，请重新登录！");
        }
        SysAccount current = sysAccountService.getOne(new QueryWrapper<SysAccount>().eq("user_name", userName));
        if (current == null) {
            return Result.badRequest("用户不存在！");
        }
        String newAvatar;
        try {
            newAvatar = normalizeAvatar(account.getAvatar());
        } catch (IllegalArgumentException e) {
            return Result.badRequest("头像路径非法！");
        }
        if (newAvatar == null) {
            return Result.badRequest("头像路径不能为空！");
        }
        // 清理旧头像文件（路径必须落在配置的上传目录内，否则跳过）
        if (StringUtil.isNotEmpty(current.getAvatar())) {
            try {
                Path oldFile = FileSecurityUtil.resolveUnderRoot(Paths.get(uploadPath), current.getAvatar());
                if (Files.isRegularFile(oldFile)) {
                    Files.deleteIfExists(oldFile);
                }
            } catch (IllegalArgumentException | IOException ignored) {
            }
        }
        UpdateWrapper<SysAccount> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("avatar", newAvatar).eq("id", current.getId());
        sysAccountService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 导出excel（账号列 + 只读的绑定人员姓名）
     */
    @GetMapping(value = "/exportExcel")
    @RequiresRoles("admin")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<SysAccountView> allAccounts = sysAccountService.listAllWithPerson(new SysAccountView());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("账号管理", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        try (ExcelWriter writer = FesodSheet.write(response.getOutputStream(), SysAccountView.class)
                .excelType(ExcelTypeEnum.XLSX)
                .build()) {
            writer.write(allAccounts, FesodSheet.writerSheet(0, "账号管理").build());
        }
    }

    /**
     * 通过excel导入数据（只导入账号：用户名 + 状态；人员档案请走人员管理）
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    @RequiresRoles("admin")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> importExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        String message = "";
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile multipartFile = entity.getValue();
            InputStream inputStream = multipartFile.getInputStream();
            FesodSheetListener<SysAccountView> listener = new FesodSheetListener<>();
            FesodSheet.read(inputStream, SysAccountView.class, listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
            List<SysAccountView> dataList = listener.getDataList();
            List<SysAccount> accountList = new ArrayList<>();
            String batchTempPassword = null;
            if (!dataList.isEmpty()) {
                List<String> userNames = new ArrayList<>();
                for (SysAccountView row : dataList) {
                    if (StringUtil.isNotEmpty(row.getUserName())) {
                        userNames.add(row.getUserName());
                    }
                }
                Set<String> existUserNames = new HashSet<>();
                if (!userNames.isEmpty()) {
                    List<String> distinct = new ArrayList<>(new HashSet<>(userNames));
                    List<SysAccount> existed = sysAccountService.list(
                            new QueryWrapper<SysAccount>().select("user_name").in("user_name", distinct));
                    for (SysAccount account : existed) {
                        if (StringUtil.isNotEmpty(account.getUserName())) {
                            existUserNames.add(account.getUserName());
                        }
                    }
                }
                Set<String> seenUserNames = new HashSet<>();
                batchTempPassword = PasswordUtil.generateTempPassword();
                String encodePassword = authServiceCommon.encryptPassword(batchTempPassword);
                for (SysAccountView row : dataList) {
                    String userName = row.getUserName();
                    if (StringUtil.isNotEmpty(userName)
                            && (existUserNames.contains(userName) || !seenUserNames.add(userName))) {
                        continue;
                    }
                    SysAccount account = new SysAccount();
                    account.setUserName(userName);
                    account.setPassword(encodePassword);
                    account.setPwdResetRequired(CommonConst.YES);
                    account.setStatus(row.getStatus() != null ? row.getStatus() : CommonConst.STATUS_ENABLED);
                    account.setDelFlag(0);
                    accountList.add(account);
                }
            }
            if (!accountList.isEmpty()) {
                sysAccountService.saveBatch(accountList);
            }
            Integer total = dataList.size();
            Integer success = accountList.size();
            Integer fail = total - success;
            message = "共" + total + "条数据,成功：" + success + "条数据，失败：" + fail + "条数据";
            if (success > 0) {
                message += "；初始密码：" + batchTempPassword + "（首次登录需修改密码）";
            }
        }
        return Result.success(message);
    }

    /**
     * 回收站 列表
     */
    @GetMapping("/listRecycleBin")
    @RequiresRoles("admin")
    public Result<IPage<SysAccountView>> listRecycleBin(SysAccountView sysAccountView,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<SysAccountView> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        IPage<SysAccountView> pageList = sysAccountService.listRecycleBin(page, sysAccountView);
        return Result.success(pageList);
    }

    @DeleteMapping("/deleteRecycleBin")
    @RequiresRoles("admin")
    public Result<String> deleteRecycleBin(@RequestParam("id") String id) {
        sysAccountService.deleteRecycleBin(id);
        return Result.success("彻底删除成功！");
    }

    @DeleteMapping("/deleteRecycleBinBatch")
    @RequiresRoles("admin")
    public Result<String> deleteRecycleBinBatch(@RequestParam("ids") String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        sysAccountService.deleteRecycleBin(String.join(",", idList));
        return Result.success("彻底删除成功！");
    }

    @PutMapping("/revertRecycleBin")
    @RequiresRoles("admin")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        sysAccountService.revertRecycleBin(id);
        return Result.success("还原成功！");
    }

    @PutMapping("/revertRecycleBinBatch")
    @RequiresRoles("admin")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        String ids = map.get("ids");
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的有效数字ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        sysAccountService.revertRecycleBin(String.join(",", idList));
        return Result.success("还原成功！");
    }

    /**
     * 下拉框（账号：用户名 + 只读的绑定人员姓名）
     */
    @GetMapping("/select")
    @RequiresRoles("admin")
    public Result<List<SysAccountView>> select(@RequestParam(name = "roleNames", required = false) String roleNames) {
        SysAccountView query = new SysAccountView();
        query.setStatus(CommonConst.STATUS_ENABLED);
        query.setDelFlag(CommonConst.DEL_FLAG_NORMAL);
        List<SysAccountView> list = sysAccountService.listAllWithPerson(query);
        return Result.success(list);
    }

    /**
     * 状态修改
     */
    @RequestMapping(value = "/changeStatus", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> changeStatus(@RequestBody SysAccountView sysAccountView) {
        UpdateWrapper<SysAccount> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", sysAccountView.getStatus()).eq("id", sysAccountView.getId());
        sysAccountService.update(updateWrapper);
        if (!CommonConst.STATUS_ENABLED.equals(sysAccountView.getStatus()) && StringUtil.isNotEmpty(sysAccountView.getId())) {
            SysAccount target = sysAccountService.getById(sysAccountView.getId());
            if (target != null) {
                authServiceCommon.kickOutAccount(target.getUserName(), "账号已被停用，请重新登录");
            }
        }
        return Result.success("状态修改成功！");
    }

    /**
     * 校验参数是否已存在（账号侧：用户名 / 邮箱 / 手机号，三者都是账号唯一标识）
     */
    @GetMapping("/validate")
    @RequiresRoles("admin")
    public Result<String> validate(SysAccountView sysAccountView) {
        if (StringUtil.isEmpty(sysAccountView.getUserName())
                && StringUtil.isEmpty(sysAccountView.getEmail())
                && StringUtil.isEmpty(sysAccountView.getMobile())) {
            return Result.badRequest("缺少待校验字段！");
        }
        String existsError = checkUnique(sysAccountView.getId(),
                sysAccountView.getUserName(),
                normalizeEmail(sysAccountView.getEmail()),
                trimToNull(sysAccountView.getMobile()));
        return existsError != null
                ? Result.conflict(existsError)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }
}
