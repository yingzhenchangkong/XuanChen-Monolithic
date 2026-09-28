package com.xuanchen.auth.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.mapper.AuthMapper;
import com.xuanchen.auth.auth.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Auth> implements IAuthService {
    private final AuthMapper authMapper;

    @Override
    public Auth getByUserName(String userName) {
        QueryWrapper<Auth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return authMapper.selectOne(queryWrapper);
    }

    @Override
    public List<String> listRoleCodesByUserName(String userName) {
        return authMapper.listRoleCodesByUserName(userName);
    }

    @Override
    public List<String> listPermsByUserName(String userName) {
        return authMapper.listPermsByUserName(userName);
    }

    @Override
    public String getConfigValueByKey(String configKey) {
        return authMapper.getConfigValueByKey(configKey);
    }
}
