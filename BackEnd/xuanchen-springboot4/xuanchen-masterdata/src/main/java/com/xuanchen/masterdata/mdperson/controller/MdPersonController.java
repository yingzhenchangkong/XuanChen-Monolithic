package com.xuanchen.masterdata.mdperson.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.AuthConst;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.excel.FesodSheetListener;
import com.xuanchen.common.utils.FileSecurityUtil;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.masterdata.mddept.entity.MdDept;
import com.xuanchen.masterdata.mddept.service.IMdDeptService;
import com.xuanchen.masterdata.mdperson.mapper.MdDictItemMapper;
import com.xuanchen.masterdata.mdperson.entity.MdPerson;
import com.xuanchen.masterdata.mdperson.entity.MdPersonImport;
import com.xuanchen.masterdata.mdperson.mapper.MdPersonMapper;
import com.xuanchen.masterdata.mdpersondept.entity.MdPersonDept;
import com.xuanchen.masterdata.mdpersondept.service.IMdPersonDeptService;
import com.xuanchen.masterdata.mdpersonpost.entity.MdPersonPost;
import com.xuanchen.masterdata.mdpersonpost.service.IMdPersonPostService;
import com.xuanchen.masterdata.mdpost.entity.MdPost;
import com.xuanchen.masterdata.mdpost.service.IMdPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 控制器-->人员管理（员工档案：姓名/证件/学历/岗位状态等）
 * 人员独立于账号存在：一个人员可对应 1~N 个账号，账号也可不绑定人员（见 SysAccountController）
 * <p>
 * 边界约定：本控制器是 md_person / md_person_dept / md_person_post 的唯一维护入口；
 * 对 sys_account 只在"人员删除时解绑"和"用户中心按登录名定位本人"两处做必要的读写。
 *
 * @author XuanChen
 * @date 2026-10-17
 */
