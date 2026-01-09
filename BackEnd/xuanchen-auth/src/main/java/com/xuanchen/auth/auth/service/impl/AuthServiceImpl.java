package com.xuanchen.auth.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.mapper.AuthMapper;
import com.xuanchen.auth.auth.service.IAuthService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Service
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Auth> implements IAuthService {
}
