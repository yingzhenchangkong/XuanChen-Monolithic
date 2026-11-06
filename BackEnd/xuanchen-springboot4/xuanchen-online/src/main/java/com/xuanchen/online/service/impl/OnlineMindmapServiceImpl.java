package com.xuanchen.online.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.online.entity.OnlineMindmap;
import com.xuanchen.online.mapper.OnlineMindmapMapper;
import com.xuanchen.online.service.IOnlineMindmapService;
import com.xuanchen.common.utils.StringUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 服务实现-->在线文档-思维导图
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Service
public class OnlineMindmapServiceImpl extends ServiceImpl<OnlineMindmapMapper, OnlineMindmap> implements IOnlineMindmapService {

    @Override
    public Page<OnlineMindmap> pageList(long pageNo, long pageSize, String keyword, String username, boolean isAdmin) {
        QueryWrapper<OnlineMindmap> queryWrapper = new QueryWrapper<>();
        //admin 可见全部，普通用户仅本人文档
        if (!isAdmin) {
            queryWrapper.eq("create_by", username);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.like("doc_name", keyword);
        }
        queryWrapper.orderByDesc("update_time");
        return this.page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public OnlineMindmap getDetail(String id, String username, boolean isAdmin) {
        OnlineMindmap doc = this.getById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在或已被删除！");
        }
        checkOwner(doc, username, isAdmin);
        return doc;
    }

    @Override
    public OnlineMindmap saveDoc(String id, String docName, String snapshot, String username, boolean isAdmin) {
        if (docName == null || docName.isBlank()) {
            throw new IllegalArgumentException("文档名称不能为空！");
        }
        if (docName.length() > 100) {
            throw new IllegalArgumentException("文档名称过长（最长100字符）！");
        }
        OnlineMindmap doc;
        if (StringUtil.isNotEmpty(id)) {
            //更新：校验存在与归属
            doc = this.getById(id);
            if (doc == null) {
                throw new IllegalArgumentException("文档不存在或已被删除！");
            }
            checkOwner(doc, username, isAdmin);
            doc.setDocName(docName);
            if (snapshot != null) {
                doc.setSnapshot(snapshot);
            }
            doc.setUpdateBy(username);
            doc.setUpdateTime(LocalDateTime.now());
            this.updateById(doc);
        } else {
            //新建：快照可为空（允许先建空文档）
            doc = new OnlineMindmap();
            doc.setDocName(docName);
            doc.setSnapshot(snapshot == null ? "" : snapshot);
            doc.setCreateBy(username);
            doc.setCreateTime(LocalDateTime.now());
            doc.setUpdateBy(username);
            doc.setUpdateTime(LocalDateTime.now());
            this.save(doc);
        }
        return doc;
    }

    @Override
    public void deleteDoc(String id, String username, boolean isAdmin) {
        OnlineMindmap doc = this.getById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在或已被删除！");
        }
        checkOwner(doc, username, isAdmin);
        this.removeById(id);
    }

    /**
     * 归属校验（本人或 admin）
     */
    private void checkOwner(OnlineMindmap doc, String username, boolean isAdmin) {
        if (!isAdmin && !username.equals(doc.getCreateBy())) {
            throw new IllegalArgumentException("无权操作他人文档！");
        }
    }
}
