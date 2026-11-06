package com.xuanchen.online.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.online.entity.OnlineForm;
import com.xuanchen.online.service.IOnlineFormService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制器-->在线表单（流程审批）
 * 数据模型：前端表单设计器产出的字段列表 JSON 整体存取；
 * POST 一律 FormData（与后端 @RequestParam 绑定约定一致）。
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Slf4j
@RestController
@RequestMapping("/online/form")
public class OnlineFormController {

    @Autowired
    private IOnlineFormService formService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页列表（本人全部 + admin 全部）
     */
    @GetMapping("/list")
    public Result<Page<OnlineForm>> list(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                         @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                         @RequestParam(name = "keyword", required = false) String keyword,
                                         HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(formService.pageList(pageNo, pageSize, keyword, username, isAdmin()));
    }

    /**
     * 下拉选项列表（流程设计绑定表单用，轻量不含 schema）
     */
    @GetMapping("/options")
    public Result<List<OnlineForm>> options(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(formService.listOptions(username, isAdmin()));
    }

    /**
     * 详情（含 schema）
     */
    @GetMapping("/detail")
    public Result<OnlineForm> detail(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success(formService.getDetail(id, username, isAdmin()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 保存（新建/更新；FormData：id 可空、formName、remark、schemaJson）
     */
    @PostMapping("/save")
    public Result<OnlineForm> save(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            OnlineForm form = formService.saveForm(request.getParameter("id"), request.getParameter("formName"),
                    request.getParameter("remark"), request.getParameter("schemaJson"), username, isAdmin());
            return Result.success("保存成功!", form);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("在线表单保存失败", e);
            return Result.error("保存失败，请重试！");
        }
    }

    /**
     * 删除（逻辑删）
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            formService.deleteForm(id, username, isAdmin());
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("在线表单删除失败 id={}", id, e);
            return Result.error("删除失败，请重试！");
        }
    }

    /**
     * sb4（Spring Security）判断当前用户是否 admin
     */
    private static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
    }
}
