package com.xuanchen.monitor.monloglogin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.monitor.monloglogin.entity.MonLogLogin;
import com.xuanchen.monitor.monloglogin.mapper.MonLogLoginMapper;
import com.xuanchen.monitor.monloglogin.service.IMonLogLoginService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->登录日志
 *
 * @author XuanChen
 * @date 2026-02-04
 */
@Service
public class MonLogLoginServiceImpl extends ServiceImpl<MonLogLoginMapper, MonLogLogin> implements IMonLogLoginService {
}