package com.xuanchen.report.rptdataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.report.rptdataset.entity.RptDataset;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper-->报表数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Repository
public interface RptDatasetMapper extends BaseMapper<RptDataset> {

    /**
     * 模板数据集列表（联查数据源名称）
     *
     * @param templateId
     * @return
     */
    java.util.List<RptDataset> listByTemplate(@Param("templateId") String templateId);
}
