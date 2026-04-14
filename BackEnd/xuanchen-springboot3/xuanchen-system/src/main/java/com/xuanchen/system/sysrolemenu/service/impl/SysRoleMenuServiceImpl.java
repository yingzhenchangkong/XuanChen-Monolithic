package com.xuanchen.system.sysrolemenu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysrolemenu.entity.SysRoleMenu;
import com.xuanchen.system.sysrolemenu.mapper.SysRoleMenuMapper;
import com.xuanchen.system.sysrolemenu.service.ISysRoleMenuService;
import org.springframework.stereotype.Service;

/**
 * Service接口实现类-->角色 菜单 对应关系
 *
 * @author XuanChen
 * @date 2025-04-14
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {
}
