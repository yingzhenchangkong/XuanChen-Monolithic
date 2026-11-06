package com.xuanchen.ai.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.ai.kb.entity.AiDocument;

/**
 * Service接口-->AI知识库文档
 *
 * @author XuanChen
 * @date 2026-10-29
 */
public interface IAiDocumentService extends IService<AiDocument> {

    /**
     * 分页查询知识库下的文档列表
     */
    IPage<AiDocument> listDoc(Page<AiDocument> page, AiDocument doc);
}
