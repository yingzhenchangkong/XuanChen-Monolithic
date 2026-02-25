package com.xuanchen.generator.gendatabase.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;
import com.xuanchen.generator.gendatabase.mapper.GenDatabaseMapper;
import com.xuanchen.generator.gendatabase.service.IGenDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->数据库
 *
 * @author XuanChen
 * @date 2026-02-23
 */
@Service
public class GenDatabaseServiceImpl extends ServiceImpl<GenDatabaseMapper, GenDatabase> implements IGenDatabaseService {
    @Autowired
    private GenDatabaseMapper genDatabaseMapper;

    @Override
    public IPage<GenDatabase> listRecycleBin(Page<GenDatabase> page, GenDatabase genDatabase) {
        return page.setRecords(genDatabaseMapper.listRecycleBin(page, genDatabase));
    }

    @Override
    public void deleteRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        genDatabaseMapper.deleteRecycleBin(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        genDatabaseMapper.revertRecycleBin(idArray);
    }
}
