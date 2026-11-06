package com.xuanchen.ai.kb.rag;

import com.xuanchen.ai.config.AiProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量检索服务-->问题向量化 + pgvector 相似检索（按知识库过滤、分数阈值）。
 * <p>P1 供检索测试页调参；P2 问答链路直接复用本服务。</p>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Service
public class AiSearchService {

    private final AiProperties aiProperties;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore embeddingStore;

    public AiSearchService(AiProperties aiProperties,
                           EmbeddingModel embeddingModel,
                           PgVectorEmbeddingStore embeddingStore) {
        this.aiProperties = aiProperties;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 相似检索
     *
     * @param query 用户问题
     * @param kbIds 允许命中的知识库 id 列表（空列表 = 全库检索）
     * @return 命中块列表：content/score/kbId/docId/fileName
     */
    public List<Map<String, Object>> search(String query, List<String> kbIds) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(aiProperties.getRag().getMaxResults())
                .minScore(aiProperties.getRag().getMinScore());
        if (kbIds != null && !kbIds.isEmpty()) {
            builder.filter(MetadataFilterBuilder.metadataKey("kb_id").isIn(kbIds));
        }
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(builder.build()).matches();

        List<Map<String, Object>> result = new ArrayList<>(matches.size());
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("content", segment.text());
            item.put("score", match.score());
            Map<String, Object> meta = segment.metadata().toMap();
            item.put("kbId", meta.get("kb_id"));
            item.put("docId", meta.get("doc_id"));
            item.put("fileName", meta.get("file_name"));
            result.add(item);
        }
        return result;
    }
}
