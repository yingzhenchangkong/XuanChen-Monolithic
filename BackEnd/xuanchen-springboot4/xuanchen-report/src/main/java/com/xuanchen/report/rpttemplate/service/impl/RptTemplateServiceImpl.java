package com.xuanchen.report.rpttemplate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rpttemplate.entity.RptTemplate;
import com.xuanchen.report.rpttemplate.mapper.RptTemplateMapper;
import com.xuanchen.report.rpttemplate.service.IRptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->报表模板
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Service
public class RptTemplateServiceImpl extends ServiceImpl<RptTemplateMapper, RptTemplate> implements IRptTemplateService {

    @Autowired
    private RptTemplateMapper rptTemplateMapper;

    @Override
    public IPage<RptTemplate> listTemplate(IPage<RptTemplate> page, RptTemplate template) {
        return rptTemplateMapper.listTemplate(page, template);
    }

    @Override
    public boolean ifExistsCode(RptTemplate template) {
        QueryWrapper<RptTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_code", template.getTemplateCode());
        if (StringUtil.isNotEmpty(template.getId())) {
            queryWrapper.ne("id", template.getId());
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<RptTemplate> listEnabled() {
        QueryWrapper<RptTemplate> queryWrapper = new QueryWrapper<>();
        // 仅返回已设计的模板：报表新增时会复制模板快照，空模板会导致报表永远无法发布
        queryWrapper.eq("status", 1)
                .isNotNull("config_json").ne("config_json", "")
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }
}
