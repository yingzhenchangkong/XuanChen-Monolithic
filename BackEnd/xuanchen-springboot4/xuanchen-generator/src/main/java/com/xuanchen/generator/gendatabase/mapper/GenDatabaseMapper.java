package com.xuanchen.generator.gendatabase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.generator.gendatabase.entity.GenDatabase;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->数据库
 *
 * @author XuanChen
 * @date 2026-02-23
 */
@Repository
public interface GenDatabaseMapper extends BaseMapper<GenDatabase> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param genDatabase
     * @return
     */
    List<GenDatabase> listRecycleBin(@Param("page") Page<GenDatabase> page, @Param("genDatabase") GenDatabase genDatabase);

    /**
     * 回收站 彻底删除
     *
     * @param genDatabaseIds
     * @return
     */
    Integer deleteRecycleBin(@Param("genDatabaseIds") String[] genDatabaseIds);

    /**
     * 回收站 还原
     *
     * @param genDatabaseIds
     * @return
     */
    Integer revertRecycleBin(@Param("genDatabaseIds") String[] genDatabaseIds);
}
