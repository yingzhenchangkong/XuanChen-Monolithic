package com.xuanchen.monitor.loglogin.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.monitor.loglogin.entity.LogLogin;
import com.xuanchen.monitor.loglogin.mapper.LogLoginMapper;
import com.xuanchen.monitor.loglogin.service.ILogLoginService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->登录日志
 *
 * @author XuanChen
 * @date 2026-02-04
 */
@Service
public class LogLoginServiceImpl extends ServiceImpl<LogLoginMapper, LogLogin> implements ILogLoginService {
}
