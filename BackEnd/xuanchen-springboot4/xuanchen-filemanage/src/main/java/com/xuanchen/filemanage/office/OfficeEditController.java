package com.xuanchen.filemanage.office;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器-->ONLYOFFICE 协同编辑
 * <p>/office/config 需登录；/office/download、/office/callback 为 Document Server
 * 服务端调用（匿名白名单），分别以短时效签名令牌 / 回调 JWT 校验防伪造。</p>
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
@RequestMapping("/office")
public class OfficeEditController {

    @Autowired
    private OfficeEditService officeEditService;
    @Autowired
    private OfficeJwtUtil jwtUtil;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 获取编辑器配置（含 DS 地址与整签 config，前端直接喂 DocsAPI.DocEditor）
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> config(@RequestParam(name = "fileId") String fileId,
                                              @RequestParam(name = "mode", required = false, defaultValue = "edit") String mode,
                                              HttpServletRequest request) {
        String username = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        try {
            return Result.success(officeEditService.buildConfig(fileId, username, isAdmin(), "view".equals(mode)));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("构建协同编辑配置失败 fileId={}", fileId, e);
            return Result.error("获取编辑配置失败，请重试！");
        }
    }

    /**
     * 文档下发（DS 拉取）：st 为短时效签名令牌，fileId 须与令牌内一致
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam(name = "fileId") String fileId,
                                           @RequestParam(name = "st") String st) {
        try {
            Map<String, Object> claims = jwtUtil.verify(st);
            if (!fileId.equals(String.valueOf(claims.get("fid")))) {
                return ResponseEntity.status(403).build();
            }
            return officeEditService.loadDocument(fileId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("文档下发被拒绝 fileId={} 原因={}", fileId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("文档下发失败 fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DS 保存回调（匿名+JWT 验签）。DS 约定：必须返回 {"error":0} 表示接收成功
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestParam(name = "xcfileid", required = false) String fileId,
                                        @org.springframework.web.bind.annotation.RequestBody Map<String, Object> body) {
        try {
            officeEditService.handleCallback(fileId, body);
            return Map.of("error", 0);
        } catch (Exception e) {
            log.error("协同保存回调处理失败 xcfileid={}", fileId, e);
            //返回非 0 让 DS 稍后重试
            return Map.of("error", 1);
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
