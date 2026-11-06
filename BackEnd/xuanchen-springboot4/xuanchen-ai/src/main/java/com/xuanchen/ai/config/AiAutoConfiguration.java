package com.xuanchen.ai.config;

import com.alibaba.druid.pool.DruidDataSource;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * AI 模块装配：Ollama 对话/流式/向量化模型 + pgvector 向量存取。
 * <p>两版（sb3/sb4）此配置类完全一致，均为手动声明 Bean，不依赖 langchain4j starter。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    /**
     * 向量库独立数据源（xuanchen-kb）。
     * <p>注意：不能注册为 {@link DataSource} 类型 Bean——主库 Druid 数据源是唯一的
     * DataSource 候选，MyBatis-Plus 按 type 注入，出现第二个候选会导致启动失败。
     * 因此用包装类型暴露，并在应用关闭时释放连接池。</p>
     */
    @Bean
    public AiVectorDataSource aiVectorDataSource(AiProperties props) {
        AiProperties.Vector v = props.getVector();
        DruidDataSource ds = new DruidDataSource();
        ds.setName("ai-vector-pg");
        // 显式 connectTimeout=5：PG 驱动默认单次建连最长 10s，maxWait 无法打断物理建连，
        // 不收敛的话「PG 不可用」会比预期多等一倍时间（实测：仅 maxWait=3s 仍耗时 10s）
        ds.setUrl("jdbc:postgresql://" + v.getHost() + "/" + v.getDatabase() + "?connectTimeout=5");
        ds.setUsername(v.getUser());
        ds.setPassword(v.getPassword());
        // 驱动名显式声明：Druid 依赖它判定 dbType，不靠 URL 推断
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaxActive(v.getMaxPoolSize());
        // 与原先 Hikari 的 minimumIdle=1 对齐；initialSize 预热少量连接，避免首个检索建连抖动
        ds.setInitialSize(Math.min(2, v.getMaxPoolSize()));
        ds.setMinIdle(1);
        // 🔴 Druid 默认 maxWait = -1（无限等待），PG 不可用会把调用线程永久挂住，
        //    必须显式设置，取值与原 Hikari connectionTimeout(10s) 对齐
        ds.setMaxWait(10_000);
        // 空闲校验：PG 语法固定 SELECT 1，配合 testWhileIdle 剔除失效连接
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);
        // 取用/归还时不再校验，省掉每次获取连接的额外往返；失效连接由 testWhileIdle 兜底
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        // 不启用 filters=stat/wall：向量检索 SQL 极长，统计采集与 SQL 防火墙只会增加开销。
        // 懒初始化（不显式调 init()）：向量库暂不可用不应阻塞应用启动，故障暴露在首次检索
        return new AiVectorDataSource(ds);
    }

    @Bean
    public ChatModel aiChatModel(AiProperties props) {
        AiProperties.Chat c = props.getChat();
        return OllamaChatModel.builder()
                .baseUrl(c.getBaseUrl())
                .modelName(c.getModel())
                .temperature(c.getTemperature())
                .timeout(Duration.ofSeconds(c.getTimeoutSeconds()))
                .build();
    }

    @Bean
    public StreamingChatModel aiStreamingChatModel(AiProperties props) {
        AiProperties.Chat c = props.getChat();
        return OllamaStreamingChatModel.builder()
                .baseUrl(c.getBaseUrl())
                .modelName(c.getModel())
                .temperature(c.getTemperature())
                .timeout(Duration.ofSeconds(c.getTimeoutSeconds()))
                .build();
    }

    @Bean
    public EmbeddingModel aiEmbeddingModel(AiProperties props) {
        AiProperties.Embed e = props.getEmbed();
        return OllamaEmbeddingModel.builder()
                .baseUrl(e.getBaseUrl())
                .modelName(e.getModel())
                // 显式超时：防止 Ollama 无响应时解析线程永久挂起（状态卡 parsing）
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * pgvector 向量存取：表结构与索引由 PgVectorEmbeddingStore 首次使用时自动创建，
     * 前提是目标库已启用 vector 扩展（部署脚本负责 CREATE EXTENSION vector）。
     */
    @Bean
    public PgVectorEmbeddingStore aiEmbeddingStore(AiVectorDataSource vectorDataSource, AiProperties props) {
        AiProperties.Vector v = props.getVector();
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(vectorDataSource.getDataSource())
                .table(v.getTable())
                .dimension(v.getDimension())
                .createTable(true)
                .build();
    }

    /**
     * 文档解析线程池：Tika 抽文本 + 本地向量化都是分钟级重活，
     * 独立小线程池避免占满 HTTP 线程；队列排队，前端轮询解析状态。
     */
    @Bean
    public ThreadPoolTaskExecutor aiIngestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-ingest-");
        executor.initialize();
        return executor;
    }
}
