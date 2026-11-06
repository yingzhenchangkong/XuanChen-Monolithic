package com.xuanchen.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块配置项（xuanchen.ai.*）。
 * <p>chat/embed 走本地 Ollama；vector 指向独立 PostgreSQL(xuanchen-kb) 向量库。</p>
 */
@Data
@ConfigurationProperties(prefix = "xuanchen.ai")
public class AiProperties {

    private Chat chat = new Chat();
    private Embed embed = new Embed();
    private Vector vector = new Vector();
    private Rag rag = new Rag();

    /** 对话模型配置 */
    @Data
    public static class Chat {
        /** Ollama 服务地址 */
        private String baseUrl = "http://localhost:11434";
        /** 对话模型名（gemma4） */
        private String model = "gemma4";
        private Double temperature = 0.7d;
        /** 本地推理较慢，超时放宽（秒） */
        private Long timeoutSeconds = 300L;
    }

    /** 向量化模型配置 */
    @Data
    public static class Embed {
        private String baseUrl = "http://localhost:11434";
        /** 向量模型名（bge-m3，1024 维） */
        private String model = "bge-m3";
    }

    /** 向量库（PostgreSQL + pgvector）配置 */
    @Data
    public static class Vector {
        private String host = "localhost:5433";
        private String database = "xuanchen-kb";
        private String user = "postgres";
        private String password = "";
        /** 分块向量表（由 PgVectorEmbeddingStore 首次启动自动建表） */
        private String table = "ai_chunk";
        /** 向量维度，必须与 embed 模型输出一致 */
        private Integer dimension = 1024;
        /** 连接池最大连接数 */
        private Integer maxPoolSize = 5;
    }

    /** RAG 检索参数 */
    @Data
    public static class Rag {
        /** 检索条数 topK */
        private Integer maxResults = 5;
        /** 相似度下限（langchain4j 归一化分数 0~1） */
        private Double minScore = 0.6d;
        /** 分块大小（字符） */
        private Integer chunkSize = 500;
        /** 分块重叠（字符） */
        private Integer chunkOverlap = 50;
    }
}
