package com.xuanchen.generator.gendatabase.entity;

import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xuanchen.generator.crypto.DatasourceCryptoTypeHandler;
import com.xuanchen.generator.crypto.MaskedPasswordSerializer;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类 --> 数据库
 *
 * @author XuanChen
 * @date 2026-02-18
 */
@Data
//autoResultMap=true：使 password 字段上的 AES 类型处理器在 SELECT 结果映射时生效
@TableName(value = "gen_database", autoResultMap = true)
public class GenDatabase implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private String id;
    /**
     * 连接类型
     */
    @ExcelProperty("连接类型")
    @ColumnWidth(15)
    private String connType;
    /**
     * 连接名称
     */
    @ExcelProperty("连接名称")
    @ColumnWidth(15)
    private String connName;
    /**
     * 主机
     */
    @ExcelProperty("主机")
    @ColumnWidth(15)
    private String host;
    /**
     * 端口
     */
    @ExcelProperty("端口")
    @ColumnWidth(15)
    private String port;
    /**
     * 数据库名称
     */
    @ExcelProperty("数据库名称")
    @ColumnWidth(15)
    private String dbName;
    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    @ColumnWidth(15)
    private String userName;
    /**
     * 密码
     * <p>
     * 三层保护：库内 AES 密文（TypeHandler 自动加解密）、HTTP 响应固定掩码（不回传真实值）、
     * Excel 导出忽略该列；入参仍可反序列化，编辑时传空或掩码表示不修改。
     */
    @ExcelIgnore
    @TableField(value = "password", typeHandler = DatasourceCryptoTypeHandler.class)
    @JsonSerialize(using = MaskedPasswordSerializer.class)
    private String password;
    /**
     * 排序码
     */
    @ExcelIgnore
    private Integer orderNo;
    /**
     * 状态（1启用，0停用）
     */
    @ExcelIgnore
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
}
