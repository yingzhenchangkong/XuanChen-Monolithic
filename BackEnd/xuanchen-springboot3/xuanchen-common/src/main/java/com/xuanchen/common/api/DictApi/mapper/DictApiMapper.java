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
     * 安全前提：table/code/text 通过 XML 的 ${} 直接拼接进 SQL，只能来自服务端 @Dict 注解常量，
     * 严禁透传任何用户输入；codeValue 走 #{} 预编译参数
     */
    String translateFieldToString(@Param("table") String table,
                                  @Param("code") String code,
                                  @Param("codeValue") String codeValue,
                                  @Param("text") String text);

    /**
     * 批量字段翻译：一次 IN 查询返回 (code, text) 行集合，供切面构建 code->text 映射，
     * 避免逐记录逐字段查询导致的 N+1。
     * 安全前提同上：table/code/text 仅接受服务端 @Dict 注解常量，codeValues 元素走 #{} 预编译参数
     */
    List<Map<String, Object>> translateFieldBatch(@Param("table") String table,
                                                  @Param("code") String code,
                                                  @Param("text") String text,
                                                  @Param("codeValues") Collection<String> codeValues);

    /**
     * 按字典编码取整组启用字典项（value -> text），供 Excel 导出翻译等批量场景一次性加载并缓存。
     * 仅返回 status=1、未删除的条目，按 order_no 排序。
     */
    List<Map<String, Object>> selectDictItemMap(@Param("dictCode") String dictCode);
}
