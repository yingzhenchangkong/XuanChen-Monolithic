package com.xuanchen.visualization.visscreen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.visualization.visscreen.entity.VisDataset;
import com.xuanchen.visualization.visscreen.mapper.VisDatasetMapper;
import com.xuanchen.visualization.visscreen.service.IVisDatasetService;
import com.xuanchen.common.utils.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->大屏数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Service
public class VisDatasetServiceImpl extends ServiceImpl<VisDatasetMapper, VisDataset> implements IVisDatasetService {

    @Override
    public List<VisDataset> listByScreen(String screenId) {
        QueryWrapper<VisDataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("screen_id", screenId).orderByAsc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public boolean ifExistsKey(VisDataset dataset) {
        QueryWrapper<VisDataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("screen_id", dataset.getScreenId()).eq("dataset_key", dataset.getDatasetKey());
        if (StringUtil.isNotEmpty(dataset.getId())) {
            queryWrapper.ne("id", dataset.getId());
        }
        return this.count(queryWrapper) > 0;
    }
}
