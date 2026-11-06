package com.xuanchen.ai.kb.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO-->AI知识库从文件中心导入已有文件（不重复上传，按 file_info.id 直接建关联）
 *
 * @author XuanChen
 * @date 2026-10-30
 */
@Data
public class AiDocImportDTO {

    /** 所属知识库id */
    private String kbId;

    /** 文件中心文件 id 列表（file_info.id，可批量） */
    private List<String> fileIds;
}
