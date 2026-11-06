package com.xuanchen.generator.generator.security;

import com.xuanchen.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 代码生成器安全校验器。
 * <p>
 * 代码生成会在服务器本地落盘，输出目录、模块名、包名均曾直接来自前端：
 * moduleName/packageName 传入 {@code ../../../} 即可路径穿越覆盖服务器任意文件。
 * 本组件统一收敛三类校验，保存配置与实际生成两处都必须调用（纵深防御，防止绕过保存入口的存量脏数据）：
 * <ol>
 *     <li>输出目录必须位于配置的白名单根目录之内（绝对化 + normalize + Path 级 startsWith，
 *     按路径段比较，杜绝 /safe 与 /safeEvil 的前缀绕过）；</li>
 *     <li>模块名/包名必须是合法 Java 包标识符（白名单正则，禁止任何路径分隔符与 ".."）；</li>
 *     <li>表名必须是合法数据库标识符，防止攻击者自建数据源时用畸形表名注入生成路径/模板。</li>
 * </ol>
 *
 * @author XuanChen
 * @date 2026-09-24
 */
@Component
public class GeneratorSafetyValidator {

    /**
     * Java 包名：一个或多个以 "." 分隔的标识符段（如 system、system.user）
     */
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    /**
     * 数据库表名：MySQL 未加引号的合法标识符（最长 64）
     */
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

    private static final int MAX_PACKAGE_LENGTH = 128;
    private static final int MAX_OUTPUT_DIR_LENGTH = 500;

    /**
     * 允许的代码输出根目录白名单（逗号分隔，必须是绝对路径）
     */
    @Value("${xuanchen.generator.allowed-output-roots:}")
    private String allowedOutputRootsRaw;

    /**
     * 是否允许覆盖已生成文件，默认关闭（关闭时 MyBatis-Plus 遇到已存在文件直接跳过）
     */
    @Value("${xuanchen.generator.allow-file-override:false}")
    private boolean allowFileOverride;

    private Set<Path> allowedRoots = Set.of();

    /**
     * 白名单是否被显式放开为"任意目录"（配置 {@code *}）。
     * <p>
     * 仅供内网/本机开发等可控环境使用：配置该值后输出目录可写到服务器任意位置，
     * 启动时会打印告警，生产环境不应使用。
     */
    private boolean allowAnyRoot;

    @PostConstruct
    void init() {
        Set<Path> roots = new LinkedHashSet<>();
        if (allowedOutputRootsRaw != null && !allowedOutputRootsRaw.isBlank()) {
            for (String item : allowedOutputRootsRaw.split(",")) {
                String root = item.trim();
                if (root.isEmpty()) {
                    continue;
                }
                // "*" = 显式放开为任意目录（escape hatch）
                if ("*".equals(root)) {
                    allowAnyRoot = true;
                    continue;
                }
                roots.add(Paths.get(root).toAbsolutePath().normalize());
            }
        }
        this.allowedRoots = roots;
    }

    public boolean isFileOverrideAllowed() {
        return allowFileOverride;
    }

    /**
     * 是否启用了输出根目录限制。
     * <p>
     * 未配置 {@code xuanchen.generator.allowed-output-roots}（或配置为 {@code *}）时不限制：
     * 目录由管理员通过目录选择器挑选，接口本身只对 admin 开放。生产环境建议显式配置白名单，
     * 一旦配置即只允许写入选定根目录之下的路径。
     */
    public boolean isRestricted() {
        return !allowAnyRoot && !allowedRoots.isEmpty();
    }

    /**
     * 目录选择器的可选根目录：已配置白名单时返回白名单，否则返回文件系统根（Windows 为各盘符）。
     */
    public List<Path> getBrowseRoots() {
        if (isRestricted()) {
            return new ArrayList<>(allowedRoots);
        }
        List<Path> roots = new ArrayList<>();
        java.io.File[] systemRoots = java.io.File.listRoots();
        if (systemRoots != null && systemRoots.length > 0) {
            for (java.io.File root : systemRoots) {
                roots.add(root.toPath().toAbsolutePath().normalize());
            }
        } else {
            roots.add(Paths.get("/").toAbsolutePath().normalize());
        }
        return roots;
    }

