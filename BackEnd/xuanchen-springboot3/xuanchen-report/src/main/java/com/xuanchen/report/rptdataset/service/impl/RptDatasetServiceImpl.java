package com.xuanchen.report.rptdataset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rptdataset.entity.RptDataset;
import com.xuanchen.report.rptdataset.mapper.RptDatasetMapper;
import com.xuanchen.report.rptdataset.service.IRptDatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->报表数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Service
public class RptDatasetServiceImpl extends ServiceImpl<RptDatasetMapper, RptDataset> implements IRptDatasetService {

    @Autowired
    private RptDatasetMapper rptDatasetMapper;

    @Override
    public List<RptDataset> listByTemplate(String templateId) {
        return rptDatasetMapper.listByTemplate(templateId);
    }

    @Override
    public boolean ifExistsCode(RptDataset dataset) {
        QueryWrapper<RptDataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", dataset.getTemplateId());
        queryWrapper.eq("dataset_code", dataset.getDatasetCode());
        if (StringUtil.isNotEmpty(dataset.getId())) {
            queryWrapper.ne("id", dataset.getId());
        }
        return this.count(queryWrapper) > 0;
    }
}
