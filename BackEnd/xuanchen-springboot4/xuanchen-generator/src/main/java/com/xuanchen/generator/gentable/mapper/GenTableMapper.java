package com.xuanchen.generator.gentable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.generator.gentable.entity.GenTable;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Repository
public interface GenTableMapper extends BaseMapper<GenTable> {

    /**
     * 回收站 列表
     *
     * @param page     分页参数
     * @param genTable 查询条件（表名/表注释）
     * @return 已逻辑删除的记录
     */
    List<GenTable> listRecycleBin(@Param("page") Page<GenTable> page, @Param("genTable") GenTable genTable);

    /**
     * 回收站 彻底删除
     *
     * @param genTableIds 主键数组
     * @return 影响行数
     */
    Integer deleteRecycleBin(@Param("genTableIds") String[] genTableIds);

    /**
     * 回收站 还原
     *
     * @param genTableIds 主键数组
     * @return 影响行数
     */
    Integer revertRecycleBin(@Param("genTableIds") String[] genTableIds);
}
