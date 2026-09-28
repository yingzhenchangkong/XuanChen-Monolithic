package com.xuanchen.common.utils;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 文件上传/访问安全工具
 * 统一收敛路径穿越防护与文件类型校验规则，所有文件入口必须经本工具校验，禁止各入口自行维护黑名单。
 *
 * @author XuanChen
 * @date 2026-09-17
 */
public final class FileSecurityUtil {

    private FileSecurityUtil() {
    }

    /**
     * 允许上传的图片扩展名（当前文件服务仅用于头像图片）
     */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /**
     * Windows 保留设备名（任意一段命中即拒绝，防止写入 CON、NUL 等设备）
     */
    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "con", "prn", "aux", "nul", "clock$",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    /**
     * 文件名/目录名中禁止出现的字符：空字节、Windows 盘符/通配/管道等；斜杠由分段处理
     */
    private static final String FORBIDDEN_CHARS = "\0:*?\"<>|";

    private static final int MAX_SEGMENT_LENGTH = 100;
    private static final int MAX_RELATIVE_PATH_LENGTH = 500;

    /**
     * 存储文件名时间戳后缀格式：年月日时分秒毫秒（如 20260917143025123）
     */
    private static final DateTimeFormatter STORED_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 校验并规范化业务子目录（上传 customPath）。
     * 规则：仅允许相对路径；逐段拒绝空段/“.”/“..”/盘符/设备名/控制字符/非法字符；统一输出“/”分隔。
     *
     * @param subPath 原始子目录，null 或空白视为无子目录
     * @return 规范化后的相对目录（“/”分隔），无子目录返回空串
     * @throws IllegalArgumentException 路径非法
     */
    public static String sanitizeSubPath(String subPath) {
        if (subPath == null || subPath.isBlank()) {
            return "";
        }
        return checkAndJoin(subPath);
    }

