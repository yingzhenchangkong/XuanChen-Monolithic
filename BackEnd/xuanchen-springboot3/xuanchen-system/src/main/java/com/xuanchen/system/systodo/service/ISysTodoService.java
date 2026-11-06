package com.xuanchen.system.systodo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.system.systodo.entity.SysTodo;

/**
 * Service接口-->系统待办
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface ISysTodoService extends IService<SysTodo> {

    /**
     * 分页查询当前用户的待办
     *
     * @param page    分页参数
     * @param sysTodo 查询条件（userName 必传；title/title 模糊、status 精确可选）
     * @return 分页结果
     */
    IPage<SysTodo> listMine(IPage<SysTodo> page, SysTodo sysTodo);
}
