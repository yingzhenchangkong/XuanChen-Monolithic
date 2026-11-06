package com.xuanchen.generator.genprofile.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.generator.genprofile.entity.GenProjectProfile;

import java.util.List;

/**
 * Service接口-->代码生成项目预设
 *
 * @author XuanChen
 * @date 2026-10-20
 */
public interface IGenProjectProfileService extends IService<GenProjectProfile> {

    /**
     * 全部预设（按排序码升序，同序按创建时间倒序）
     *
     * @return 预设列表
     */
    List<GenProjectProfile> listAll();

    /**
     * 新增或修改预设（含名称唯一、标识符、相对路径校验）
     *
     * @param profile 预设
     * @return 预设主键
     */
    String saveProfile(GenProjectProfile profile);
}