@RestController
@RequestMapping("/system/person")
@RequiredArgsConstructor
public class MdPersonController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_IDS = 100;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_IMPORT_ERROR_DETAIL = 5;
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(AuthConst.EMAIL_REGEX);

    private final MdPersonMapper mdPersonMapper;
    private final IMdPersonDeptService mdPersonDeptService;
    private final IMdPersonPostService mdPersonPostService;
    /**
     * 仅用于导入：把 Excel 里的字典文案（如「在职」）换算成编码值
     */
    private final MdDictItemMapper mdDictItemMapper;
    /**
     * 仅用于列表回填部门/岗位名称（只读，不做维护）
     */
    private final IMdDeptService mdDeptService;
    private final IMdPostService mdPostService;

    @Value("${xuanchen.path.upload}")
    private String uploadPath;

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
     * 校验并规范化 32 位主键参数
     */
    private List<String> parseIds(String ids) {
        if (StringUtil.isEmpty(ids)) {
            return null;
        }
        String[] parts = ids.split(",");
        List<String> result = new java.util.ArrayList<>(parts.length);
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
     * 枚举字段合法性校验
     */
    private String validateEnums(MdPerson profile) {
        if (profile.getGender() != null && (profile.getGender() < 1 || profile.getGender() > 2)) {
            return "性别取值非法！";
        }
        if (profile.getMaritalStatus() != null && (profile.getMaritalStatus() < 1 || profile.getMaritalStatus() > 4)) {
            return "婚姻状况取值非法！";
        }
        if (profile.getEducation() != null && (profile.getEducation() < 1 || profile.getEducation() > 6)) {
            return "学历取值非法！";
        }
        if (profile.getPersonType() != null && (profile.getPersonType() < 1 || profile.getPersonType() > 3)) {
            return "人员类型取值非法！";
        }
        if (profile.getStatus() != null && (profile.getStatus() < 1 || profile.getStatus() > 4)) {
            return "人员状态取值非法！";
        }
        if (StringUtil.isNotEmpty(profile.getMobile()) && !MOBILE_PATTERN.matcher(profile.getMobile()).matches()) {
            return "手机号格式不正确！";
        }
        if (StringUtil.isNotEmpty(profile.getIdCard()) && !ID_CARD_PATTERN.matcher(profile.getIdCard()).matches()) {
            return "身份证号格式不正确！";
        }
        return null;
    }

    /**
     * 分页列表查询（人员档案）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<MdPerson>> list(MdPerson profile,
                                              @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<MdPerson> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        QueryWrapper<MdPerson> qw = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(profile.getRealName())) {
            qw.like("real_name", profile.getRealName());
        }
        if (StringUtil.isNotEmpty(profile.getMobile())) {
            qw.like("mobile", profile.getMobile());
        }
        if (StringUtil.isNotEmpty(profile.getPersonCode())) {
            qw.like("person_code", profile.getPersonCode());
        }
        if (profile.getPersonType() != null) {
            qw.eq("person_type", profile.getPersonType());
        }
        if (profile.getStatus() != null) {
            qw.eq("status", profile.getStatus());
        }
        qw.orderByDesc("create_time");
        IPage<MdPerson> pageList = mdPersonMapper.selectPage(page, qw);
        fillRelations(pageList.getRecords());
        return Result.success(pageList);
    }

    /**
     * 回填人员的部门/岗位（整页各一次 IN 查询，避免 N+1）
     */
    private void fillRelations(List<MdPerson> persons) {
        if (persons == null || persons.isEmpty()) {
            return;
        }
        List<String> personIds = new ArrayList<>(persons.size());
        for (MdPerson person : persons) {
            if (StringUtil.isNotEmpty(person.getId())) {
                personIds.add(person.getId());
            }
        }
        if (personIds.isEmpty()) {
            return;
        }
        Map<String, List<String>> deptMap = new HashMap<>();
        Map<String, List<String>> postMap = new HashMap<>();
        List<MdPersonDept> deptRelations = mdPersonDeptService.list(
                new QueryWrapper<MdPersonDept>().in("person_id", personIds));
        for (MdPersonDept rel : deptRelations) {
            deptMap.computeIfAbsent(rel.getPersonId(), k -> new ArrayList<>()).add(rel.getDeptId());
        }
        List<MdPersonPost> postRelations = mdPersonPostService.list(
                new QueryWrapper<MdPersonPost>().in("person_id", personIds));
        for (MdPersonPost rel : postRelations) {
            postMap.computeIfAbsent(rel.getPersonId(), k -> new ArrayList<>()).add(rel.getPostId());
        }
        Set<String> allDeptCodes = new HashSet<>();
        Set<String> allPostIds = new HashSet<>();
        for (MdPerson person : persons) {
            List<String> deptIds = deptMap.get(person.getId());
            if (deptIds != null && !deptIds.isEmpty()) {
                person.setDeptIds(deptIds.toArray(new String[0]));
                allDeptCodes.addAll(deptIds);
            }
            List<String> postIds = postMap.get(person.getId());
            if (postIds != null && !postIds.isEmpty()) {
                person.setPostIds(postIds.toArray(new String[0]));
                allPostIds.addAll(postIds);
            }
        }
        // 关系表里部门存 dept_code、岗位存 post_id；整页各一次查询换名称，供列表/详情直接展示
        Map<String, String> deptNameMap = new HashMap<>();
        Map<String, String> postNameMap = new HashMap<>();
        if (!allDeptCodes.isEmpty()) {
            List<MdDept> depts = mdDeptService.list(new QueryWrapper<MdDept>().in("dept_code", allDeptCodes));
            for (MdDept dept : depts) {
                deptNameMap.put(dept.getDeptCode(), dept.getDeptName());
            }
        }
        if (!allPostIds.isEmpty()) {
            List<MdPost> posts = mdPostService.list(new QueryWrapper<MdPost>().in("id", allPostIds));
            for (MdPost post : posts) {
                postNameMap.put(post.getId(), post.getPostName());
            }
        }
        for (MdPerson person : persons) {
            person.setDeptNames(joinNameText(person.getDeptIds(), deptNameMap));
            person.setPostNames(joinNameText(person.getPostIds(), postNameMap));
        }
    }

    /**
     * 把ID数组按名称映射拼成「甲、乙」形式的展示文本；映射缺失时回退为ID
     */
    private String joinNameText(String[] ids, Map<String, String> nameMap) {
        if (ids == null || ids.length == 0) {
            return null;
        }
        List<String> names = new ArrayList<>(ids.length);
        for (String id : ids) {
            String name = nameMap.get(id);
            names.add(StringUtil.isNotEmpty(name) ? name : id);
        }
        return String.join("、", names);
    }

    /**
     * 为人员分配部门/岗位（重建关系：先清空该人员的部门/岗位关系，再按提交的 ID 写入）
     * 关系表 md_person_dept / md_person_post 以 person_id 为准，属人员管理的维护范围
     */
    @PostMapping(value = "/assignDeptPost")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> assignDeptPost(@RequestBody MdPerson person) {
        if (StringUtil.isEmpty(person.getId())) {
            return Result.badRequest("缺少人员ID！");
        }
        if (mdPersonMapper.selectById(person.getId()) == null) {
            return Result.badRequest("人员不存在！");
        }
        mdPersonDeptService.remove(new QueryWrapper<MdPersonDept>().eq("person_id", person.getId()));
        mdPersonPostService.remove(new QueryWrapper<MdPersonPost>().eq("person_id", person.getId()));
        mdPersonDeptService.add(person.getId(), person.getDeptIds());
        mdPersonPostService.add(person.getId(), person.getPostIds());
        return Result.success("分配成功！");
    }

    /**
     * 保存人员的部门/岗位关系（整组覆盖）
     * 入参为 null 表示本次不涉及该维度；空数组表示清空
     */
    private void saveDeptPost(String personId, String[] deptIds, String[] postIds) {
        if (StringUtil.isEmpty(personId) || (deptIds == null && postIds == null)) {
            return;
        }
        if (deptIds != null) {
            mdPersonDeptService.remove(new QueryWrapper<MdPersonDept>().eq("person_id", personId));
            if (deptIds.length > 0) {
                mdPersonDeptService.add(personId, deptIds);
            }
        }
        if (postIds != null) {
            mdPersonPostService.remove(new QueryWrapper<MdPersonPost>().eq("person_id", personId));
            if (postIds.length > 0) {
                mdPersonPostService.add(personId, postIds);
            }
        }
    }

    /**
     * 人员照片路径入库前校验：只接受上传目录内的图片相对路径（如 photo/xxx.png），
     * 拒绝绝对路径、盘符与“..”穿越；空白视为未上传（置空）
     */
    private String normalizePhoto(String photo) {
        if (photo == null) {
            return null;
        }
        String trimmed = photo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return FileSecurityUtil.validateImageRelativePath(trimmed);
    }

    /**
     * 添加人员档案
     */
    @PostMapping(value = "/add")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody MdPerson profile) {
        String enumError = validateEnums(profile);
        if (enumError != null) {
            return Result.badRequest(enumError);
        }
        // 唯一性校验：身份证/手机号/邮箱
        if (StringUtil.isNotEmpty(profile.getIdCard())
                && mdPersonMapper.selectCount(new QueryWrapper<MdPerson>().eq("id_card", profile.getIdCard())) > 0) {
            return Result.conflict("该身份证号已存在！");
        }
        if (StringUtil.isNotEmpty(profile.getMobile())
                && mdPersonMapper.selectCount(new QueryWrapper<MdPerson>().eq("mobile", profile.getMobile())) > 0) {
            return Result.conflict("该手机号已被其他人员使用！");
        }
        if (StringUtil.isNotEmpty(profile.getEmail())
                && mdPersonMapper.selectCount(new QueryWrapper<MdPerson>().eq("email", profile.getEmail())) > 0) {
            return Result.conflict("该邮箱已被其他人员使用！");
        }
        try {
            profile.setPhoto(normalizePhoto(profile.getPhoto()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest("照片路径非法！");
        }
        //主键与角色/岗位一致：由 MyBatis-Plus 雪花算法生成，同时忽略客户端传入的 id
        profile.setId(null);
        profile.setDelFlag(0);
        mdPersonMapper.insert(profile);
        // 部门/岗位随人员一并保存（关系表 md_person_dept / md_person_post）
        saveDeptPost(profile.getId(), profile.getDeptIds(), profile.getPostIds());
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改人员档案
     */
    @PostMapping(value = "/edit")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody MdPerson profile) {
        if (StringUtil.isEmpty(profile.getId())) {
            return Result.badRequest("缺少人员ID！");
        }
        MdPerson existed = mdPersonMapper.selectById(profile.getId());
        if (existed == null) {
            return Result.badRequest("人员不存在！");
        }
        String enumError = validateEnums(profile);
        if (enumError != null) {
            return Result.badRequest(enumError);
        }
        if (StringUtil.isNotEmpty(profile.getIdCard())) {
            long used = mdPersonMapper.selectCount(new QueryWrapper<MdPerson>()
                    .eq("id_card", profile.getIdCard()).ne("id", profile.getId()));
            if (used > 0) {
                return Result.conflict("该身份证号已存在！");
            }
        }
        if (StringUtil.isNotEmpty(profile.getMobile())) {
            long used = mdPersonMapper.selectCount(new QueryWrapper<MdPerson>()
                    .eq("mobile", profile.getMobile()).ne("id", profile.getId()));
            if (used > 0) {
                return Result.conflict("该手机号已被其他人员使用！");
            }
        }
        if (StringUtil.isNotEmpty(profile.getEmail())) {
            long used = mdPersonMapper.selectCount(new QueryWrapper<MdPerson>()
                    .eq("email", profile.getEmail()).ne("id", profile.getId()));
            if (used > 0) {
                return Result.conflict("该邮箱已被其他人员使用！");
            }
        }
        String photo;
        try {
            photo = normalizePhoto(profile.getPhoto());
        } catch (IllegalArgumentException e) {
            return Result.badRequest("照片路径非法！");
        }
        // 只更新允许编辑的字段
        existed.setRealName(profile.getRealName());
        existed.setIdCard(profile.getIdCard());
        existed.setBirthday(profile.getBirthday());
        existed.setNation(profile.getNation());
        existed.setAddress(profile.getAddress());
        existed.setNativePlace(profile.getNativePlace());
        existed.setGender(profile.getGender());
        existed.setMaritalStatus(profile.getMaritalStatus());
        existed.setEducation(profile.getEducation());
        existed.setPersonCode(profile.getPersonCode());
        existed.setPhoto(photo);
        existed.setMobile(profile.getMobile());
        existed.setEmergencyContact(profile.getEmergencyContact());
        existed.setEmergencyPhone(profile.getEmergencyPhone());
        existed.setEmail(profile.getEmail());
        existed.setEntryDate(profile.getEntryDate());
        existed.setLeaveDate(profile.getLeaveDate());
        existed.setPersonType(profile.getPersonType());
        existed.setOrderNo(profile.getOrderNo());
        existed.setStatus(profile.getStatus());
        mdPersonMapper.updateById(existed);
        // 部门/岗位随人员一并保存（null=本次不涉及，空数组=清空）
        saveDeptPost(profile.getId(), profile.getDeptIds(), profile.getPostIds());
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除人员档案（同时解除账号绑定）
     */
    @DeleteMapping(value = "/delete")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        if (mdPersonMapper.selectById(id) == null) {
            return Result.badRequest("人员不存在！");
        }
        mdPersonMapper.deleteById(id);
        // 解除引用该人员的账号绑定（账号保留为无人员信息的账号）
        // 同步清理该人员的部门/岗位关系
        mdPersonMapper.deleteDeptRelation(id);
        mdPersonMapper.deletePostRelation(id);
        mdPersonMapper.unbindPerson(id);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 通过id批量删除人员档案（同时解除账号绑定）
     */
    @DeleteMapping(value = "/deleteBatch")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的32位人员ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        mdPersonMapper.deleteByIds(idList);
        for (String id : idList) {
            // 同步清理该人员的部门/岗位关系并解除账号绑定
            mdPersonMapper.deleteDeptRelation(id);
            mdPersonMapper.deletePostRelation(id);
            mdPersonMapper.unbindPerson(id);
        }
        return Result.success(TipConst.DEL_BATCH_SUCC);
    }

    /**
     * 下拉框（人员选项，供账号绑定选择）
     */
    @GetMapping("/select")
    @PreAuthorize("hasRole('admin')")
    public Result<List<MdPerson>> select() {
        QueryWrapper<MdPerson> qw = new QueryWrapper<MdPerson>()
                .select("id", "real_name", "person_code")
                .orderByAsc("order_no");
        return Result.success(mdPersonMapper.selectList(qw));
    }

    /**
     * 校验参数是否已存在（身份证/手机/邮箱唯一性）
     */
    @GetMapping("/validate")
    @PreAuthorize("hasRole('admin')")
    public Result<String> validate(MdPerson profile) {
        String column;
        String value;
        if (StringUtil.isNotEmpty(profile.getIdCard())) {
            column = "id_card";
            value = profile.getIdCard();
        } else if (StringUtil.isNotEmpty(profile.getMobile())) {
            column = "mobile";
            value = profile.getMobile();
        } else if (StringUtil.isNotEmpty(profile.getEmail())) {
            column = "email";
            value = profile.getEmail();
        } else {
            return Result.badRequest("缺少待校验字段！");
        }
        QueryWrapper<MdPerson> qw = new QueryWrapper<MdPerson>().eq(column, value);
        if (StringUtil.isNotEmpty(profile.getId())) {
            qw.ne("id", profile.getId());
        }
        boolean exists = mdPersonMapper.selectCount(qw) > 0;
        return exists
                ? Result.conflict(TipConst.PARAM_EXISTS)
                : Result.success(TipConst.PARAM_AVAILABLE);
    }

    /**
     * 导出excel（人员档案：枚举列按数据字典翻译成中文）
     */
    @GetMapping(value = "/exportExcel")
    @PreAuthorize("hasRole('admin')")
    public void exportExcel(HttpServletResponse response) throws IOException {
        List<MdPerson> persons = mdPersonMapper.selectList(new QueryWrapper<MdPerson>().orderByDesc("create_time"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("人员管理", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        try (ExcelWriter writer = FesodSheet.write(response.getOutputStream(), MdPerson.class)
                .excelType(org.apache.fesod.sheet.support.ExcelTypeEnum.XLSX)
                .build()) {
            writer.write(persons, FesodSheet.writerSheet(0, "人员管理").build());
        }
    }

    /**
     * 通过excel导入人员档案（表头与导出一致；枚举列支持中文文案或编码）
     * 说明：部门/岗位属关系表数据，Excel 不参与导入，请导入后在编辑弹窗中维护
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> importExcel(HttpServletRequest request) throws IOException {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            return Result.badRequest("请使用 multipart/form-data 上传 Excel 文件！");
        }
        String message = "";
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile multipartFile = entity.getValue();
            List<MdPersonImport> dataList;
            try (InputStream inputStream = multipartFile.getInputStream()) {
                FesodSheetListener<MdPersonImport> listener = new FesodSheetListener<>();
                FesodSheet.read(inputStream, MdPersonImport.class, listener)
                        .sheet()
                        .headRowNumber(1)
                        .doRead();
                dataList = listener.getDataList();
            } catch (Exception e) {
                return Result.badRequest("Excel 解析失败，请使用「导出」得到的表头填写后再导入！");
            }
            if (dataList.size() > MAX_IMPORT_ROWS) {
                return Result.badRequest("单次导入不超过 " + MAX_IMPORT_ROWS + " 条数据！");
            }
            message = saveImportRows(dataList);
        }
        return Result.success(message);
    }

    /**
     * 解析并落库导入行，返回统计信息（逐行校验，失败行不影响其它行）
     */
    private String saveImportRows(List<MdPersonImport> dataList) {
        Map<String, Map<String, Integer>> dicts = buildImportDicts();
        List<String> idCards = new ArrayList<>();
        List<String> mobiles = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        for (MdPersonImport row : dataList) {
            if (StringUtil.isNotEmpty(row.getIdCard())) {
                idCards.add(row.getIdCard().trim());
            }
            if (StringUtil.isNotEmpty(row.getMobile())) {
                mobiles.add(row.getMobile().trim());
            }
            if (StringUtil.isNotEmpty(row.getEmail())) {
                emails.add(row.getEmail().trim());
            }
        }
        Set<String> existIdCards = selectExistingValues("id_card", idCards);
        Set<String> existMobiles = selectExistingValues("mobile", mobiles);
        Set<String> existEmails = selectExistingValues("email", emails);

        Set<String> fileIdCards = new HashSet<>();
        Set<String> fileMobiles = new HashSet<>();
        Set<String> fileEmails = new HashSet<>();
        List<MdPerson> toInsert = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = dataList.size();
        int rowNo = 1;
        for (MdPersonImport row : dataList) {
            rowNo++;
            try {
                MdPerson person = toEntity(row, dicts);
                if (isBlankPerson(person)) {
                    // 空行不计入统计
                    total--;
                    continue;
                }
                String invalid = validateEnums(person);
                if (invalid != null) {
                    throw new IllegalArgumentException(invalid.replace("！", ""));
                }
                if (StringUtil.isNotEmpty(person.getEmail()) && !EMAIL_PATTERN.matcher(person.getEmail()).matches()) {
                    throw new IllegalArgumentException("邮箱格式不正确");
                }
                if (StringUtil.isNotEmpty(person.getIdCard())) {
                    if (existIdCards.contains(person.getIdCard()) || !fileIdCards.add(person.getIdCard())) {
                        throw new IllegalArgumentException("身份证号已存在（" + person.getIdCard() + "）");
                    }
                }
                if (StringUtil.isNotEmpty(person.getMobile())) {
                    if (existMobiles.contains(person.getMobile()) || !fileMobiles.add(person.getMobile())) {
                        throw new IllegalArgumentException("手机号已被使用（" + person.getMobile() + "）");
                    }
                }
                if (StringUtil.isNotEmpty(person.getEmail())) {
                    if (existEmails.contains(person.getEmail()) || !fileEmails.add(person.getEmail())) {
                        throw new IllegalArgumentException("邮箱已被使用（" + person.getEmail() + "）");
                    }
                }
                person.setId(null);
                person.setDelFlag(0);
                if (person.getStatus() == null) {
                    // 未填写人员状态时默认「在职」，避免列表出现空状态
                    person.setStatus(1);
                }
                toInsert.add(person);
            } catch (IllegalArgumentException e) {
                if (errors.size() < MAX_IMPORT_ERROR_DETAIL) {
                    errors.add("第" + rowNo + "行：" + e.getMessage());
                }
            }
        }
        for (MdPerson person : toInsert) {
            mdPersonMapper.insert(person);
        }
        int success = toInsert.size();
        int fail = total - success;
        StringBuilder sb = new StringBuilder();
        sb.append("共").append(total).append("条数据,成功：").append(success)
                .append("条数据，失败：").append(fail).append("条数据");
        if (!errors.isEmpty()) {
            sb.append("；失败明细：").append(String.join("；", errors));
            if (fail > errors.size()) {
                sb.append("；其余 ").append(fail - errors.size()).append(" 条同类问题不再列出");
            }
        }
        return sb.toString();
    }

    /**
     * 导入用字典映射：字段 -> (文案/编码 -> 编码值)。
     * 先放内置默认值保证字典表为空时也能导入，再用 DB 字典项覆盖（字典管理里改文案后仍可识别）
     */
    private Map<String, Map<String, Integer>> buildImportDicts() {
        Map<String, Map<String, Integer>> dicts = new HashMap<>();
        dicts.put("gender", labelMap("男", "1", "女", "2"));
        dicts.put("marital_status", labelMap("未婚", "1", "已婚", "2", "离异", "3", "丧偶", "4"));
        dicts.put("education", labelMap("小学", "1", "初中", "2", "高中", "3", "大专", "4", "本科", "5", "研究生", "6"));
        dicts.put("person_type", labelMap("正式员工", "1", "外协", "2", "临时人员", "3"));
        dicts.put("person_status", labelMap("在职", "1", "试用", "2", "离职", "3", "退休", "4"));
        List<String> codes = new ArrayList<>(dicts.keySet());
        List<Map<String, Object>> items = mdDictItemMapper.selectByCodes(codes);
        for (Map<String, Object> item : items) {
            String dictCode = dictColumn(item, "dictCode");
            String value = dictColumn(item, "dictItemValue");
            if (StringUtil.isEmpty(dictCode) || StringUtil.isEmpty(value)) {
                continue;
            }
            Integer code;
            try {
                code = Integer.valueOf(value.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            Map<String, Integer> mapping = dicts.computeIfAbsent(dictCode, k -> new HashMap<>());
            mapping.putIfAbsent(String.valueOf(code), code);
            String text = dictColumn(item, "dictItemText");
            if (StringUtil.isNotEmpty(text)) {
                mapping.put(text.trim(), code);
            }
        }
        return dicts;
    }

    /** 兼容 MyBatis 返回列标签大小写差异，按名取字典项字符串值 */
    private static String dictColumn(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Integer> labelMap(String... labelAndValue) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i + 1 < labelAndValue.length; i += 2) {
            map.put(labelAndValue[i], Integer.valueOf(labelAndValue[i + 1]));
        }
        return map;
    }

    /**
     * 字典文案/编码 -> 编码值；无法识别时抛出可读异常（由调用方记为失败行）
     */
    private Integer parseDictValue(String text, Map<String, Integer> mapping, String fieldName) {
        if (StringUtil.isEmpty(text)) {
            return null;
        }
        String key = text.trim();
        Integer code = mapping.get(key);
        if (code != null) {
            return code;
        }
        try {
            return Integer.valueOf(key);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "「" + key + "」无法识别");
        }
    }

    /**
     * 导入行 -> 实体（部门/岗位不参与导入）
     */
    private MdPerson toEntity(MdPersonImport row, Map<String, Map<String, Integer>> dicts) {
        MdPerson person = new MdPerson();
        person.setRealName(trimToNull(row.getRealName()));
        person.setIdCard(trimToNull(row.getIdCard()));
        person.setBirthday(row.getBirthday());
        person.setNation(trimToNull(row.getNation()));
        person.setAddress(trimToNull(row.getAddress()));
        person.setNativePlace(trimToNull(row.getNativePlace()));
        person.setGender(parseDictValue(row.getGender(), dicts.get("gender"), "性别"));
        person.setMaritalStatus(parseDictValue(row.getMaritalStatus(), dicts.get("marital_status"), "婚姻状况"));
        person.setEducation(parseDictValue(row.getEducation(), dicts.get("education"), "学历"));
        person.setPersonCode(trimToNull(row.getPersonCode()));
        person.setMobile(trimToNull(row.getMobile()));
        person.setEmergencyContact(trimToNull(row.getEmergencyContact()));
        person.setEmergencyPhone(trimToNull(row.getEmergencyPhone()));
        person.setEmail(trimToNull(row.getEmail()));
        person.setEntryDate(row.getEntryDate());
        person.setLeaveDate(row.getLeaveDate());
        person.setPersonType(parseDictValue(row.getPersonType(), dicts.get("person_type"), "人员类型"));
        person.setOrderNo(row.getOrderNo());
        person.setStatus(parseDictValue(row.getStatus(), dicts.get("person_status"), "人员状态"));
        return person;
    }

    /**
     * 判断导入行是否为空行（全列留空）
     */
    private boolean isBlankPerson(MdPerson person) {
        return StringUtil.isEmpty(person.getRealName())
                && StringUtil.isEmpty(person.getIdCard())
                && StringUtil.isEmpty(person.getMobile())
                && StringUtil.isEmpty(person.getEmail())
                && StringUtil.isEmpty(person.getPersonCode())
                && person.getGender() == null
                && person.getStatus() == null
                && person.getPersonType() == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 查询库内已存在的唯一值（身份证/手机号/邮箱），用于导入去重
     */
    private Set<String> selectExistingValues(String column, List<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null || values.isEmpty()) {
            return result;
        }
        List<String> distinct = new ArrayList<>(new HashSet<>(values));
        List<MdPerson> rows = mdPersonMapper.selectList(
                new QueryWrapper<MdPerson>().select(column).in(column, distinct));
        for (MdPerson row : rows) {
            String value;
            if ("id_card".equals(column)) {
                value = row.getIdCard();
            } else if ("mobile".equals(column)) {
                value = row.getMobile();
            } else {
                value = row.getEmail();
            }
            if (StringUtil.isNotEmpty(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 回收站 列表
     */
    @GetMapping("/listRecycleBin")
    @PreAuthorize("hasRole('admin')")
    public Result<IPage<MdPerson>> listRecycleBin(MdPerson person,
                                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<MdPerson> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        return Result.success(page.setRecords(mdPersonMapper.listRecycleBin(page, person)));
    }

    /**
     * 回收站 彻底删除（物理删除档案，并清理该人员的部门/岗位关系）
     */
    @DeleteMapping(value = "/deleteRecycleBin")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteRecycleBin(@RequestParam(name = "id") String id) {
        if (StringUtil.isEmpty(id) || !ID_PATTERN.matcher(id).matches()) {
            return Result.badRequest("人员ID非法！");
        }
        purgePersons(List.of(id));
        return Result.success("彻底删除成功！");
    }

    /**
     * 回收站 批量彻底删除
     */
    @DeleteMapping(value = "/deleteRecycleBinBatch")
    @PreAuthorize("hasRole('admin')")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteRecycleBinBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = parseIds(ids);
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的32位人员ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        purgePersons(idList);
        return Result.success("彻底删除成功！");
    }

    /**
     * 彻底删除（仅回收站内的记录会被删除，关系表乐观限定在同一批人员上）
     */
    private void purgePersons(List<String> ids) {
        String[] idArray = ids.toArray(new String[0]);
        mdPersonMapper.deleteRecycleBinDept(idArray);
        mdPersonMapper.deleteRecycleBinPost(idArray);
        mdPersonMapper.deleteRecycleBin(idArray);
        // 兜底：清理仍指向已被彻底删除人员的账号绑定（账号本身保留）
        for (String id : ids) {
            mdPersonMapper.unbindPerson(id);
        }
    }

    /**
     * 回收站 还原
     */
    @PutMapping("/revertRecycleBin")
    @PreAuthorize("hasRole('admin')")
    public Result<String> revertRecycleBin(@RequestBody Map<String, String> map) {
        String id = map.get("id");
        if (StringUtil.isEmpty(id) || !ID_PATTERN.matcher(id).matches()) {
            return Result.badRequest("人员ID非法！");
        }
        mdPersonMapper.revertRecycleBin(new String[]{id});
        return Result.success("还原成功！");
    }

    /**
     * 回收站 批量还原
     */
    @PutMapping("/revertRecycleBinBatch")
    @PreAuthorize("hasRole('admin')")
    public Result<String> revertRecycleBinBatch(@RequestBody Map<String, String> map) {
        List<String> idList = parseIds(map.get("ids"));
        if (idList == null) {
            return Result.badRequest("ids 参数非法（需为逗号分隔的32位人员ID，单次不超过 " + MAX_BATCH_IDS + " 条）！");
        }
        mdPersonMapper.revertRecycleBin(idList.toArray(new String[0]));
        return Result.success("还原成功！");
    }
}
