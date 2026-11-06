package com.xuanchen.visualization.visscreen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.visualization.visscreen.entity.VisScreen;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper-->数据大屏
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Repository
public interface VisScreenMapper extends BaseMapper<VisScreen> {

    /**
     * 分页列表（带创建/更新人姓名，不查 config_json 大字段）
     *
     * @param page
     * @param screen
     * @return
     */
    IPage<VisScreen> listScreen(@Param("page") IPage<VisScreen> page, @Param("screen") VisScreen screen);
}
