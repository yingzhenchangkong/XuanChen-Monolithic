package com.xuanchen.masterdata.mdperson.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xuanchen.common.excel.annotation.ExcelDict;
import com.xuanchen.common.excel.converter.ExcelDictConverter;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;
import org.apache.fesod.sheet.annotation.write.style.ColumnWidth;
import org.apache.fesod.sheet.annotation.write.style.ContentStyle;
import org.apache.fesod.sheet.enums.poi.HorizontalAlignmentEnum;
import org.apache.fesod.sheet.enums.poi.VerticalAlignmentEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实体类-->人员信息（员工档案）
 * 人员独立于账号存在：一个人员可对应 1~N 个账号，也可无任何账号
 * <p>
 * 导出说明：本类的 {@code @ExcelProperty} 仅用于「人员管理 → 导出」；
 * 导入使用单独的 {@link MdPersonImport}（枚举列按中文文案读取，再由控制器换算成编码值）。
 *
 * @author XuanChen
 * @date 2026-10-16
 */
@Data
@TableName("md_person")
public class MdPerson implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键（人员ID）
     */
    @ExcelIgnore
    private String id;
    /**
     * 真实姓名
     */
    @ExcelProperty("真实姓名")
    @ColumnWidth(14)
    private String realName;
    /**
     * 身份证号
     */
    @ExcelProperty("身份证号")
    @ColumnWidth(22)
    private String idCard;
    /**
     * 出生日期
     */
    @ExcelProperty("出生日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private LocalDate birthday;
    /**
     * 民族
     */
    @ExcelProperty("民族")
    @ColumnWidth(10)
    private String nation;
    /**
     * 户籍 / 常住地址
     */
    @ExcelProperty("户籍/常住地址")
    @ColumnWidth(34)
    private String address;
    /**
     * 籍贯
     */
    @ExcelProperty("籍贯")
    @ColumnWidth(14)
    private String nativePlace;
    /**
     * 性别：1男2女（导出翻译取自 DB 数据字典 gender）
     */
    @ExcelProperty(value = "性别", converter = ExcelDictConverter.class)
    @ExcelDict("gender")
    @ColumnWidth(8)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private Integer gender;
    /**
     * 婚姻状况：1未婚2已婚3离异4丧偶（导出翻译取自 DB 数据字典 marital_status）
     */
    @ExcelProperty(value = "婚姻状况", converter = ExcelDictConverter.class)
    @ExcelDict("marital_status")
    @ColumnWidth(12)
    private Integer maritalStatus;
    /**
     * 学历：1小学2初中3高中4大专5本科6研究生（导出翻译取自 DB 数据字典 education）
     */
    @ExcelProperty(value = "学历", converter = ExcelDictConverter.class)
    @ExcelDict("education")
    @ColumnWidth(10)
    private Integer education;
    /**
     * 人员编码
     */
    @ExcelProperty("人员编码")
    @ColumnWidth(14)
    private String personCode;
    /**
     * 人员照片
     */
    @ExcelIgnore
    private String photo;
    /**
     * 手机号
     */
    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String mobile;
    /**
     * 紧急联系人
     */
    @ExcelProperty("紧急联系人")
    @ColumnWidth(14)
    private String emergencyContact;
    /**
     * 紧急联系电话
     */
    @ExcelProperty("紧急联系电话")
    @ColumnWidth(16)
    private String emergencyPhone;
    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    @ColumnWidth(26)
    private String email;
    /**
     * 入职日期
     */
    @ExcelProperty("入职日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private LocalDate entryDate;
    /**
     * 离职日期
     */
    @ExcelProperty("离职日期")
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(14)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private LocalDate leaveDate;
    /**
     * 人员类型：1正式员工2外协3临时人员（导出翻译取自 DB 数据字典 person_type）
     */
    @ExcelProperty(value = "人员类型", converter = ExcelDictConverter.class)
    @ExcelDict("person_type")
    @ColumnWidth(12)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private Integer personType;
    /**
     * 排序码
     */
    @ExcelProperty("排序码")
    @ColumnWidth(10)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private Integer orderNo;
    /**
     * 状态（1在职2试用3离职4退休）：导出翻译取自 DB 数据字典 person_status
     */
    @ExcelProperty(value = "人员状态", converter = ExcelDictConverter.class)
    @ExcelDict("person_status")
    @ColumnWidth(12)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER)
    private Integer status;
    /**
     * 删除状态（0正常，1已删除）
     */
    @ExcelIgnore
    @TableLogic
    private Integer delFlag;
    /**
     * 创建者
     */
    @ExcelIgnore
    private String createBy;
    /**
     * 创建时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    @ExcelIgnore
    private String updateBy;
    /**
     * 更新时间
     */
    @ExcelIgnore
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 部门IDS（关系表 md_person_dept，非本表列；人员管理内维护）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] deptIds;
    /**
     * 岗位IDS（关系表 md_person_post，非本表列；人员管理内维护）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String[] postIds;
    /**
     * 部门名称（关系表 md_person_dept 对应的部门名称，仅列表/详情展示，非本表列）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String deptNames;
    /**
     * 岗位名称（关系表 md_person_post 对应的岗位名称，仅列表/详情展示，非本表列）
     */
    @ExcelIgnore
    @TableField(exist = false)
    private String postNames;
}
