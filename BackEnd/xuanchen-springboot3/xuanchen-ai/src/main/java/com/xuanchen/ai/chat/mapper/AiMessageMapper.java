package com.xuanchen.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.ai.chat.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 问答消息 Mapper
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
