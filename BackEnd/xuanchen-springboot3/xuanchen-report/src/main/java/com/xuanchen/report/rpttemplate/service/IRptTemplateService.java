package com.xuanchen.report.rpttemplate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.report.rpttemplate.entity.RptTemplate;

import java.util.List;

/**
 * Service接口-->报表模板
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface IRptTemplateService extends IService<RptTemplate> {

    /**
     * 分页列表（带创建/更新人姓名）
     *
     * @param page
     * @param template
     * @return
     */
    IPage<RptTemplate> listTemplate(IPage<RptTemplate> page, RptTemplate template);

    /**
     * 模板编码唯一性校验（排除自身 id）
     *
     * @param template
     * @return true=已存在
     */
    boolean ifExistsCode(RptTemplate template);

    /**
     * 启用中的模板（报表新增时的下拉数据）
     *
     * @return
     */
    List<RptTemplate> listEnabled();
}
