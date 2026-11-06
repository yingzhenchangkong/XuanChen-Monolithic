package com.xuanchen.visualization.visscreen.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.visualization.visscreen.entity.VisDataset;

import java.util.List;

/**
 * Service接口-->大屏数据集
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface IVisDatasetService extends IService<VisDataset> {

    /**
     * 按大屏ID查询数据集
     *
     * @param screenId
     * @return
     */
    List<VisDataset> listByScreen(String screenId);

    /**
     * 数据集编码在同一大屏内唯一性校验（排除自身 id）
     *
     * @param dataset
     * @return true=已存在
     */
    boolean ifExistsKey(VisDataset dataset);
}
