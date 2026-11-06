package com.xuanchen.ai.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.ai.kb.entity.AiDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper接口-->AI知识库文档
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Mapper
public interface AiDocumentMapper extends BaseMapper<AiDocument> {

    /**
     * 按知识库物理删除文档行（回收站彻底删除知识库时级联清理；
     * 实体 del_flag 带 @TableLogic，MP 的 remove 无法真删，需手写 SQL）
     */
    @Delete("DELETE FROM ai_document WHERE kb_id = #{kbId}")
    int hardDeleteByKbId(@Param("kbId") String kbId);
}
