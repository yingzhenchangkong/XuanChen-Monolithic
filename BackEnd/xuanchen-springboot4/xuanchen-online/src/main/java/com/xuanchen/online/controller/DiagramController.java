package com.xuanchen.online.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.online.entity.OnlineDiagram;
import com.xuanchen.online.service.IOnlineDiagramService;
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

/**
 * 控制器-->在线文档-流程图/架构图
 * 数据模型：前端 AntV X6 graph.toJSON() 快照 JSON 整体存取；
 * POST 一律 FormData（与后端 @RequestParam 绑定约定一致）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
@RequestMapping("/online/diagram")
public class DiagramController {

    @Autowired
    private IOnlineDiagramService diagramService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 分页列表（本人全部 + admin 全部）
     */
    @GetMapping("/list")
    public Result<Page<OnlineDiagram>> list(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                          @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                          @RequestParam(name = "keyword", required = false) String keyword,
                                          HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(diagramService.pageList(pageNo, pageSize, keyword, username, isAdmin()));
    }

    /**
     * 详情（含快照）
     */
    @GetMapping("/detail")
    public Result<OnlineDiagram> detail(@RequestParam(name = "id") String id, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success(diagramService.getDetail(id, username, isAdmin()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 保存（新建/更新；FormData：id 可空、docName、snapshot）
     */
    @PostMapping("/save")
    public Result<OnlineDiagram> save(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            OnlineDiagram doc = diagramService.saveDoc(request.getParameter("id"), request.getParameter("docName"),
                    request.getParameter("snapshot"), username, isAdmin());
            return Result.success("保存成功!", doc);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("流程图保存失败", e);
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
            diagramService.deleteDoc(id, username, isAdmin());
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("流程图删除失败 id={}", id, e);
            return Result.error("删除失败，请重试！");
        }
    }

    private static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_admin".equals(a.getAuthority()));
    }
}
