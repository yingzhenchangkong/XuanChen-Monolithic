package com.xuanchen.system.sysmenu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.system.sysmenu.entity.SysMenu;
import com.xuanchen.system.sysmenu.entity.SysMenuTree;
import com.xuanchen.system.sysmenu.mapper.SysMenuMapper;
import com.xuanchen.system.sysmenu.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->菜单
 *
 * @author XuanChen
 * @date 2025-04-04
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {
    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> listMenuByUserName(String userName) {
        return sysMenuMapper.listMenuByUserName(userName);
    }

    @Override
    public List<String> listAuthByUserId(String userId) {
        return sysMenuMapper.listAuthByUserId(userId);
    }

    @Override
    public void listToTree(List<SysMenuTree> listTree, List<SysMenu> list, SysMenuTree temp) {
        for (SysMenu item : list) {
            String tempPid = item.getParentId();
            SysMenuTree tree = new SysMenuTree(item);
            if (temp == null && StringUtil.isEmpty(tempPid)) {
                listTree.add(tree);
                if (!tree.getIsLeaf()) {
                    listToTree(listTree, list, tree);
                }
            } else if (temp != null && tempPid != null && tempPid.equals(temp.getId())) {
                temp.getChildren().add(tree);
                if (!tree.getIsLeaf()) {
                    listToTree(listTree, list, tree);
                }
            }
        }
    }

    @Override
    public Boolean ifExistsId(SysMenu sysMenu) {
        return sysMenuMapper.ifExistsId(sysMenu).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysMenu sysMenu) {
        return sysMenuMapper.ifExistsNoId(sysMenu).size() > 0;
    }
}
