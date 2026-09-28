package com.xuanchen.common.api.DictApi.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.common.api.DictApi.entity.Dict;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service接口-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
public interface IDictApiService extends IService<Dict> {
    /**
     * 字段翻译为字符串
     *
     * @param table 表
     * @param code  字段编码
     * @param text  字段名称
     * @return 字段编码对应的字段名称对应的值
     */
    String translateFieldToString(String table, String code, String codeValue, String text);

    /**
     * 批量字段翻译，返回行集合（键：dict_code_val/dict_text_val）
     */
    List<Map<String, Object>> translateFieldBatch(String table, String code, String text, Collection<String> codeValues);
}
