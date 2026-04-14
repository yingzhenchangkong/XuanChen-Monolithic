package com.xuanchen.common.api.DictApi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.common.api.DictApi.entity.Dict;
import org.springframework.stereotype.Repository;

/**
 * Mapper接口-->字典API
 *
 * @author XuanChen
 * @date 2026-02-05
 */
@Repository
public interface DictApiMapper extends BaseMapper<Dict> {
    String translateFieldToString(String table, String code, String codeValue, String text);
}
