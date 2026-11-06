package com.xuanchen.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.ai.chat.entity.AiSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 问答会话 Mapper
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {
}
