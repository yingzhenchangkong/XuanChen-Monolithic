package com.xuanchen.generator.gentable.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.mapper.GenTableMapper;
import com.xuanchen.generator.gentable.service.IGenTableService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->数据库表
 *
 * @author XuanChen
 * @date 2026-02-25
 */
@Service
public class GenTableServiceImpl extends ServiceImpl<GenTableMapper, GenTable> implements IGenTableService {
}
