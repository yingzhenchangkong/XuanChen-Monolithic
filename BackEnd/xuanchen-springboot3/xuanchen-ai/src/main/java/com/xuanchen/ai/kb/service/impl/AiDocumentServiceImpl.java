package com.xuanchen.ai.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.ai.kb.entity.AiDocument;
import com.xuanchen.ai.kb.mapper.AiDocumentMapper;
import com.xuanchen.ai.kb.service.IAiDocumentService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->AI知识库文档
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Service
public class AiDocumentServiceImpl extends ServiceImpl<AiDocumentMapper, AiDocument> implements IAiDocumentService {

    @Override
    public IPage<AiDocument> listDoc(Page<AiDocument> page, AiDocument doc) {
        QueryWrapper<AiDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("kb_id", doc.getKbId());
        queryWrapper.like(doc.getFileName() != null && !doc.getFileName().isBlank(), "file_name", doc.getFileName());
        queryWrapper.eq(doc.getStatus() != null && !doc.getStatus().isBlank(), "status", doc.getStatus());
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }
}
