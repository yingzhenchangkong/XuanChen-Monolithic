package com.xuanchen.generator.generator.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 生成预览文件（不落盘到用户目录，仅在内存/临时目录中展示）。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
@Data
public class GenPreviewFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 相对生成根目录的路径（统一用 / 分隔） */
    private String path;
    /** 文件内容（文本） */
    private String content;
    /** 内容是否被截断（超过上限时只返回前 N 字符） */
    private boolean truncated;
}
