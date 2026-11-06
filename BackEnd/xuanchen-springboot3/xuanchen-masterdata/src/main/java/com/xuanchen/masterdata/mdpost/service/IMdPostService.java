package com.xuanchen.masterdata.mdpost.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.masterdata.mdpost.entity.MdPost;

/**
 * Service接口-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
public interface IMdPostService extends IService<MdPost> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param mdPost
     * @return
     */
    IPage<MdPost> listRecycleBin(Page<MdPost> page, MdPost mdPost);

    /**
     * 回收站 彻底删除
     *
     * @param ids
     */
    void deleteRecycleBin(String ids);

    /**
     * 回收站 还原
     *
     * @param ids
     */
    void revertRecycleBin(String ids);

    Boolean ifExistsId(MdPost mdPost);

    Boolean ifExistsNoId(MdPost mdPost);
}
