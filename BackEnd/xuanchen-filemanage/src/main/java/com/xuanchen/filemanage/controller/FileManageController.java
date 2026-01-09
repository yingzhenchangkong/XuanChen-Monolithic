package com.xuanchen.filemanage.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 控制器 --> 文件管理
 *
 * @author XuanChen
 * @date 2025-04-21
 */
@RestController
@RequestMapping("/filemanage")
public class FileManageController {
    @Value("${xuanchen.path.upload}")
    private String uploadPath;

    /**
     * 通用 文件上传
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/upload")
    public Result upload(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String customPath = request.getParameter("customPath");
        if (StringUtil.isNotEmpty(customPath)) {
            if (customPath.contains("../") || customPath.contains("..\\")) {
                throw new Exception("上传目录格式非法!");
            }
        } else {
            customPath = "";
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        // 获取上传文件对象
        MultipartFile multipartFile = multipartRequest.getFile("file");
        File file = new File(uploadPath + File.separator + customPath + File.separator);
        if (!file.exists()) {
            // 创建文件根目录
            file.mkdirs();
        }
        // 获取文件名
        String originalFilename = multipartFile.getOriginalFilename();
        String fileName = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "_" + System.currentTimeMillis() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String savePath = file.getPath() + File.separator + fileName;
        File saveFile = new File(savePath);
        FileCopyUtils.copy(multipartFile.getBytes(), saveFile);
        String dbPath = customPath + File.separator + fileName;
        return Result.success(dbPath);
    }

    /**
     * 预览图片 、 下载文件
     *
     * @param request
     * @param response
     */
    @GetMapping(value = "/static/**")
    public void view(HttpServletRequest request, HttpServletResponse response) {
        String imgPath = extractPathFromPattern(request);
        imgPath = URLDecoder.decode(imgPath, StandardCharsets.UTF_8);
        boolean tf = false;
        if (StringUtil.isEmpty(imgPath)) {
            imgPath = "src/main/resources/static/image/noimg.png";
            tf = true;
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            imgPath = imgPath.replace("..", "").replace("../", "");
            if (imgPath.endsWith(",")) {
                imgPath = imgPath.substring(0, imgPath.length() - 1);
            }
            String filePath = "";
            if (tf) {
                filePath = imgPath;
            } else {
                filePath = uploadPath + File.separator + imgPath;

            }
            File file = new File(filePath);
            if (!file.exists()) {
                response.setStatus(404);
                return;
            }

            // 设置强制下载不打开
            response.setContentType("application/force-download");
            response.addHeader("Content-Disposition", "attachment;fileName=" + new String(file.getName().getBytes("UTF-8"), "iso-8859-1"));

            inputStream = new BufferedInputStream(new FileInputStream(filePath));
            outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            response.flushBuffer();
        } catch (IOException e) {
            response.setStatus(404);
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 把指定URL后的字符串全部截断当成参数
     * 这么做是为了防止URL中包含中文或者特殊字符（/等）时，匹配不了的问题
     *
     * @param request
     * @return
     */
    private static String extractPathFromPattern(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }
}
