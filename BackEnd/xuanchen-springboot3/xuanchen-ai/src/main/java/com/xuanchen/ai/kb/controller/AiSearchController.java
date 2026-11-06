package com.xuanchen.ai.kb.controller;

import com.xuanchen.ai.kb.rag.AiSearchService;
import com.xuanchen.common.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 控制器-->AI向量检索测试（知识库调参用；P2 问答链路服务端复用 AiSearchService）
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@RestController
@RequestMapping("/ai/kb")
public class AiSearchController {

    @Autowired
    private AiSearchService searchService;

    /**
     * 检索测试：返回 topK 命中块与相似度分数
     */
    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestBody SearchDTO dto) {
        if (dto.getQuery() == null || dto.getQuery().isBlank()) {
            return Result.badRequest("检索内容不能为空！");
        }
        return Result.success(searchService.search(dto.getQuery(), dto.getKbIds()));
    }

    /**
     * 检索测试入参
     */
    public static class SearchDTO {
        private String query;
        private List<String> kbIds;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<String> getKbIds() {
            return kbIds;
        }

        public void setKbIds(List<String> kbIds) {
            this.kbIds = kbIds;
        }
    }
}
