package com.xuanchen.system.sysaccount.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.system.sysaccount.entity.SysAccountView;
import com.xuanchen.system.sysaccount.entity.SysAccount;
import com.xuanchen.system.sysaccount.mapper.SysAccountMapper;
import com.xuanchen.system.sysaccount.service.ISysAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service接口实现类-->用户
 *
 * @author XuanChen
 * @date 2025-04-03
 */
@Service
public class SysAccountServiceImpl extends ServiceImpl<SysAccountMapper, SysAccount> implements ISysAccountService {
    @Autowired
    private SysAccountMapper sysAccountMapper;

    @Override
    public IPage<SysAccountView> listWithPerson(Page<SysAccountView> page, SysAccountView sysAccountView) {
        return page.setRecords(sysAccountMapper.listWithPerson(page, sysAccountView));
    }

    @Override
    public List<SysAccountView> listAllWithPerson(SysAccountView sysAccountView) {
        return sysAccountMapper.listAllWithPerson(sysAccountView);
    }

    @Override
    public IPage<SysAccountView> listRecycleBin(Page<SysAccountView> page, SysAccountView sysAccountView) {
        return page.setRecords(sysAccountMapper.listRecycleBin(page, sysAccountView));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecycleBin(String ids) {
        // 仅处理账号自身数据：账号 + 账号角色关系；人员档案及其部门/岗位关系由人员管理负责
        String[] idArray = ids.split(",");
        sysAccountMapper.deleteRecycleBin(idArray);
        sysAccountMapper.deleteUserRole(idArray);
    }

    @Override
    public void revertRecycleBin(String ids) {
        String[] idArray = ids.split(",");
        sysAccountMapper.revertRecycleBin(idArray);
    }

    @Override
    public Boolean ifExistsId(SysAccountView sysAccountView) {
        return sysAccountMapper.ifExistsId(sysAccountView).size() > 0;
    }

    @Override
    public Boolean ifExistsNoId(SysAccountView sysAccountView) {
        return sysAccountMapper.ifExistsNoId(sysAccountView).size() > 0;
    }
}
