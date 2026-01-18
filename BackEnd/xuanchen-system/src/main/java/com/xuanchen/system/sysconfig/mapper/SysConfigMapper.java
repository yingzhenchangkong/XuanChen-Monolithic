package com.xuanchen.system.sysconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.system.sysconfig.entity.SysConfig;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Repository
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    /**
     * 回收站 列表
     *
     * @param page
     * @param sysConfig
     * @return
     */
    List<SysConfig> listRecycleBin(@Param("page") Page<SysConfig> page, @Param("sysConfig") SysConfig sysConfig);

    /**
     * 回收站 彻底删除
     *
     * @param sysConfigIds
     * @return
     */
    Integer deleteRecycleBin(@Param("sysConfigIds") String[] sysConfigIds);

    /**
     * 回收站 还原
     *
     * @param sysConfigIds
     * @return
     */
    Integer revertRecycleBin(@Param("sysConfigIds") String[] sysConfigIds);

    List<SysConfig> ifExistsId(@Param("sysConfig") SysConfig sysConfig);

    List<SysConfig> ifExistsNoId(@Param("sysConfig") SysConfig sysConfig);
}