    /**
     * 把目录选择器传入的路径解析为可浏览/可写入的目录：绝对化 + normalize + 白名单（若启用）+ 必须是已存在目录。
     */
    public Path resolveExistingDir(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException(400, "缺少目录路径");
        }
        if (path.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            throw new BusinessException(400, "目录路径包含控制字符");
        }
        Path target;
        try {
            target = Paths.get(path).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new BusinessException(400, "目录路径包含非法字符");
        }
        assertWithinRoots(target);
        if (!Files.isDirectory(target)) {
            throw new BusinessException(400, "目录不存在或不是一个文件夹：" + target);
        }
        return target;
    }

    /**
     * 校验一份完整的代码生成输出配置（保存时与生成前都应调用）。
     *
     * @param outputDir  输出目录
     * @param moduleName 模块名（Java 包标识符）
     * @param packageName 包名（Java 包标识符）
     */
    public void validateOutputConfig(String outputDir, String moduleName, String packageName) {
        validateIdentifiers(moduleName, packageName);
        validateOutputDir(outputDir);
    }

    /**
     * 仅校验模块名/包名（zip 打包下载不写用户配置的输出目录，但模块名/包名仍参与落盘路径拼接，
     * 必须校验，否则 {@code ../../} 可让临时目录内的落盘路径逃逸）。
     */
    public void validateIdentifiers(String moduleName, String packageName) {
        requireIdentifier(moduleName, "模块名");
        requireIdentifier(packageName, "包名");
    }

    /**
     * 在白名单根目录内创建一个本次生成专用的临时目录（zip 打包下载用）。
     * <p>
     * 不能直接用 {@link java.nio.file.Files#createTempDirectory}：系统临时目录通常不在白名单内，
     * 后续落盘前的 containment 断言会必然失败。这里把临时目录建在白名单根下的
     * {@code .codegen-tmp/gen-<uuid>}，生成完由调用方整体删除。
     */
    public Path createTempGenerationRoot() {
        if (!isRestricted()) {
            try {
                return Files.createTempDirectory("xuanchen-codegen-");
            } catch (IOException e) {
                throw new BusinessException(500, "创建代码生成临时目录失败：" + e.getMessage());
            }
        }
        Path base = allowedRoots.iterator().next().resolve(".codegen-tmp");
        try {
            Files.createDirectories(base);
            return Files.createDirectory(base.resolve("gen-" + UUID.randomUUID().toString().replace("-", "")));
        } catch (IOException e) {
            throw new BusinessException(500, "创建代码生成临时目录失败：" + e.getMessage());
        }
    }

    /**
     * 校验输出目录：非空、可解析为合法路径、规范化后必须位于任一白名单根目录之内。
     */
    public Path validateOutputDir(String outputDir) {
        if (outputDir == null || outputDir.isBlank()) {
            throw new BusinessException(400, "输出目录不能为空");
        }
        if (outputDir.length() > MAX_OUTPUT_DIR_LENGTH) {
            throw new BusinessException(400, "输出目录长度超出限制");
        }
        if (outputDir.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            throw new BusinessException(400, "输出目录包含控制字符，请重新填写");
        }
        Path target;
        try {
            target = Paths.get(outputDir).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new BusinessException(400, "输出目录包含非法字符");
        }
        // 启用白名单时用真实路径再校验一次：目录内若有指向外部的符号链接，字符串前缀会绕过限制
        if (isRestricted() && Files.exists(target)) {
            try {
                assertWithinRoots(target.toRealPath());
            } catch (IOException e) {
                throw new BusinessException(400, "输出目录无法解析：" + e.getMessage());
            }
        } else {
            assertWithinRoots(target);
        }
        return target;
    }

    /**
     * 断言目标路径位于白名单根目录之内。用于对最终落盘路径（如 mapper xml 拼接路径）做二次确认。
     */
    public void assertWithinRoots(Path target) {
        // 未配置白名单 = 不限制（目录由管理员在目录选择器中挑选，接口仅 admin 可用）
        if (!isRestricted()) {
            return;
        }
        Path normalized = target.toAbsolutePath().normalize();
        for (Path root : allowedRoots) {
            // Path.startsWith 按路径段比较，天然规避 /safe 与 /safeEvil 的字符串前缀问题
            if (normalized.startsWith(root)) {
                return;
            }
        }
        // 报错带上白名单实际取值：管理员据此可直接填出合法目录（该接口仅管理员可用）
        StringBuilder roots = new StringBuilder();
        for (Path root : allowedRoots) {
            roots.append(roots.isEmpty() ? "" : "、").append(root);
        }
        throw new BusinessException(400,
                "输出目录越出允许的代码生成根目录范围，仅允许写入服务器配置的白名单目录。"
                        + "当前解析到的输出目录：" + normalized + "；已配置白名单根目录：" + roots);
    }

    /**
     * 校验表名为合法数据库标识符。
     */
    public void validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank() || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new BusinessException(400,
                    "表名仅允许字母、数字、下划线，且必须以字母或下划线开头（最长 64 位）");
        }
    }

    /**
     * 校验数据库标识符（列名/子表名/外键名等），报错文案可自定义。
     */
    public void validateDbIdentifier(String value, String displayName) {
        if (value == null || value.isBlank() || !TABLE_NAME_PATTERN.matcher(value).matches()) {
            throw new BusinessException(400,
                    displayName + "仅允许字母、数字、下划线，且必须以字母或下划线开头（最长 64 位）");
        }
    }

    /**
     * 校验「相对输出路径」：必须是输出目录之下的相对路径，禁止绝对路径、盘符、"." / ".." 段与控制字符。
     * <p>
     * 该路径参与落盘目录拼接（如指向仓库的 src/main/java），必须在保存时就拦住路径穿越。
     *
     * @param relativePath 相对路径（可空，空表示使用默认布局）
     * @param displayName  字段中文名（用于报错）
     */
    public void validateRelativeOutputPath(String relativePath, String displayName) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        String value = relativePath.trim();
        if (value.length() > MAX_OUTPUT_DIR_LENGTH) {
            throw new BusinessException(400, displayName + "长度超出限制");
        }
        if (value.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            throw new BusinessException(400, displayName + "包含控制字符");
        }
        String unified = value.replace('\\', '/');
        if (unified.startsWith("/") || unified.matches("^[A-Za-z]:.*")) {
            throw new BusinessException(400, displayName + "必须是相对输出目录的路径，不能是绝对路径或盘符路径");
        }
        for (String segment : unified.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new BusinessException(400, displayName + "不能包含空段、\".\" 或 \"..\"");
            }
        }
    }

    /**
     * 把相对路径解析到指定根目录之下，并做一次 containment 断言（防止保存后被篡改）。
     *
     * @param root         根目录（已绝对化）
     * @param relativePath 相对路径（可空）
     * @return 解析后的绝对路径
     */
    public Path resolveUnderRoot(Path root, String relativePath) {
        Path base = root.toAbsolutePath().normalize();
        if (relativePath == null || relativePath.isBlank()) {
            return base;
        }
        Path target = base.resolve(relativePath.trim()).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessException(400, "相对输出路径越出输出目录范围：" + relativePath);
        }
        assertWithinRoots(target);
        return target;
    }

    /**
     * 校验可选的 Java 包标识符（子表名/外键名等可空字段），空白视为未填写直接放行。
     */
    public void validateOptionalIdentifier(String value, String displayName) {
        if (value == null || value.isBlank()) {
            return;
        }
        requireIdentifier(value, displayName);
    }

    private void requireIdentifier(String value, String displayName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, displayName + "不能为空");
        }
        if (value.length() > MAX_PACKAGE_LENGTH
                || !PACKAGE_PATTERN.matcher(value).matches()) {
            throw new BusinessException(400,
                    displayName + "仅允许字母、数字、下划线，多段以英文句点分隔（如 system.user），"
                            + "禁止路径分隔符与 \"..\"");
        }
    }
}
