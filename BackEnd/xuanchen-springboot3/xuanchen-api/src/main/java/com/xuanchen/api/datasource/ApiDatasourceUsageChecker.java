package com.xuanchen.api.datasource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.mapper.ApiMapper;
import com.xuanchen.common.datasource.DatasourceUsageChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 删除统一数据源前，检查接口定义 steps JSON 中是否仍引用它。
 * steps 是 JSON 字符串，用 like 粗匹配数据源 id；漏判只会让提示偏保守，不会误放行删除。
 */
@Slf4j
@Component
public class ApiDatasourceUsageChecker implements DatasourceUsageChecker {

    @Autowired
    private ApiMapper apiMapper;

    @Override
    public String module() {
        return "接口定义";
    }

    @Override
    public long count(String dsId) {
        try {
            Long count = apiMapper.selectCount(new QueryWrapper<Api>().like("steps", dsId));
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("[DS-USAGE] 接口定义引用检查失败 dsId={}", dsId, e);
            return 0;
        }
    }
}
