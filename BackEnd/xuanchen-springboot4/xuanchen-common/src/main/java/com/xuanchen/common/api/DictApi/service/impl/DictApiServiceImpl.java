package com.xuanchen.common.api.DictApi.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.api.DictApi.entity.Dict;
import com.xuanchen.common.api.DictApi.mapper.DictApiMapper;
import com.xuanchen.common.api.DictApi.service.IDictApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service接口实现类-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
@RequiredArgsConstructor
public class DictApiServiceImpl extends ServiceImpl<DictApiMapper, Dict> implements IDictApiService {
    private final DictApiMapper dictApiMapper;

    @Override
    public String translateFieldToString(String table, String code, String codeValue, String text) {
        return dictApiMapper.translateFieldToString(table, code, codeValue, text);
    }

    @Override
    public List<Map<String, Object>> translateFieldBatch(String table, String code, String text, Collection<String> codeValues) {
        return dictApiMapper.translateFieldBatch(table, code, text, codeValues);
    }
}
