package com.xuanchen.system.sysuserpost.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysuserpost.entity.SysUserPost;
import com.xuanchen.system.sysuserpost.mapper.SysUserPostMapper;
import com.xuanchen.system.sysuserpost.service.ISysUserPostService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Service
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements ISysUserPostService {
}
