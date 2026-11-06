package com.xuanchen.ai.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.ai.kb.entity.AiDocument;
import com.xuanchen.ai.kb.entity.AiKb;
import com.xuanchen.ai.kb.mapper.AiDocumentMapper;
import com.xuanchen.ai.kb.mapper.AiKbMapper;
import com.xuanchen.ai.kb.rag.AiIngestService;
import com.xuanchen.ai.kb.service.IAiKbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service接口实现类-->AI知识库
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Service
public class AiKbServiceImpl extends ServiceImpl<AiKbMapper, AiKb> implements IAiKbService {

    @Autowired
    private AiDocumentMapper documentMapper;
    @Autowired
    private AiIngestService ingestService;

    @Override
    public IPage<AiKb> listKb(Page<AiKb> page, AiKb kb) {
        QueryWrapper<AiKb> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(kb.getKbName() != null && !kb.getKbName().isBlank(), "kb_name", kb.getKbName());
        queryWrapper.eq(kb.getStatus() != null, "status", kb.getStatus());
        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public boolean ifExistsName(AiKb kb) {
        QueryWrapper<AiKb> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("kb_name", kb.getKbName());
        queryWrapper.ne(kb.getId() != null && !kb.getId().isBlank(), "id", kb.getId());
        return this.count(queryWrapper) > 0;
    }

    @Override
    public IPage<AiKb> listRecycleBin(Page<AiKb> page, AiKb kb) {
        return page.setRecords(baseMapper.listRecycleBin(page, kb));
    }

    @Override
    public void revertRecycleBin(String ids) {
        baseMapper.revertRecycleBin(Arrays.asList(ids.split(",")));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        for (String id : idList) {
            // 级联清理：该库下文档的向量（物理）与文档行（物理），最后物理删知识库行
            // （文档行 del_flag 仍为 0，selectList 可正常查出）
            for (AiDocument doc : documentMapper.selectList(
                    new QueryWrapper<AiDocument>().eq("kb_id", id).select("id"))) {
                ingestService.removeVectors(doc.getId());
            }
            documentMapper.hardDeleteByKbId(id);
        }
        baseMapper.deleteRecycleBin(idList);
    }
}

