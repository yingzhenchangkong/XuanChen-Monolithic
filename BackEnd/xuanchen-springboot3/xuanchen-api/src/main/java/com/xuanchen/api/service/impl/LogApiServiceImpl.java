package com.xuanchen.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.LogApi;
import com.xuanchen.api.mapper.ApiMapper;
import com.xuanchen.api.mapper.LogApiMapper;
import com.xuanchen.api.service.ILogApiService;
import com.xuanchen.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service接口实现类-->数据接口服务-调用日志
 * <p>
 * 日志只读：不提供新增/修改/删除，写入统一由 {@code ApiInvokeServiceImpl#writeLog} 负责。
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Slf4j
@Service
public class LogApiServiceImpl extends ServiceImpl<LogApiMapper, LogApi> implements ILogApiService {

    @Autowired
    private ApiMapper apiMapper;

    @Override
    public IPage<LogApi> pageQuery(LogApi query, Integer pageNo, Integer pageSize) {
        QueryWrapper<LogApi> wrapper = new QueryWrapper<>();
        if (query != null) {
            // 接口编码模糊查询（日志页多为按编码线索排查，精确匹配可用性差）
            if (StringUtil.isNotEmpty(query.getApiCode())) {
                wrapper.like("api_code", query.getApiCode().trim());
            }
            if (StringUtil.isNotEmpty(query.getTriggerType())) {
                wrapper.eq("trigger_type", query.getTriggerType());
            }
            // success 为 tinyint(1)：0 是有效筛选值，不能用布尔判断
            if (StringUtil.isNotEmpty(query.getSuccess())) {
                wrapper.eq("success", query.getSuccess());
            }
            if (StringUtil.isNotEmpty(query.getBeginTime())) {
                wrapper.ge("create_time", query.getBeginTime());
            }
            if (StringUtil.isNotEmpty(query.getEndTime())) {
                wrapper.le("create_time", query.getEndTime());
            }
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<LogApi> page = new Page<>(pageNo == null || pageNo < 1 ? 1 : pageNo,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
        IPage<LogApi> pageList = page(page, wrapper);
        fillApiName(pageList.getRecords());
        return pageList;
    }

    /**
     * 按 api_code 批量回填接口名称，避免前端只能看到一串编码。
     * 失败（如接口已删除、api 表结构变更）不影响日志列表本身，仅名称留空。
     */
    private void fillApiName(List<LogApi> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> codes = records.stream()
                .map(LogApi::getApiCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        try {
            List<Api> apis = apiMapper.selectList(new QueryWrapper<Api>()
                    .select("api_code", "api_name")
                    .in("api_code", codes));
            Map<String, String> nameMap = apis.stream()
                    .filter(api -> api.getApiCode() != null)
                    .collect(Collectors.toMap(Api::getApiCode,
                            api -> api.getApiName() == null ? "" : api.getApiName(),
                            (first, second) -> first));
            records.forEach(row -> row.setApiName(nameMap.get(row.getApiCode())));
        } catch (Exception e) {
            log.warn("回填接口名称失败，日志列表将只显示接口编码", e);
        }
    }
}
