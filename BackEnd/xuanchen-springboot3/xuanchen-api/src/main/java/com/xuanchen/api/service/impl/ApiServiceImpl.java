package com.xuanchen.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.api.entity.Api;
import com.xuanchen.api.entity.ApiData;
import com.xuanchen.api.entity.ApiSchedule;
import com.xuanchen.api.mapper.ApiDataMapper;
import com.xuanchen.api.mapper.ApiScheduleMapper;
import com.xuanchen.api.mapper.ApiMapper;
import com.xuanchen.api.service.IApiInvokeService;
import com.xuanchen.api.service.IApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务实现-->数据接口服务-接口管理
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Service
public class ApiServiceImpl extends ServiceImpl<ApiMapper, Api> implements IApiService {

    @Autowired
    private ApiScheduleMapper scheduleMapper;
    @Autowired
    private ApiDataMapper dataMapper;
    @Autowired
    private IApiInvokeService invokeService;

    @Override
    public Page<Api> pageList(long pageNo, long pageSize, String keyword) {
        QueryWrapper<Api> qw = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(keyword)) {
            qw.like("api_name", keyword).or().like("api_code", keyword);
        }
        qw.orderByDesc("update_time");
        return this.page(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public Api getDetail(String id) {
        Api api = this.getById(id);
        if (api == null) {
            throw new IllegalArgumentException("接口不存在或已被删除！");
        }
        return api;
    }

    @Override
    public Api saveDoc(String id, String apiCode, String apiName, String apiKey, String steps, String remark, String username) {
        if (apiCode == null || !apiCode.matches("[a-zA-Z][a-zA-Z0-9_-]{1,63}")) {
            throw new IllegalArgumentException("接口编码须以字母开头，仅含字母数字-_，长度2~64！");
        }
        if (apiName == null || apiName.isBlank()) {
            throw new IllegalArgumentException("接口名称不能为空！");
        }
        if (apiName.length() > 100) {
            throw new IllegalArgumentException("接口名称过长（最长100字符）！");
        }
        if (apiKey != null && apiKey.isBlank()) {
            apiKey = null;
        }
        //步骤链合法性（解析即校验）
        IApiInvokeService.parseSteps(steps);
        // 已发布接口一旦引用外部数据源，不允许去掉访问密钥（匿名入口不可直连外部库）
        boolean published = false;
        if (StringUtil.isNotEmpty(id)) {
            Api existing = this.getById(id);
            published = existing != null && existing.getStatus() != null && existing.getStatus() == 1;
        }
        if (published && StringUtil.isEmpty(apiKey) && IApiInvokeService.usesExternalDatasource(steps)) {
            throw new IllegalArgumentException("已发布的接口引用外部数据源时必须保留访问密钥！");
        }
        Api api;
        if (StringUtil.isNotEmpty(id)) {
            api = this.getById(id);
            if (api == null) {
                throw new IllegalArgumentException("接口不存在或已被删除！");
            }
            //编码不可与其他接口重复（排除自身）
            checkCodeUnique(apiCode, id);
            api.setApiCode(apiCode);
            api.setApiName(apiName);
            api.setApiKey(apiKey);
            api.setSteps(steps);
            api.setRemark(remark);
            api.setUpdateBy(username);
            api.setUpdateTime(LocalDateTime.now());
            this.updateById(api);
        } else {
            checkCodeUnique(apiCode, null);
            api = new Api();
            api.setApiCode(apiCode);
            api.setApiName(apiName);
            api.setApiKey(apiKey);
            api.setSteps(steps);
            api.setRemark(remark);
            api.setStatus(0);
            api.setCreateBy(username);
            api.setCreateTime(LocalDateTime.now());
            api.setUpdateBy(username);
            api.setUpdateTime(LocalDateTime.now());
            this.save(api);
        }
        //配置变更使路由缓存失效（重新发布后按新配置回源）
        invokeService.evictRouteCache(apiCode);
        return api;
    }

    private void checkCodeUnique(String apiCode, String excludeId) {
        QueryWrapper<Api> qw = new QueryWrapper<>();
        qw.eq("api_code", apiCode);
        if (StringUtil.isNotEmpty(excludeId)) {
            qw.ne("id", excludeId);
        }
        if (this.count(qw) > 0) {
            throw new IllegalArgumentException("接口编码已存在：" + apiCode);
        }
    }

    @Override
    public void deleteDoc(String id, String username) {
        Api api = this.getDetail(id);
        this.removeById(id);
        //重命名编码释放唯一索引（逻辑删行仍占用 uk_api_code）
        this.baseMapper.renameCodeOnDelete(id);
        //同步清理该接口的定时配置（物理删，配置无保留价值）
        scheduleMapper.delete(new QueryWrapper<ApiSchedule>().eq("api_code", api.getApiCode()));
        invokeService.evictRouteCache(api.getApiCode());
    }

    @Override
    public void publish(String id, String username) {
        Api api = this.getDetail(id);
        //发布前跑一遍步骤链校验 + 安全约束
        IApiInvokeService.parseSteps(api.getSteps());
        if (StringUtil.isEmpty(api.getApiKey()) && IApiInvokeService.usesExternalDatasource(api.getSteps())) {
            throw new IllegalArgumentException("含外部数据源的接口必须设置访问密钥后才能发布（匿名分发入口不允许直连外部库）！");
        }
        api.setStatus(1);
        api.setUpdateBy(username);
        api.setUpdateTime(LocalDateTime.now());
        this.updateById(api);
        invokeService.evictRouteCache(api.getApiCode());
    }

    @Override
    public void stop(String id, String username) {
        Api api = this.getDetail(id);
        api.setStatus(2);
        api.setUpdateBy(username);
        api.setUpdateTime(LocalDateTime.now());
        this.updateById(api);
        invokeService.evictRouteCache(api.getApiCode());
    }

    // ---------------- 定时采集配置 ----------------

    @Override
    public Page<ApiSchedule> schedulePage(long pageNo, long pageSize, String apiCode) {
        QueryWrapper<ApiSchedule> qw = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(apiCode)) {
            qw.eq("api_code", apiCode);
        }
        qw.orderByDesc("create_time");
        return scheduleMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public ApiSchedule scheduleSave(String id, String apiCode, String cron, Integer enabled, String remark, String username) {
        if (StringUtil.isEmpty(apiCode)) {
            throw new IllegalArgumentException("接口编码不能为空！");
        }
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("cron 表达式不能为空！");
        }
        CronExpression expression;
        try {
            expression = CronExpression.parse(cron.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cron 表达式不合法（Spring 6 位格式，如 0 0/30 * * * *）！");
        }
        ApiSchedule row;
        LocalDateTime now = LocalDateTime.now();
        if (StringUtil.isNotEmpty(id)) {
            row = scheduleMapper.selectById(id);
            if (row == null) {
                throw new IllegalArgumentException("定时配置不存在或已被删除！");
            }
        } else {
            row = new ApiSchedule();
            row.setCreateBy(username);
            row.setCreateTime(now);
        }
        //校验接口存在
        Api api = this.getOne(new QueryWrapper<Api>().eq("api_code", apiCode));
        if (api == null) {
            throw new IllegalArgumentException("接口不存在：" + apiCode);
        }
        row.setApiCode(apiCode);
        row.setCron(cron.trim());
        row.setEnabled(enabled == null || enabled == 1 ? 1 : 0);
        row.setRemark(remark);
        row.setNextRunTime(expression.next(now));
        row.setUpdateBy(username);
        row.setUpdateTime(now);
        if (row.getId() == null) {
            scheduleMapper.insert(row);
        } else {
            scheduleMapper.updateById(row);
        }
        return row;
    }

    @Override
    public void scheduleDelete(String id, String username) {
        if (scheduleMapper.selectById(id) == null) {
            throw new IllegalArgumentException("定时配置不存在或已被删除！");
        }
        scheduleMapper.deleteById(id);
    }

    // ---------------- 采集数据 ----------------

    @Override
    public Page<ApiData> dataPage(long pageNo, long pageSize, String apiCode) {
        QueryWrapper<ApiData> qw = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(apiCode)) {
            qw.eq("api_code", apiCode);
        }
        qw.orderByDesc("create_time");
        return dataMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    @Override
    public void dataDelete(String id, String username) {
        if (dataMapper.selectById(id) == null) {
            throw new IllegalArgumentException("采集数据不存在！");
        }
        dataMapper.deleteById(id);
    }

    /**
     * 供调度任务批量取到期任务
     */
    public List<ApiSchedule> listDueSchedules(LocalDateTime now) {
        return scheduleMapper.selectList(new QueryWrapper<ApiSchedule>()
                .eq("enabled", 1).le("next_run_time", now));
    }
}
