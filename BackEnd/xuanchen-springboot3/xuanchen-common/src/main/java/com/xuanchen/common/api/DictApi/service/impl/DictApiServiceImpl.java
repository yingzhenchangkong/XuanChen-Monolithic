package com.xuanchen.common.api.DictApi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.common.api.DictApi.entity.Dict;
import com.xuanchen.common.api.DictApi.mapper.DictApiMapper;
import com.xuanchen.common.api.DictApi.service.IDictApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Service接口实现类-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Service
public class DictApiServiceImpl extends ServiceImpl<DictApiMapper, Dict> implements IDictApiService {
    @Autowired
    private DictApiMapper dictApiMapper;

    @Override
    public String translateFieldToString(String table, String code, String codeValue, String text) {
        return dictApiMapper.translateFieldToString(table, code, codeValue, text);
    }
}
