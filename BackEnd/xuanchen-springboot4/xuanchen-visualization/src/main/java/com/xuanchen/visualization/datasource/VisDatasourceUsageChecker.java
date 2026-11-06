package com.xuanchen.visualization.datasource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceUsageChecker;
import com.xuanchen.visualization.visscreen.entity.VisDataset;
import com.xuanchen.visualization.visscreen.mapper.VisDatasetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 删除统一数据源前，检查大屏数据集是否仍引用它 */
@Slf4j
@Component
public class VisDatasourceUsageChecker implements DatasourceUsageChecker {

    @Autowired
    private VisDatasetMapper mapper;

    @Override
    public String module() {
        return "大屏数据集";
    }

    @Override
    public long count(String dsId) {
        try {
            Long count = mapper.selectCount(new QueryWrapper<VisDataset>().eq("ds_id", dsId));
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("[DS-USAGE] 大屏数据集引用检查失败 dsId={}", dsId, e);
            return 0;
        }
    }
}
