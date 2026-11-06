package com.xuanchen.filemanage.util;

import com.xuanchen.common.utils.FileSecurityUtil;

import java.util.Set;

/**
 * 工具类-->通用文件魔数识别（文件中心专用，图片魔数复用 FileSecurityUtil）
 * 白名单校验规则：扩展名必须在 GENERAL_ALLOW 内，且扩展名必须能被文件头真实类型集合覆盖，
 * 防止改后缀绕过；纯文本类（txt/md/csv/json）无稳定魔数，仅限白名单直接放行。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public final class FileMagicUtil {

    private FileMagicUtil() {
    }

    /**
     * 通用上传允许的扩展名总白名单
     */
    public static final Set<String> GENERAL_ALLOW = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv", "json", "zip", "rar", "7z",
            "svg", "dwg", "dxf");

    /**
     * 无稳定魔数、按扩展名直接放行的纯文本类
     */
    private static final Set<String> TEXT_EXT = Set.of("txt", "md", "csv", "json", "svg");

    /**
     * 依据文件头识别真实类型集合（同一魔数可能对应多种 Office 扩展名）。
     *
     * @param bytes 文件头（至少 8 字节）
     * @return 可信扩展名集合；无法识别返回空集合
     */
    public static Set<String> detectExtSet(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return Set.of();
        }
        //JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Set.of("jpg", "jpeg");
        }
        //PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return Set.of("png");
        }
        //GIF87a/GIF89a
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return Set.of("gif");
        }
        //BMP
        if (bytes[0] == 'B' && bytes[1] == 'M') {
            return Set.of("bmp");
        }
        //WEBP: RIFF....WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Set.of("webp");
        }
        //PDF: %PDF-
        if (bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
            return Set.of("pdf");
        }
        //OLE2 复合文档（doc/xls/ppt 老格式）: D0 CF 11 E0 A1 B1 1A E1
        if ((bytes[0] & 0xFF) == 0xD0 && (bytes[1] & 0xFF) == 0xCF && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0 && (bytes[4] & 0xFF) == 0xA1 && (bytes[5] & 0xFF) == 0xB1
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0xE1) {
            return Set.of("doc", "xls", "ppt");
        }
        //ZIP 系容器（docx/xlsx/pptx/zip）: PK\x03\x04
        if (bytes[0] == 'P' && bytes[1] == 'K' && (bytes[2] & 0xFF) == 0x03 && (bytes[3] & 0xFF) == 0x04) {
            return Set.of("docx", "xlsx", "pptx", "zip");
        }
        //RAR: Rar!\x1A\x07
        if (bytes[0] == 'R' && bytes[1] == 'a' && bytes[2] == 'r' && bytes[3] == '!'
                && (bytes[4] & 0xFF) == 0x1A && (bytes[5] & 0xFF) == 0x07) {
            return Set.of("rar");
        }
        //7z: 37 7A BC AF 27 1C
        if ((bytes[0] & 0xFF) == 0x37 && bytes[1] == 'z' && (bytes[2] & 0xFF) == 0xBC
                && (bytes[3] & 0xFF) == 0xAF && (bytes[4] & 0xFF) == 0x27 && (bytes[5] & 0xFF) == 0x1C) {
            return Set.of("7z");
        }
        //DWG：文件头为版本字符串（AC1002~AC1032 / MC0.0 等），统一按 AC/MC 前缀识别
        if (bytes[0] == 'A' && bytes[1] == 'C' && bytes[2] == '1' && Character.isDigit((char) (bytes[3] & 0xFF))) {
            return Set.of("dwg");
        }
        if (bytes[0] == 'M' && bytes[1] == 'C' && bytes[2] == '0') {
            return Set.of("dwg");
        }
        //DXF：ASCII 文本，节头为 "  0\r\nSECTION" / "999" 注释开头，取前 64 字节做启发式
        if (looksLikeDxf(bytes)) {
            return Set.of("dxf");
        }
        return Set.of();
    }

    /**
     * DXF 启发式识别：前 4 字符为数字/空白（组码），且前 64 字节内出现 SECTION 关键字
     */
    private static boolean looksLikeDxf(byte[] bytes) {
        char c0 = (char) (bytes[0] & 0xFF);
        char c1 = (char) (bytes[1] & 0xFF);
        boolean groupCodeStart = Character.isDigit(c0) || Character.isWhitespace(c0);
        boolean groupCodeLine2 = Character.isDigit(c1) || Character.isWhitespace(c1);
        if (!groupCodeStart || !groupCodeLine2) {
            return false;
        }
        StringBuilder head = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 64); i++) {
            head.append((char) (bytes[i] & 0xFF));
        }
        return head.toString().contains("SECTION");
    }

    /**
     * 通用上传完整校验：扩展名在总白名单内 + 魔数真实类型覆盖该扩展名（纯文本类除外）。
     *
     * @param originalFilename 已剥离路径的原始文件名
     * @param bytes            文件内容（至少 12 字节）
     * @return 归一化扩展名（jpeg→jpg）
     * @throws IllegalArgumentException 校验不通过
     */
    public static String validateGeneral(String originalFilename, byte[] bytes) {
        String ext = FileSecurityUtil.extensionOf(originalFilename);
        if (!GENERAL_ALLOW.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型：" + ext);
        }
        ext = "jpeg".equals(ext) ? "jpg" : ext;
        if (TEXT_EXT.contains(ext)) {
            //纯文本类无稳定魔数，仅限总白名单直接放行
            return ext;
        }
        Set<String> realSet = detectExtSet(bytes);
        if (realSet.isEmpty() || !realSet.contains(ext)) {
            throw new IllegalArgumentException("文件内容与扩展名不符，禁止上传！");
        }
        return ext;
    }
}
