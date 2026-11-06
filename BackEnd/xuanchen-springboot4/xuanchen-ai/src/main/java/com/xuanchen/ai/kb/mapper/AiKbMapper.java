package com.xuanchen.ai.kb.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.ai.kb.entity.AiKb;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper接口-->AI知识库
 * <p>回收站三个方法走 XML 手写 SQL：实体 del_flag 带 @TableLogic，
 * MP 自动注入 del_flag=0 条件，无法用 QueryWrapper 查询/操作已删除行。</p>
 *
 * @author XuanChen
 * @date 2026-10-29
 */
@Mapper
public interface AiKbMapper extends BaseMapper<AiKb> {

    /**
     * 回收站分页（del_flag=1；MP 分页插件拦截填充 page）
     */
    List<AiKb> listRecycleBin(IPage<AiKb> page, @Param("kb") AiKb kb);

    /**
     * 彻底删除（物理 DELETE）
     */
    int deleteRecycleBin(@Param("ids") List<String> ids);

    /**
     * 恢复（del_flag 置回 0）
     */
    int revertRecycleBin(@Param("ids") List<String> ids);
}

