package com.xuanchen.ai.kb.rag;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuanchen.ai.config.AiProperties;
import com.xuanchen.ai.kb.entity.AiDocument;
import com.xuanchen.ai.kb.service.IAiDocumentService;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.filemanage.storage.StorageService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识入库服务-->Tika 抽文本 → 递归分块 → bge-m3 向量化 → pgvector 落库。
 * <p>全链路在独立线程池异步执行，文档状态机：pending → parsing → ready/failed。</p>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Slf4j
@Service
public class AiIngestService {

    /**
     * 解析线程池（单实例 Tika 解析器非线程安全，池内同步执行恰好匹配）
     */
    private final ThreadPoolTaskExecutor ingestExecutor;
    private final AiProperties aiProperties;
    private final EmbeddingModel embeddingModel;
    private final PgVectorEmbeddingStore embeddingStore;
    private final IFileInfoService fileInfoService;
    private final StorageService storageService;
    private final IAiDocumentService documentService;

    private final ApacheTikaDocumentParser documentParser = new ApacheTikaDocumentParser();

    public AiIngestService(@Qualifier("aiIngestExecutor") ThreadPoolTaskExecutor ingestExecutor,
                           AiProperties aiProperties,
                           EmbeddingModel embeddingModel,
                           PgVectorEmbeddingStore embeddingStore,
                           IFileInfoService fileInfoService,
                           StorageService storageService,
                           IAiDocumentService documentService) {
        this.ingestExecutor = ingestExecutor;
        this.aiProperties = aiProperties;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.fileInfoService = fileInfoService;
        this.storageService = storageService;
        this.documentService = documentService;
    }

    /**
     * 提交异步解析（上传后/失败重试/重新解析均走此入口）。
     * <p>兜底：线程池 worker 内抛出 Error 时会被 ThreadPoolExecutor 静默杀死
     * （无日志、状态永久卡 parsing），故必须在此捕获 Throwable 落 failed。</p>
     */
    public void submitParse(String docId) {
        ingestExecutor.execute(() -> {
            try {
                parse(docId);
            } catch (Throwable t) {
                log.error("解析任务被 Throwable 终止 docId={}", docId, t);
                try {
                    AiDocument doc = documentService.getById(docId);
                    if (doc != null) {
                        doc.setStatus("failed");
                        doc.setErrorMsg(abbreviate("解析线程异常终止: " + t));
                        saveDocState(doc);
                    }
                } catch (Exception e) {
                    log.error("回写失败状态时出错 docId={}", docId, e);
                }
            }
        });
    }

    /**
     * 解析单文档：状态机推进，任何异常落 failed + error_msg，可重试
     */
    public void parse(String docId) {
        AiDocument doc = documentService.getById(docId);
        if (doc == null) {
            log.warn("待解析文档不存在 docId={}", docId);
            return;
        }
        doc.setStatus("parsing");
        doc.setErrorMsg(null);
        saveDocState(doc);
        try {
            FileInfo info = fileInfoService.getById(doc.getFileId());
            if (info == null) {
                throw new IllegalStateException("文件中心记录不存在（fileId=" + doc.getFileId() + "）");
            }
            // 1、Tika 抽取全文
            Document document;
            try (InputStream in = storageService.get(info.getBucket(), info.getObjectKey())) {
                document = documentParser.parse(in);
            }
            // 2、分块前注入元数据（检索过滤/引用溯源全靠它，分块自动继承）
            document.metadata()
                    .put("kb_id", doc.getKbId())
                    .put("doc_id", doc.getId())
                    .put("file_name", doc.getFileName() == null ? "" : doc.getFileName());
            // 3、递归分块（段落+句子边界，中文友好）
            List<TextSegment> segments = DocumentSplitters.recursive(
                    aiProperties.getRag().getChunkSize(), aiProperties.getRag().getChunkOverlap()).split(document);
            if (segments.isEmpty()) {
                throw new IllegalStateException("未解析出任何文本内容（可能是纯图片扫描件）");
            }
            // 4、清掉旧向量（支持重复解析/重试），再批量向量化落库
            embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("doc_id").isEqualTo(docId));
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            // 5、就绪
            doc.setStatus("ready");
            doc.setChunkCount(segments.size());
            doc.setErrorMsg(null);
            saveDocState(doc);
            log.info("文档解析完成 docId={} chunks={}", docId, segments.size());
        } catch (Exception e) {
            log.error("文档解析失败 docId={}", docId, e);
            doc.setStatus("failed");
            doc.setErrorMsg(abbreviate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            saveDocState(doc);
        }
    }

    /**
     * 状态回写：显式 set 全部关注列（updateById 会忽略 null 字段，导致 error_msg 清不掉）
     */
    private void saveDocState(AiDocument doc) {
        documentService.update(new LambdaUpdateWrapper<AiDocument>()
                .eq(AiDocument::getId, doc.getId())
                .set(AiDocument::getStatus, doc.getStatus())
                .set(AiDocument::getErrorMsg, doc.getErrorMsg())
                .set(AiDocument::getChunkCount, doc.getChunkCount())
                .set(AiDocument::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 删除文档时清理其在向量库中的全部分块
     */
    public void removeVectors(String docId) {
        try {
            embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("doc_id").isEqualTo(docId));
        } catch (Exception e) {
            // 向量清理失败不阻断业务删除，残留数据最多多占检索位，不产生错误引用
            log.error("清理文档向量失败 docId={}", docId, e);
        }
    }

    private static String abbreviate(String msg) {
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
