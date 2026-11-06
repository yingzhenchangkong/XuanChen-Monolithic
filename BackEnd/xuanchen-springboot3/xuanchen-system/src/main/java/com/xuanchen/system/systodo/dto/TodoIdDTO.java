package com.xuanchen.system.systodo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO --> 待办 id 入参（完成/删除等单 id 操作）
 *
 * <p>独立 DTO 接参，不复用 SysTodo 实体，避免客户端通过实体字段越权改写他人数据。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Data
public class TodoIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待办 id
     */
    private String id;
}
