package com.xuanchen.masterdata.mdpersondept.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 实体类-->人员 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@Data
public class MdPersonDept implements Serializable {
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
     * 部门ID
     */
    private String deptId;
    /**
     * 部门编码
     */
    @TableField(exist = false)
    private String deptCode;
    /**
     * 部门名称
     */
    @TableField(exist = false)
    private String deptName;
    /**
     * 人员ID(多)：待关联部门的人员主键集合
     */
    @TableField(exist = false)
    private List<String> personIds;
}
