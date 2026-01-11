package com.xuanchen.system.sysconfig.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysconfig.entity.SysConfig;
import com.xuanchen.system.sysconfig.mapper.SysConfigMapper;
import com.xuanchen.system.sysconfig.service.ISysConfigService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->参数配置
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {
}
