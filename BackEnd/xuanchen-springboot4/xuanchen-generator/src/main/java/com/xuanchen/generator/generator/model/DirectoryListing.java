package com.xuanchen.generator.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录选择器返回结构：当前目录 + 可选根目录 + 子目录列表。
 *
 * @author XuanChen
 * @date 2026-10-18
 */
@Data
public class DirectoryListing implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前目录绝对路径（首次打开为空，表示还没进入任何目录） */
    private String path;
    /** 上一级目录；已在根目录时为 null */
    private String parent;
    /** 当前目录是否可写（不可写时生成会失败，前端提示换目录） */
    private boolean writable;
    /** 是否启用了白名单限制（true 时只能在 roots 之下浏览） */
    private boolean restricted;
    /** 可快速跳转的根目录 */
    private List<RootItem> roots = new ArrayList<>();
    /** 建议的默认目录（新建配置时预填，避免直接写进盘符根） */
    private String defaultPath;
    /** 子目录 */
    private List<DirItem> dirs = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String path;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String path;
        private boolean writable;
    }
}
