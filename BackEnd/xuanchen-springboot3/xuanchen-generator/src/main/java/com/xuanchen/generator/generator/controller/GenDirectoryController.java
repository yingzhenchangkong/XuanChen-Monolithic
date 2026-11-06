package com.xuanchen.generator.generator.controller;

import com.xuanchen.common.entity.Result;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.generator.model.DirectoryListing;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 控制器 --> 代码生成输出目录选择。
 * <p>
 * 代码生成在服务器本地落盘，输出目录由管理员在目录选择器里挑选（不再手工输入路径、也不需要预先配置白名单）。
 * 若配置了 {@code xuanchen.generator.allowed-output-roots}，浏览范围与写入范围都会被限制在该白名单之内。
 * 所有接口仅 admin 可用。
 *
 * @author XuanChen
 * @date 2026-10-18
 */
@RestController
@RequestMapping("/tool/generator/directory")
@RequiresRoles("admin")
public class GenDirectoryController {

    /** 单层最多返回的子目录数，避免超大目录拖慢前端 */
    private static final int MAX_DIRS = 500;

    /** 新建文件夹名称：单段目录名，禁止路径分隔符与 Windows 非法字符 */
    private static final String INVALID_NAME_CHARS = "[\\\\/:*?\"<>|]";

    @Resource
    private GeneratorSafetyValidator generatorSafetyValidator;

    /**
     * 列出目录内容。
     *
     * @param path 目标目录绝对路径；为空时只返回可跳转的根目录（前端首次打开用）
     * @return 当前目录、上一级、子目录列表、是否可写
     */
    @GetMapping("/list")
    public Result<DirectoryListing> list(@RequestParam(name = "path", required = false) String path) {
        DirectoryListing listing = new DirectoryListing();
        listing.setRestricted(generatorSafetyValidator.isRestricted());
        for (Path root : generatorSafetyValidator.getBrowseRoots()) {
            listing.getRoots().add(new DirectoryListing.RootItem(displayName(root), root.toString()));
        }
        if (path == null || path.isBlank()) {
            // 未指定目录：只返回根列表与建议默认目录，由前端默认进入建议目录
            listing.setPath("");
            listing.setDefaultPath(resolveDefaultPath(listing.getRoots()));
            return Result.success(listing);
        }

        Path current = generatorSafetyValidator.resolveExistingDir(path);
        listing.setPath(current.toString());
        listing.setWritable(Files.isWritable(current));

        Path parent = current.getParent();
        // 盘符根目录与白名单根都不再提供"上一级"，避免跳出允许范围（跳出去也会被后端拒绝）
        boolean topmost = parent == null || isRoot(current)
                || generatorSafetyValidator.getBrowseRoots().contains(current);
        if (!topmost) {
            listing.setParent(parent.toString());
        }

        try (Stream<Path> stream = Files.list(current)) {
            List<DirectoryListing.DirItem> dirs = stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .limit(MAX_DIRS)
                    .map(p -> new DirectoryListing.DirItem(p.getFileName().toString(), p.toString(), Files.isWritable(p)))
                    .toList();
            listing.setDirs(dirs);
        } catch (IOException e) {
            throw new BusinessException(500, "读取目录失败：" + e.getMessage());
        }
        return Result.success(listing);
    }

    /**
     * 在指定目录下新建文件夹（生成产物目录通常还没有创建）。
     *
     * @param body parent=父目录绝对路径，name=新目录名
     * @return 新目录绝对路径
     */
    @PostMapping("/mkdir")
    public Result<String> mkdir(@RequestBody Map<String, String> body) {
        Path parent = generatorSafetyValidator.resolveExistingDir(body.get("parent"));
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "请输入文件夹名称");
        }
        name = name.trim();
        if (name.length() > 64) {
            throw new BusinessException(400, "文件夹名称最长 64 个字符");
        }
        if (".".equals(name) || "..".equals(name) || name.contains("..")
                || name.matches(".*" + INVALID_NAME_CHARS + ".*")
                || name.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            throw new BusinessException(400, "文件夹名称不能包含 \\ / : * ? \" < > | 或 ..");
        }
        Path target = parent.resolve(name).normalize();
        // 单段名字理论上不会逃逸，仍按白名单复核一次（纵深防御）
        generatorSafetyValidator.assertWithinRoots(target);
        if (!parent.equals(target.getParent())) {
            throw new BusinessException(400, "文件夹名称不合法");
        }
        if (Files.exists(target)) {
            throw new BusinessException(400, "同名文件或文件夹已存在：" + target);
        }
        try {
            Files.createDirectory(target);
        } catch (IOException e) {
            throw new BusinessException(500, "创建文件夹失败：" + e.getMessage());
        }
        // 注意：Result.success(字符串) 会命中 success(String msg) 重载，把路径当成提示语返回、data 为 null，
        // 前端拿不到 data 就不弹成功提示也不刷新列表；必须显式 (msg, data) 两参形式。
        return Result.success("创建成功", target.toString());
    }

    private boolean isRoot(Path path) {
        return path.getParent() == null || path.equals(path.getRoot());
    }

    /**
     * 建议默认目录：限制模式下取白名单第一个根，否则取当前用户主目录（避免默认落到盘符根 C:\ 下）。
     */
    private String resolveDefaultPath(List<DirectoryListing.RootItem> roots) {
        if (!generatorSafetyValidator.isRestricted()) {
            String home = System.getProperty("user.home");
            if (home != null && !home.isBlank()) {
                Path homePath = Paths.get(home).toAbsolutePath().normalize();
                if (Files.isDirectory(homePath)) {
                    return homePath.toString();
                }
            }
        }
        return roots.isEmpty() ? null : roots.get(0).getPath();
    }

    private String displayName(Path root) {
        Path fileName = root.getFileName();
        return fileName == null ? root.toString() : fileName.toString();
    }
}
