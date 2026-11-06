package com.xuanchen.generator.datasource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.datasource.DatasourceUsageChecker;
import com.xuanchen.generator.genprofile.entity.GenProjectProfile;
import com.xuanchen.generator.genprofile.mapper.GenProjectProfileMapper;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.mapper.GenTableMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 删除统一数据源前，检查代码生成的表配置与项目档案是否仍引用它 */
@Slf4j
@Component
public class GenDatasourceUsageChecker implements DatasourceUsageChecker {

    @Autowired
    private GenTableMapper tableMapper;
    @Autowired
    private GenProjectProfileMapper profileMapper;

    @Override
    public String module() {
        return "代码生成配置";
    }

    @Override
    public long count(String dsId) {
        try {
            Long tables = tableMapper.selectCount(new QueryWrapper<GenTable>().eq("database_id", dsId));
            Long profiles = profileMapper.selectCount(new QueryWrapper<GenProjectProfile>().eq("database_id", dsId));
            return (tables == null ? 0 : tables) + (profiles == null ? 0 : profiles);
        } catch (Exception e) {
            log.warn("[DS-USAGE] 代码生成配置引用检查失败 dsId={}", dsId, e);
            return 0;
        }
    }
}
