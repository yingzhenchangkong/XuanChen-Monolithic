package com.xuanchen.monitor.monlogoperation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.monitor.monlogoperation.entity.MonLogOperation;
import com.xuanchen.monitor.monlogoperation.mapper.MonLogOperationMapper;
import com.xuanchen.monitor.monlogoperation.service.IMonLogOperationService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->操作日志
 *
 * @author XuanChen
 * @date 2026-02-07
 */
@Service
public class MonLogOperationServiceImpl extends ServiceImpl<MonLogOperationMapper, MonLogOperation> implements IMonLogOperationService {
}
