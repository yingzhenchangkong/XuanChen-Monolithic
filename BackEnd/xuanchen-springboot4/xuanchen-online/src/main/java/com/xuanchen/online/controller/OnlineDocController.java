package com.xuanchen.online.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.filemanage.entity.FileCollaborator;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.online.entity.OnlineDocVO;
import com.xuanchen.online.service.IOnlineDocService;
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
 * 控制器-->在线协同文档（Word/Excel/PPT）
 * <p>内容落文件中心（file_info.biz_type='online'），协同编辑复用
 * /filemanage/office/config + ONLYOFFICE DS 既有链路（前端 /fileedit/:fileId）。
 * POST 一律 FormData（与后端 @RequestParam 绑定约定一致）。</p>
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Slf4j
@RestController
@RequestMapping("/online/doc")
public class OnlineDocController {

    @Autowired
    private IOnlineDocService onlineDocService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 新建空白文档（FormData：docType=word/cell/slide、docName）
     */
    @PostMapping("/create")
    public Result<FileInfo> create(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            FileInfo file = onlineDocService.create(request.getParameter("docType"),
                    request.getParameter("docName"), username);
            return Result.success("创建成功!", file);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("在线文档创建失败", e);
            return Result.error("创建失败，请重试！");
        }
    }

    /**
     * 分页列表（本人创建 + 我是协作者；admin 全部）
     */
    @GetMapping("/list")
    public Result<Page<OnlineDocVO>> list(@RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                          @RequestParam(name = "pageSize", defaultValue = "10") long pageSize,
                                          @RequestParam(name = "keyword", required = false) String keyword,
                                          HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(onlineDocService.page(pageNo, pageSize, keyword, username, isAdmin()));
    }

    /**
     * 重命名（FormData：fileId、docName）
     */
    @PostMapping("/rename")
    public Result<Void> rename(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            onlineDocService.rename(request.getParameter("fileId"), request.getParameter("docName"),
                    username, isAdmin());
            return Result.success("重命名成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("在线文档重命名失败 fileId={}", request.getParameter("fileId"), e);
            return Result.error("重命名失败，请重试！");
        }
    }

    /**
     * 删除（进文件中心回收站，仅属主/admin）
     */
    @DeleteMapping("/remove")
    public Result<Void> remove(@RequestParam(name = "fileId") String fileId, HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            onlineDocService.remove(fileId, username, isAdmin());
            return Result.success("删除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("在线文档删除失败 fileId={}", fileId, e);
            return Result.error("删除失败，请重试！");
        }
    }

    /**
     * 协作者列表（仅属主/admin）
     */
    @GetMapping("/collaborator/list")
    public Result<List<FileCollaborator>> collaborators(@RequestParam(name = "fileId") String fileId,
                                                        HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success(onlineDocService.collaborators(fileId, username, isAdmin()));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 添加协作者（FormData：fileId、username、permission=1可编辑/2只读）
     */
    @PostMapping("/collaborator/add")
    public Result<Void> addCollaborator(HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            String permissionRaw = request.getParameter("permission");
            Integer permission = permissionRaw == null || permissionRaw.isBlank() ? null
                    : Integer.valueOf(permissionRaw);
            onlineDocService.addCollaborator(request.getParameter("fileId"), request.getParameter("username"),
                    permission, username, isAdmin());
            return Result.success("添加成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("添加协作者失败 fileId={}", request.getParameter("fileId"), e);
            return Result.error("添加失败，请重试！");
        }
    }

    /**
     * 移除协作者
     */
    @DeleteMapping("/collaborator/delete")
    public Result<Void> removeCollaborator(@RequestParam(name = "fileId") String fileId,
                                           @RequestParam(name = "username") String targetUser,
                                           HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            onlineDocService.removeCollaborator(fileId, targetUser, username, isAdmin());
            return Result.success("移除成功!", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("移除协作者失败 fileId={} target={}", fileId, targetUser, e);
            return Result.error("移除失败，请重试！");
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
