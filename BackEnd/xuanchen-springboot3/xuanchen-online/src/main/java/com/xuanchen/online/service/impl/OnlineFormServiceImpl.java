package com.xuanchen.online.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.online.entity.OnlineForm;
import com.xuanchen.online.mapper.OnlineFormMapper;
import com.xuanchen.online.service.IOnlineFormService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务实现-->在线表单（流程审批）
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Service
public class OnlineFormServiceImpl extends ServiceImpl<OnlineFormMapper, OnlineForm> implements IOnlineFormService {

    @Override
    public Page<OnlineForm> pageList(long pageNo, long pageSize, String keyword, String username, boolean isAdmin) {
        QueryWrapper<OnlineForm> queryWrapper = new QueryWrapper<>();
        //admin 可见全部，普通用户仅本人表单
        if (!isAdmin) {
            queryWrapper.eq("create_by", username);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.like("form_name", keyword);
        }
        queryWrapper.orderByDesc("update_time");
        return this.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public List<OnlineForm> listOptions(String username, boolean isAdmin) {
        QueryWrapper<OnlineForm> queryWrapper = new QueryWrapper<>();
        if (!isAdmin) {
            queryWrapper.eq("create_by", username);
        }
        queryWrapper.select("id", "form_name", "update_time").orderByDesc("update_time");
        return this.list(queryWrapper);
    }

    @Override
    public OnlineForm getDetail(String id, String username, boolean isAdmin) {
        OnlineForm form = this.getById(id);
        if (form == null) {
            throw new IllegalArgumentException("表单不存在或已被删除！");
        }
        // 读取不校验归属：表单会被流程定义绑定，发起申请的普通用户需要读取 schema 渲染表单；
        // 写操作（保存/删除）仍走 checkOwner 归属校验
        return form;
    }

    @Override
    public OnlineForm saveForm(String id, String formName, String remark, String schemaJson, String username, boolean isAdmin) {
        if (formName == null || formName.isBlank()) {
            throw new IllegalArgumentException("表单名称不能为空！");
        }
        if (formName.length() > 100) {
            throw new IllegalArgumentException("表单名称过长（最长100字符）！");
        }
        OnlineForm form;
        if (StringUtil.isNotEmpty(id)) {
            //更新：校验存在与归属
            form = this.getById(id);
            if (form == null) {
                throw new IllegalArgumentException("表单不存在或已被删除！");
            }
            checkOwner(form, username, isAdmin);
            form.setFormName(formName);
            form.setRemark(remark);
            if (schemaJson != null) {
                form.setSchemaJson(schemaJson);
            }
            form.setUpdateBy(username);
            form.setUpdateTime(LocalDateTime.now());
            this.updateById(form);
        } else {
            //新建：schema 可为空（允许先建空表单）
            form = new OnlineForm();
            form.setFormName(formName);
            form.setRemark(remark);
            form.setSchemaJson(schemaJson == null ? "" : schemaJson);
            form.setCreateBy(username);
            form.setCreateTime(LocalDateTime.now());
            form.setUpdateBy(username);
            form.setUpdateTime(LocalDateTime.now());
            this.save(form);
        }
        return form;
    }

    @Override
    public void deleteForm(String id, String username, boolean isAdmin) {
        OnlineForm form = this.getById(id);
        if (form == null) {
            throw new IllegalArgumentException("表单不存在或已被删除！");
        }
        checkOwner(form, username, isAdmin);
        this.removeById(id);
    }

    /**
     * 归属校验（本人或 admin）
     */
    private void checkOwner(OnlineForm form, String username, boolean isAdmin) {
        if (!isAdmin && !username.equals(form.getCreateBy())) {
            throw new IllegalArgumentException("无权操作他人表单！");
        }
    }
}
