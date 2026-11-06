package com.xuanchen.online.entity;

import com.xuanchen.filemanage.entity.FileInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 在线协同文档列表行 VO：file_info 记录 + 当前用户的协作角色
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineDocVO extends FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前用户角色：owner=属主 collaborator=协作者
     */
    private String role;

    /**
     * 协作权限（1=可编辑 2=只读；属主为 null）
     */
    private Integer permission;
}
