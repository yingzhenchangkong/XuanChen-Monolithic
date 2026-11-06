package com.xuanchen.monitor.logoperation.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.monitor.logoperation.entity.LogOperation;
import com.xuanchen.monitor.logoperation.mapper.LogOperationMapper;
import com.xuanchen.monitor.logoperation.service.ILogOperationService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->操作日志
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@Service
public class LogOperationServiceImpl extends ServiceImpl<LogOperationMapper, LogOperation> implements ILogOperationService {
}

