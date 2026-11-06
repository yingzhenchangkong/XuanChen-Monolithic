package com.xuanchen.report.rpttemplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.report.rpttemplate.entity.RptTemplate;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper-->报表模板
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Repository
public interface RptTemplateMapper extends BaseMapper<RptTemplate> {

    /**
     * 分页列表（带创建人/更新人姓名联查）
     *
     * @param page
     * @param template
     * @return
     */
    IPage<RptTemplate> listTemplate(@Param("page") IPage<RptTemplate> page, @Param("template") RptTemplate template);
}
