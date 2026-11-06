package com.xuanchen.filemanage.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.filemanage.service.IFileShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 控制器-->文件共享公开访问（二期，免登录，已加入两端鉴权白名单）
 * 安全约束：shareNo 为 32 位随机 UUID 不可枚举；可选提取码；
 * 撤销/过期/源文件删除均即时失效；下载为现签预签名 URL（30 分钟自动失效）。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Slf4j
@RestController
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private IFileShareService fileShareService;

    /**
     * 分享信息（公开分享页展示用，不含下载地址）
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestParam(name = "no") String no) {
        try {
            return Result.success(fileShareService.resolve(no, null, false));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("分享信息获取失败 no={}", no, e);
            return Result.error("分享信息获取失败，请重试！");
        }
    }

    /**
     * 分享下载（校验提取码/有效期后签发一次性预签名下载地址）
     */
    @GetMapping("/download")
    public Result<Map<String, Object>> download(@RequestParam(name = "no") String no,
                                                @RequestParam(name = "code", required = false) String code) {
        try {
            return Result.success(fileShareService.resolve(no, code, true));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("分享下载失败 no={}", no, e);
            return Result.error("分享下载失败，请重试！");
        }
    }
}
