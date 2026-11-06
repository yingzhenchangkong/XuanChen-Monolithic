package com.xuanchen.ai.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.ai.kb.entity.AiKb;

/**
 * Service接口-->AI知识库
 *
 * @author XuanChen
 * @date 2026-10-29
 */
public interface IAiKbService extends IService<AiKb> {

    /**
     * 分页查询知识库列表（按名称模糊、状态过滤）
     */
    IPage<AiKb> listKb(Page<AiKb> page, AiKb kb);

    /**
     * 名称是否已存在（编辑时排除自身）
     */
    boolean ifExistsName(AiKb kb);

    /**
     * 回收站分页（del_flag=1）
     */
    IPage<AiKb> listRecycleBin(Page<AiKb> page, AiKb kb);

    /**
     * 回收站还原（del_flag 置回 0，逗号分隔多个 id）
     */
    void revertRecycleBin(String ids);

    /**
     * 回收站彻底删除（物理删除 KB 行，并级联删除其下文档行与向量，逗号分隔多个 id）
     */
    void deleteRecycleBin(String ids);
}
