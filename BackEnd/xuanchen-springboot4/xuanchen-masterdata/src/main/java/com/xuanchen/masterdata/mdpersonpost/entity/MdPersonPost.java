package com.xuanchen.masterdata.mdpersonpost.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类-->用户 岗位 对应关系
 *
 * @author XuanChen
 * @date 2026-01-10
 */
@Data
public class MdPersonPost implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private String id;
    /**
     * 人员ID
     */
    private String personId;
    /**
     * 人员编码（来自人员档案 md_person.person_code，列表展示用）
     */
    @TableField(exist = false)
    private String personCode;
    /**
     * 真实姓名（人员档案姓名，人员维度列表统一展示此字段）
     */
    @TableField(exist = false)
    private String realName;
    /**
     * 岗位ID
     */
    private String postId;
    /**
     * 岗位编码
     */
    @TableField(exist = false)
    private String postCode;
    /**
     * 岗位名称
     */
    @TableField(exist = false)
    private String postName;
}