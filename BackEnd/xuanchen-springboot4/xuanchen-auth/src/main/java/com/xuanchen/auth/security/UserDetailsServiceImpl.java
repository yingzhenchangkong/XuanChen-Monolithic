package com.xuanchen.auth.security;

import com.xuanchen.auth.auth.entity.Auth;
import com.xuanchen.auth.auth.service.IAuthService;
import com.xuanchen.common.utils.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务实现类
 * 从数据库加载用户真实角色（ROLE_角色编码）与菜单权限标识（perms），并体现账号冻结状态
 *
 * @author XuanChen
 * @date 2026-03-20
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final IAuthService authService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //复用 IAuthService 统一入口；@TableLogic 已保证 del_flag = 0，无需再手写删除标记条件
        Auth auth = authService.getByUserName(username);

        if (auth == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        //1. 角色：ROLE_ + 角色编码（如 ROLE_admin、ROLE_common）
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roleCodes = authService.listRoleCodesByUserName(username);
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                if (StringUtil.isNotEmpty(roleCode)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.trim()));
                }
            }
        }

        //2. 菜单权限标识（如 system:user:list），供细粒度 hasAuthority 校验
        List<String> perms = authService.listPermsByUserName(username);
        if (perms != null) {
            for (String perm : perms) {
                if (StringUtil.isNotEmpty(perm)) {
                    authorities.add(new SimpleGrantedAuthority(perm.trim()));
                }
            }
        }

        //3. 账号状态：status=1 正常，其余（2冻结）禁用
        boolean enabled = auth.getStatus() != null && auth.getStatus() == 1;
        boolean pwdResetRequired = Integer.valueOf(1).equals(auth.getPwdResetRequired());

        return new LoginUser(auth.getId(), auth.getUserName(), auth.getPassword(), enabled,
                pwdResetRequired, authorities);
    }
}
