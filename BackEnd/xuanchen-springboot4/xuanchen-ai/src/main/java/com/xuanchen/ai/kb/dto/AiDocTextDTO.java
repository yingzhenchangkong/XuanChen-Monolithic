package com.xuanchen.ai.kb.dto;

import lombok.Data;

/**
 * DTO-->AI知识库手动录入文本
 *
 * @author XuanChen
 * @date 2026-10-30
 */
@Data
public class AiDocTextDTO {

    /** 所属知识库id */
    private String kbId;

    /** 知识标题（入库后作为文件名/文档名） */
    private String title;

    /** 知识正文（纯文本/Markdown） */
    private String content;
}
