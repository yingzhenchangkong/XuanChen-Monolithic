package com.xuanchen.masterdata.mdperson.entity;

import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 导入模型-->人员档案 Excel 行
 * <p>
 * 与 {@link MdPerson} 的导出表头一一对应，区别在于：枚举列（性别/婚姻状况/学历/人员类型/人员状态）
 * 在这里用 {@code String} 接收中文文案（如「在职」「本科」），由控制器换算为编码值后写入
 * {@link MdPerson}。这样做是因为导出的字典翻译转换器只实现「编码 -> 文案」，
 * 直接在实体上读回会抛 UnsupportedOperationException。
 *
 * @author XuanChen
 * @date 2026-10-17
 */
@Data
public class MdPersonImport implements Serializable {
    private static final long serialVersionUID = 1L;

    @ExcelProperty("真实姓名")
    private String realName;

    @ExcelProperty("身份证号")
    private String idCard;

    @ExcelProperty("出生日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate birthday;

    @ExcelProperty("民族")
    private String nation;

    @ExcelProperty("户籍/常住地址")
    private String address;

    @ExcelProperty("籍贯")
    private String nativePlace;

    /**
     * 性别：支持文案（男/女）或编码（1/2）
     */
    @ExcelProperty("性别")
    private String gender;

    /**
     * 婚姻状况：支持文案（未婚/已婚/离异/丧偶）或编码
     */
    @ExcelProperty("婚姻状况")
    private String maritalStatus;

    /**
     * 学历：支持文案或编码
     */
    @ExcelProperty("学历")
    private String education;

    @ExcelProperty("人员编码")
    private String personCode;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("紧急联系人")
    private String emergencyContact;

    @ExcelProperty("紧急联系电话")
    private String emergencyPhone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("入职日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate entryDate;

    @ExcelProperty("离职日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate leaveDate;

    /**
     * 人员类型：支持文案或编码
     */
    @ExcelProperty("人员类型")
    private String personType;

    @ExcelProperty("排序码")
    private Integer orderNo;

    /**
     * 人员状态：支持文案（在职/试用/离职/退休）或编码
     */
    @ExcelProperty("人员状态")
    private String status;
}
