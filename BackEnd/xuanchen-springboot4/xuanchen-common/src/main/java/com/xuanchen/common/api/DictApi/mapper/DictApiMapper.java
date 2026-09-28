package com.xuanchen.common.api.DictApi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.common.api.DictApi.entity.Dict;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mapper接口-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Repository
public interface DictApiMapper extends BaseMapper<Dict> {
    /**
     * 单值字典翻译。
     * <p>安全前提（修改调用方时必须保持）：table/code/text 三个参数通过 XML 的 ${} 直接拼接进 SQL，
     * 因此它们只能来自服务端代码里的 @Dict 注解常量（表名、编码列名、文本列名），
     * <b>严禁</b>把任何 HTTP 请求参数/前端输入透传给这三个参数；可变量值 codeValue 走 #{} 预编译参数。
     */
    String translateFieldToString(@Param("table") String table,
                                  @Param("code") String code,
                                  @Param("codeValue") String codeValue,
                                  @Param("text") String text);

    /**
     * 批量字段翻译：一次 IN 查询返回 (code, text) 行集合，供切面构建 code->text 映射，
     * 避免逐记录逐字段查询导致的 N+1。
     * <p>安全前提：table/code/text 来自服务端 @Dict 注解常量（非用户输入，XML 中以 ${} 拼接），
     * codeValues 集合元素走 #{} 预编译参数。
     */
    List<Map<String, Object>> translateFieldBatch(@Param("table") String table,
                                                  @Param("code") String code,
                                                  @Param("text") String text,
                                                  @Param("codeValues") Collection<String> codeValues);
}
