package com.xuanchen.common.api.DictApi.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Data
public class Dict implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    private String id;
    /**
     * table
     */
    private String table;
    /**
     * code
     */
    private String code;
    /**
     * codeValue
     */
    private String codeValue;
    /**
     * text
     */
    private String text;
    /**
     * textValue
     */
    private String textValue;

}
