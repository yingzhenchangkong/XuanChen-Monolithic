package com.xuanchen.masterdata.mdpost.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.masterdata.mdpost.entity.MdPost;
import com.xuanchen.masterdata.mdpost.mapper.MdPostMapper;
import com.xuanchen.masterdata.mdpost.service.IMdPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->岗位
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
@RequiredArgsConstructor
public class MdPostServiceImpl extends ServiceImpl<MdPostMapper, MdPost> implements IMdPostService {
    private final MdPostMapper mdPostMapper;

    @Override
    public IPage<MdPost> listRecycleBin(Page<MdPost> page, MdPost mdPost) {
        return page.setRecords(mdPostMapper.listRecycleBin(page, mdPost));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        mdPostMapper.deleteRecycleBin(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        mdPostMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean ifExistsId(MdPost mdPost) {
        return mdPostMapper.ifExistsId(mdPost).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(MdPost mdPost) {
        return mdPostMapper.ifExistsNoId(mdPost).size() > 0;
    }
}
