package com.xuanchen.businessprocess.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.businessprocess.engine.BpmEngine;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;
import com.xuanchen.businessprocess.instance.mapper.BpmInstanceMapper;
import com.xuanchen.businessprocess.support.mapper.BpmSupportMapper;
import com.xuanchen.businessprocess.task.entity.BpmTask;
import com.xuanchen.businessprocess.task.mapper.BpmTaskMapper;
import com.xuanchen.businessprocess.task.service.IBpmTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Service-->审批任务
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class BpmTaskServiceImpl extends ServiceImpl<BpmTaskMapper, BpmTask> implements IBpmTaskService {

    @Autowired
    private BpmEngine engine;
    @Autowired
    private BpmInstanceMapper instanceMapper;
    @Autowired
    private BpmSupportMapper supportMapper;

    @Override
    public IPage<BpmTask> listTodo(IPage<BpmTask> page, String userName, String keyword) {
        IPage<BpmTask> result = listByAssignee(page, userName, BpmEngine.TASK_PENDING, keyword, null);
        fillSummaryAndNames(result.getRecords());
        return result;
    }

    @Override
    public IPage<BpmTask> listDone(IPage<BpmTask> page, String userName, String keyword) {
        IPage<BpmTask> result = listByAssignee(page, userName, BpmEngine.TASK_HANDLED, keyword, null);
        fillSummaryAndNames(result.getRecords());
        return result;
    }

    private IPage<BpmTask> listByAssignee(IPage<BpmTask> page, String userName, Integer status,
                                         String keyword, Integer resultFilter) {
        // 需按业务摘要模糊过滤：先查符合条件的实例ID集合
        Set<String> matchedIds = new HashSet<>();
        if (StringUtil.isNotEmpty(keyword)) {
            List<BpmInstance> matched = instanceMapper.selectList(new LambdaQueryWrapper<BpmInstance>()
                    .like(BpmInstance::getBusinessSummary, keyword)
                    .or().like(BpmInstance::getDefinitionName, keyword)
                    .select(BpmInstance::getId));
            if (matched != null) {
                matched.forEach(i -> matchedIds.add(i.getId()));
            }
            if (matchedIds.isEmpty()) {
                return page;
            }
        }
        final Set<String> instanceIds = matchedIds;
        LambdaQueryWrapper<BpmTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmTask::getAssignee, userName).eq(BpmTask::getStatus, status);
        if (resultFilter != null) {
            wrapper.eq(BpmTask::getResult, resultFilter);
        }
        // 仅在有关键词过滤结果时追加 IN 条件（空集合 in() 会生成非法 SQL "IN ()"）
        if (!instanceIds.isEmpty()) {
            wrapper.in(BpmTask::getInstanceId, instanceIds);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.and(w -> w.like(BpmTask::getNodeName, keyword));
        }
        wrapper.orderByDesc(BpmTask::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public String approve(String userName, String taskId, String opinion) {
        return engine.approve(userName, taskId, opinion);
    }

    @Override
    public String reject(String userName, String taskId, String opinion) {
        return engine.reject(userName, taskId, opinion);
    }

    @Override
    public String transfer(String userName, String taskId, String targetUser, String opinion) {
        return engine.transfer(userName, taskId, targetUser, opinion);
    }

    @Override
    public String rejectToNode(String userName, String taskId, String targetNodeKey, String opinion) {
        return engine.rejectToNode(userName, taskId, targetNodeKey, opinion);
    }

    @Override
    public List<Map<String, String>> rejectTargets(String taskId) {
        return engine.rejectTargets(taskId);
    }

    /** 回填：审批人昵称 + 业务摘要 */
    private void fillSummaryAndNames(List<BpmTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<String> instanceIds = new HashSet<>();
        Set<String> assignees = new HashSet<>();
        tasks.forEach(t -> {
            instanceIds.add(t.getInstanceId());
            assignees.add(t.getAssignee());
        });
        Map<String, String> summaryMap = new HashMap<>();
        List<BpmInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<BpmInstance>()
                .in(BpmInstance::getId, instanceIds)
                .select(BpmInstance::getId, BpmInstance::getBusinessSummary));
        if (instances != null) {
            instances.forEach(i -> summaryMap.put(i.getId(), i.getBusinessSummary()));
        }
        List<Map<String, Object>> pairs = supportMapper.mapNickNames(new ArrayList<>(assignees));
        Map<String, String> nameMap = new HashMap<>();
        if (pairs != null) {
            for (Map<String, Object> pair : pairs) {
                Object userName = pair.get("userName");
                Object nickName = pair.get("nickName");
                if (userName != null) {
                    nameMap.put(userName.toString(),
                            nickName == null ? userName.toString() : nickName.toString());
                }
            }
        }
        for (BpmTask task : tasks) {
            task.setBusinessSummary(summaryMap.get(task.getInstanceId()));
            if (nameMap.containsKey(task.getAssignee())) {
                task.setAssigneeName(nameMap.get(task.getAssignee()));
            }
        }
    }
}
