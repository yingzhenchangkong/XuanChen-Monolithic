package com.xuanchen.report.rptdataset.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.report.rptdataset.entity.RptDataset;

import java.util.List;

/**
 * Service接口-->报表数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface IRptDatasetService extends IService<RptDataset> {

    /**
     * 模板数据集列表（联查数据源名称）
     *
     * @param templateId
     * @return
     */
    List<RptDataset> listByTemplate(String templateId);

    /**
     * 数据集编码在模板内唯一性校验（排除自身 id）
     *
     * @param dataset
     * @return true=已存在
     */
    boolean ifExistsCode(RptDataset dataset);
}
