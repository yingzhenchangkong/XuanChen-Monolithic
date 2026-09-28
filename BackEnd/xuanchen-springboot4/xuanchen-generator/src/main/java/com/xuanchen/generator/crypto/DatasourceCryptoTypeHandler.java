package com.xuanchen.generator.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * gen_database.password 字段的 MyBatis 类型处理器：
 * 写入（INSERT/UPDATE）自动 AES 加密，读取（SELECT 映射）自动解密，
 * 使"库内密文、后端内存明文、HTTP 响应掩码"三层语义对业务代码透明。
 * <p>
 * 注意：本类刻意<strong>不注册为 Spring Bean，也不加 @MappedTypes(String.class)</strong>——
 * 处理器泛型为 String，一旦进入全局 TypeHandlerRegistry 就会成为所有 String 参数的默认处理器，
 * 导致全库任意字符串查询参数被误加密。仅通过实体字段
 * {@code @TableField(typeHandler = DatasourceCryptoTypeHandler.class)}
 * 与 XML resultMap 中的全限定名显式引用，由 MyBatis 按需实例化；
 * 密钥状态保存在 {@link DatasourceCipher} 静态层（启动时由
 * {@link DatasourceCryptoInitializer} 注入），与实例无关。
 *
 * @author XuanChen
 * @date 2026-09-24
 */
public class DatasourceCryptoTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, DatasourceCipher.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return DatasourceCipher.decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return DatasourceCipher.decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return DatasourceCipher.decrypt(cs.getString(columnIndex));
    }
}
