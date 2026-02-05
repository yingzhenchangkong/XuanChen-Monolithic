package com.xuanchen.common.api.DictApi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuanchen.common.api.DictApi.entity.Dict;

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
}
