package com.xuanchen.system.sysdatasource.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceUsageChecker;
import com.xuanchen.common.entity.DatasourceOption;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.common.utils.SysAesUtil;
import com.xuanchen.common.utils.SqlGuard;
import com.xuanchen.system.sysdatasource.entity.SysDatasource;
import com.xuanchen.system.sysdatasource.service.ISysDatasourceService;
import com.xuanchen.system.sysdatasource.service.impl.SysDatasourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制器-->统一数据源（sys_datasource）：报表/API/大屏/代码生成共用。
 * <p>
 * 鉴权口径：管理与写操作仅 admin 角色；{@code /select} 是登录即可访问的只读端点，
 * 只返回 id/名称/类型/状态，绝不返回 JDBC 连接串与口令（其余模块有各自的下拉包装端点）。
 * <p>
 * 写操作完成后调用 {@link SysDatasourceProvider#evict(String)}，让读取缓存与连接池立即失效，
 * 保证「改完数据源马上生效」。
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@RestController
@RequestMapping("/system/datasource")
public class SysDatasourceController {

    @Autowired
    private ISysDatasourceService sysDatasourceService;
    @Autowired
    private SysAesUtil sysAesUtil;
    @Autowired
    private IAuthServiceCommon authServiceCommon;
    @Autowired
    private SysDatasourceProvider datasourceProvider;
    /** 各业务模块上报的引用检查，删除数据源前汇总校验 */
    @Autowired(required = false)
    private List<DatasourceUsageChecker> usageCheckers = new ArrayList<>();

    /** 数据源下拉：统一走 SPI 的 listOptions（不解密口令），登录即可访问 */
    @GetMapping("/select")
    public Result<List<DatasourceOption>> select() {
        return Result.success(datasourceProvider.listOptions());
    }

    @GetMapping("/list")
    @RequiresRoles("admin")
    public Result<List<SysDatasource>> list() {
        QueryWrapper<SysDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<SysDatasource> list = sysDatasourceService.list(queryWrapper);
        list.forEach(ds -> {
            ds.setHasPassword(StringUtil.isNotEmpty(ds.getPassword()));
            ds.setPassword("");
        });
        return Result.success(list);
    }

    @PostMapping("/add")
    @RequiresRoles("admin")
    public Result<String> add(@RequestBody SysDatasource datasource, HttpServletRequest request) {
        String error = validate(datasource);
        if (error != null) {
            return Result.badRequest(error);
        }
        datasource.setPassword(sysAesUtil.encrypt(datasource.getPassword()));
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        datasource.setCreateBy(userName);
        datasource.setCreateTime(LocalDateTime.now());
        // 不接受客户端传入的逻辑删除标记，新增一律为正常状态
        datasource.setDelFlag(0);
        sysDatasourceService.save(datasource);
        datasourceProvider.evict(datasource.getId());
        return Result.success("添加成功!");
    }

    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    @RequiresRoles("admin")
    public Result<String> edit(@RequestBody SysDatasource datasource) {
        if (StringUtil.isEmpty(datasource.getId())) {
            return Result.badRequest("缺少数据源ID！");
        }
        String error = validate(datasource);
        if (error != null) {
            return Result.badRequest(error);
        }
        SysDatasource db = sysDatasourceService.getById(datasource.getId());
        if (db == null) {
            return Result.badRequest("数据源不存在或已被删除！");
        }
        SysDatasource update = new SysDatasource();
        update.setId(datasource.getId());
        update.setDsName(datasource.getDsName());
        update.setDsType(datasource.getDsType());
        update.setJdbcUrl(datasource.getJdbcUrl());
        update.setUsername(datasource.getUsername());
        update.setStatus(datasource.getStatus());
        update.setPassword(StringUtil.isEmpty(datasource.getPassword())
                ? db.getPassword() : sysAesUtil.encrypt(datasource.getPassword()));
        if (!sysDatasourceService.updateById(update)) {
            return Result.badRequest("数据源不存在或已被删除！");
        }
        datasourceProvider.evict(datasource.getId());
        return Result.success("修改成功!");
    }

    @DeleteMapping("/delete")
    @RequiresRoles("admin")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        List<String> usages = new ArrayList<>();
        for (DatasourceUsageChecker checker : usageCheckers) {
            try {
                long count = checker.count(id);
                if (count > 0) {
                    usages.add(checker.describe(count));
                }
            } catch (Exception e) {
                // 单个模块检查失败不阻塞删除，避免监控类异常把管理功能锁死
                usages.add(checker.module() + "（引用检查失败，请人工确认）");
            }
        }
        if (!usages.isEmpty()) {
            return Result.badRequest("该数据源仍被引用，请先解除引用再删除：" + String.join("、", usages));
        }
        sysDatasourceService.removeById(id);
        datasourceProvider.evict(id);
        return Result.success("删除成功!");
    }

    @PostMapping("/testConnection")
    @RequiresRoles("admin")
    public Result<String> testConnection(@RequestBody SysDatasource datasource) {
        if (datasource == null) {
            return Result.badRequest("参数不能为空！");
        }
        // 列表页「测试连接」只传 id；编辑/新建弹窗传明文口令。此处统一归一化为明文，
        // service 只负责连库，不再做第二次解密（历史 bug：双重解密导致空口令）。
        SysDatasource stored = StringUtil.isEmpty(datasource.getId())
                ? null : sysDatasourceService.getById(datasource.getId());
        String plainPassword;
        try {
            plainPassword = resolvePlainPassword(datasource.getPassword(), stored);
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        }
        SysDatasource probe = new SysDatasource();
        probe.setDsType(pick(datasource.getDsType(), stored == null ? null : stored.getDsType()));
        probe.setJdbcUrl(pick(datasource.getJdbcUrl(), stored == null ? null : stored.getJdbcUrl()));
        probe.setUsername(pick(datasource.getUsername(), stored == null ? null : stored.getUsername()));
        probe.setPassword(plainPassword);
        if (StringUtil.isEmpty(probe.getJdbcUrl())) {
            return Result.badRequest("JDBC 连接串不能为空！");
        }
        String error = sysDatasourceService.testConnection(probe);
        if (error != null) {
            return Result.badRequest(error);
        }
        return Result.success("连接成功!");
    }

    /** 口令归一化：密文解密成明文，明文原样返回；未传口令时回查库中已存密文 */
    private String resolvePlainPassword(String input, SysDatasource stored) {
        if (StringUtil.isNotEmpty(input)) {
            String decrypted = sysAesUtil.decryptOrNull(input);
            return decrypted == null ? input : decrypted;
        }
        if (stored != null && StringUtil.isNotEmpty(stored.getPassword())) {
            String decrypted = sysAesUtil.decryptOrNull(stored.getPassword());
            if (decrypted == null) {
                throw new BusinessException(500, "数据源密码解密失败，请重新保存该数据源");
            }
            return decrypted;
        }
        return null;
    }

    private static String pick(String primary, String fallback) {
        return StringUtil.isEmpty(primary) ? fallback : primary;
    }

    private String validate(SysDatasource datasource) {
        if (StringUtil.isEmpty(datasource.getDsName())) {
            return "数据源名称不能为空！";
        }
        if (StringUtil.isEmpty(datasource.getDsType())) {
            return "数据源类型不能为空！";
        }
        if (StringUtil.isEmpty(datasource.getJdbcUrl())) {
            return "JDBC 连接串不能为空！";
        }
        SqlGuard.validateJdbcUrl(datasource.getJdbcUrl());
        SqlGuard.validateTypeMatchesUrl(datasource.getDsType(), datasource.getJdbcUrl());
        return null;
    }
}
