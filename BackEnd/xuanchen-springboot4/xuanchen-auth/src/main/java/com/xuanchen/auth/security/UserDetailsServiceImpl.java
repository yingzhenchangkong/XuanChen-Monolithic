package com.xuanchen.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现类
 *
 * @author XuanChen
 * @date 2026-03-20
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IAuthService authService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<Auth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Auth::getUserName, username).eq(Auth::getDelFlag, 0);
        Auth auth = authService.getOne(queryWrapper);

        if (auth == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 这里可以根据实际情况添加权限信息
        return User.withUsername(auth.getUserName())
                .password(auth.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}
