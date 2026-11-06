package com.xuanchen.visualization.visscreen.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.visualization.visscreen.entity.VisScreen;

/**
 * Service接口-->数据大屏
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface IVisScreenService extends IService<VisScreen> {

    /**
     * 分页列表（带创建/更新人姓名）
     *
     * @param page
     * @param screen
     * @return
     */
    IPage<VisScreen> listScreen(IPage<VisScreen> page, VisScreen screen);

    /**
     * 大屏编码唯一性校验（排除自身 id）
     *
     * @param screen
     * @return true=已存在
     */
    boolean ifExistsCode(VisScreen screen);

    /**
     * 复制大屏（配置 JSON 与数据集一并复制）
     *
     * @param id      源大屏ID
     * @param newCode 新编码
     * @param newName 新名称
     * @param userName 操作人
     * @return 新大屏ID
     */
    String copyScreen(String id, String newCode, String newName, String userName);
}
