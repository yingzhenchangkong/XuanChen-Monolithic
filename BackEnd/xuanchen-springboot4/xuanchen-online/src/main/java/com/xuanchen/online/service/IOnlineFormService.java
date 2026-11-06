package com.xuanchen.online.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.online.entity.OnlineForm;

import java.util.List;

/**
 * 服务接口-->在线表单（流程审批）
 *
 * @author XuanChen
 * @date 2026-10-28
 */
public interface IOnlineFormService {

    /**
     * 分页列表（本人全部 + admin 全部）
     */
    Page<OnlineForm> pageList(long pageNo, long pageSize, String keyword, String username, boolean isAdmin);

    /**
     * 下拉选项列表（本人全部 + admin 全部，轻量不含 schema）
     */
    List<OnlineForm> listOptions(String username, boolean isAdmin);

    /**
     * 详情（含 schema）
     */
    OnlineForm getDetail(String id, String username, boolean isAdmin);

    /**
     * 保存（新建/更新），返回落库后的实体
     */
    OnlineForm saveForm(String id, String formName, String remark, String schemaJson, String username, boolean isAdmin);

    /**
     * 删除（逻辑删）
     */
    void deleteForm(String id, String username, boolean isAdmin);
}