    /**
     * 校验库存的图片相对路径（如用户头像 avatar 字段），用于入库前/删除前。
     *
     * @param relativePath 相对路径（“/”或“\”分隔均可）
     * @return 规范化后的相对路径（“/”分隔）
     * @throws IllegalArgumentException 路径非法或扩展名不在图片白名单
     */
    public static String validateImageRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        String normalized = checkAndJoin(relativePath);
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String ext = extensionOf(fileName);
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("文件类型不被允许");
        }
        return normalized;
    }

    /**
     * 在根目录下安全解析相对路径，规范化后必须仍位于根目录之内。
     *
     * @param root         根目录（上传根目录）
     * @param relativePath 相对路径，不可为空
     * @return 规范化后的目标路径（绝对路径）
     * @throws IllegalArgumentException 相对路径为空或越出根目录
     */
    public static Path resolveUnderRoot(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("相对路径不能为空");
        }
        String normalized = checkAndJoin(relativePath);
        Path rootNorm = root.toAbsolutePath().normalize();
        Path target = rootNorm.resolve(normalized).normalize();
        if (!target.startsWith(rootNorm)) {
            throw new IllegalArgumentException("非法路径：禁止访问上传目录之外的文件");
        }
        return target;
    }

    /**
     * 剥离客户端原始文件名中可能携带的路径（旧浏览器可能传完整路径），只保留最后一段。
     */
    public static String stripClientPath(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int slash = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String name = slash >= 0 ? originalFilename.substring(slash + 1) : originalFilename;
        if (name.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return name;
    }

    /**
     * 提取小写扩展名，要求文件必须有主名和非空扩展名。
     */
    public static String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0 || idx == fileName.length() - 1) {
            throw new IllegalArgumentException("文件名缺少合法扩展名");
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

    /**
     * 扩展名白名单校验并归一化（jpeg 归一为 jpg）。
     */
    public static String normalizeImageExt(String ext) {
        if (ext == null || !ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅允许上传 jpg/jpeg/png/gif/webp/bmp 图片文件");
        }
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    /**
     * 依据文件头魔数识别真实图片类型，防止改扩展名绕过白名单。
     *
     * @param bytes 文件内容（至少前 12 字节）
     * @return 真实类型扩展名（jpeg 归一为 jpg），无法识别返回 null
     */
    public static String detectImageExt(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (b0 == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        // GIF: GIF87a / GIF89a
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "gif";
        }
        // BMP: BM
        if (bytes[0] == 'B' && bytes[1] == 'M') {
            return "bmp";
        }
        // WEBP: "RIFF" .... "WEBP"
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /**
     * 按“原文件名_年月日时分秒毫秒.ext”生成服务端存储文件名。
     * 原始主名仍需通过路径段合法性校验（拒绝非法字符/设备名/控制字符）并截断超长部分，
     * 即保留可读原名，又不放弃安全约束。
     *
     * @param strippedFilename 已剥离客户端路径的原始文件名（必须含合法扩展名）
     * @param ext              已归一化并经魔数确认的扩展名
     * @return 存储文件名，如 头像_20260917143025123.png
     */
    public static String buildTimestampedStoredName(String strippedFilename, String ext) {
        return buildTimestampedStoredName(strippedFilename, ext, "");
    }

    /**
     * 同 {@link #buildTimestampedStoredName(String, String)}，extraSuffix 非空时追加在时间戳之后，
     * 用于同一毫秒内同名上传的冲突避让。
     */
    public static String buildTimestampedStoredName(String strippedFilename, String ext, String extraSuffix) {
        int dotIdx = strippedFilename.lastIndexOf('.');
        String baseName = dotIdx > 0 ? strippedFilename.substring(0, dotIdx) : "";
        if (baseName.isBlank()) {
            throw new IllegalArgumentException("文件名主名不能为空");
        }
        //纵深防御：主名中不允许任何路径分隔符（调用方应已用 stripClientPath 剥离）
        if (baseName.indexOf('/') >= 0 || baseName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("文件名包含非法字符");
        }
        //为 “_17位时间戳_可选后缀.ext” 预留长度，保证最终路径段不超过 MAX_SEGMENT_LENGTH
        int reserve = 1 + 17 + (extraSuffix.isEmpty() ? 0 : extraSuffix.length() + 1) + 1 + ext.length();
        if (baseName.length() + reserve > MAX_SEGMENT_LENGTH) {
            baseName = baseName.substring(0, Math.max(1, MAX_SEGMENT_LENGTH - reserve));
        }
        //主名按路径段规则校验（允许中文、空格、多个点，拒绝非法字符与保留设备名）
        checkSegment(baseName);
        String suffix = extraSuffix.isEmpty() ? "" : "_" + extraSuffix;
        return baseName + "_" + LocalDateTime.now().format(STORED_NAME_TIMESTAMP) + suffix + "." + ext;
    }

    /**
     * 生成短随机后缀（同名同毫秒冲突避让）
     */
    public static String shortHexSuffix() {
        return Integer.toHexString(SECURE_RANDOM.nextInt(0xFFFF) | 0x10000);
    }

    /**
     * 逐段校验并以“/”重新拼接
     */
    private static String checkAndJoin(String rawPath) {
        if (rawPath.length() > MAX_RELATIVE_PATH_LENGTH) {
            throw new IllegalArgumentException("路径长度超出限制");
        }
        // 同时兼容“/”与“\”（Windows 历史库存路径）
        String[] segments = rawPath.split("[/\\\\]", -1);
        for (String segment : segments) {
            checkSegment(segment);
        }
        return String.join("/", segments);
    }

    /**
     * 单个路径段（目录名/文件名）合法性校验
     */
    private static void checkSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            throw new IllegalArgumentException("路径中存在空段");
        }
        if (segment.length() > MAX_SEGMENT_LENGTH) {
            throw new IllegalArgumentException("路径段长度超出限制");
        }
        if (".".equals(segment) || "..".equals(segment)) {
            throw new IllegalArgumentException("路径中禁止出现当前目录或上级目录跳转");
        }
        if (segment.endsWith(" ") || segment.endsWith(".")) {
            throw new IllegalArgumentException("路径段不允许以空格或点结尾");
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c <= 31 || c == 127 || FORBIDDEN_CHARS.indexOf(c) >= 0) {
                throw new IllegalArgumentException("路径包含非法字符");
            }
        }
        // 任意一个“.”分隔部分命中 Windows 保留设备名即拒绝（如 con.jpg）
        String[] dotParts = segment.split("\\.");
        for (String part : dotParts) {
            if (WINDOWS_RESERVED_NAMES.contains(part.toLowerCase())) {
                throw new IllegalArgumentException("路径包含系统保留名称");
            }
        }
    }
}
