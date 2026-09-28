package com.xuanchen.generator.generator.security;

import com.xuanchen.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
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

    @PostConstruct
    void init() {
        Set<Path> roots = new LinkedHashSet<>();
        if (allowedOutputRootsRaw != null && !allowedOutputRootsRaw.isBlank()) {
            for (String item : allowedOutputRootsRaw.split(",")) {
                String root = item.trim();
                if (root.isEmpty()) {
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
     * 校验一份完整的代码生成输出配置（保存时与生成前都应调用）。
     *
     * @param outputDir  输出目录
     * @param moduleName 模块名（Java 包标识符）
     * @param packageName 包名（Java 包标识符）
     */
    public void validateOutputConfig(String outputDir, String moduleName, String packageName) {
        requireIdentifier(moduleName, "模块名");
        requireIdentifier(packageName, "包名");
        validateOutputDir(outputDir);
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
        Path target;
        try {
            target = Paths.get(outputDir).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new BusinessException(400, "输出目录包含非法字符");
        }
        assertWithinRoots(target);
        return target;
    }

    /**
     * 断言目标路径位于白名单根目录之内。用于对最终落盘路径（如 mapper xml 拼接路径）做二次确认。
     */
    public void assertWithinRoots(Path target) {
        if (allowedRoots.isEmpty()) {
            throw new BusinessException(500,
                    "代码生成输出根目录白名单未配置（xuanchen.generator.allowed-output-roots / "
                            + "XUANCHEN_GEN_OUTPUT_ROOTS），已拒绝生成，请联系管理员配置");
        }
        Path normalized = target.toAbsolutePath().normalize();
        for (Path root : allowedRoots) {
            // Path.startsWith 按路径段比较，天然规避 /safe 与 /safeEvil 的字符串前缀问题
            if (normalized.startsWith(root)) {
                return;
            }
        }
        throw new BusinessException(400,
                "输出目录越出允许的代码生成根目录范围，仅允许写入服务器配置的白名单目录");
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
