package com.xuanchen.businessprocess.instance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.utils.JsonUtils;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.businessprocess.definition.entity.BpmDefinition;
import com.xuanchen.businessprocess.definition.service.IBpmDefinitionService;
import com.xuanchen.businessprocess.engine.BpmEngine;
import com.xuanchen.businessprocess.instance.dto.BpmSubmitDTO;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;
import com.xuanchen.businessprocess.instance.mapper.BpmInstanceMapper;
import com.xuanchen.businessprocess.instance.service.IBpmInstanceService;
import com.xuanchen.businessprocess.record.entity.BpmRecord;
import com.xuanchen.businessprocess.record.mapper.BpmRecordMapper;
import com.xuanchen.businessprocess.support.mapper.BpmSupportMapper;
import com.xuanchen.businessprocess.task.entity.BpmTask;
import com.xuanchen.businessprocess.task.mapper.BpmTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service-->审批流程实例
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Service
public class BpmInstanceServiceImpl extends ServiceImpl<BpmInstanceMapper, BpmInstance>
        implements IBpmInstanceService {

    @Autowired
    private BpmEngine engine;
    @Autowired
    private IBpmDefinitionService definitionService;
    @Autowired
    private BpmRecordMapper recordMapper;
    @Autowired
    private BpmTaskMapper taskMapper;
    @Autowired
    private BpmSupportMapper supportMapper;

    @Override
    public String submit(String userName, BpmSubmitDTO dto) {
        if (StringUtil.isEmpty(dto.getDefinitionId())) {
            return "缺少流程定义ID！";
        }
        if (StringUtil.isEmpty(dto.getBusinessSummary())) {
            return "申请摘要不能为空！";
        }
        BpmDefinition definition = definitionService.getById(dto.getDefinitionId());
        if (definition == null) {
            return "流程定义不存在或已被删除！";
        }
        if (definition.getStatus() == null || definition.getStatus() != 1) {
            return "该流程未启用，不能发起申请！";
        }
        BpmInstance instance = new BpmInstance();
        instance.setDefinitionId(definition.getId());
        instance.setDefinitionCode(definition.getCode());
        instance.setDefinitionName(definition.getName());
        instance.setVersion(definition.getVersion() == null ? 1 : definition.getVersion());
        instance.setBusinessType(definition.getBusinessType());
        instance.setBusinessId(dto.getBusinessId());
        instance.setBusinessSummary(dto.getBusinessSummary());
        instance.setFormData(dto.getFormData() == null ? null : JsonUtils.toJson(dto.getFormData()));
        engine.createAndStart(instance, definition.getBpmnXml(), userName, dto.getChosenAssignees());
        return null;
    }

    @Override
    public IPage<BpmInstance> listMine(IPage<BpmInstance> page, String userName, Integer status, String keyword) {
        LambdaQueryWrapper<BpmInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmInstance::getApplyUser, userName);
        if (status != null) {
            wrapper.eq(BpmInstance::getStatus, status);
        }
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.and(w -> w.like(BpmInstance::getBusinessSummary, keyword)
                    .or().like(BpmInstance::getDefinitionName, keyword));
        }
        wrapper.orderByDesc(BpmInstance::getCreateTime);
        // 列表不返回大字段
        wrapper.select(BpmInstance.class,
                i -> !"form_data".equals(i.getColumn()) && !"graph_snapshot".equals(i.getColumn()));
        IPage<BpmInstance> result = this.page(page, wrapper);
        fillNickNames(result.getRecords().stream().map(BpmInstance::getApplyUser).distinct().toList(),
                result.getRecords(), BpmInstance::getApplyUser, BpmInstance::setApplyUserName);
        return result;
    }

    @Override
    public Map<String, Object> records(String instanceId) {
        BpmInstance instance = this.getById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在！");
        }
        List<BpmRecord> records = recordMapper.selectList(new LambdaQueryWrapper<BpmRecord>()
                .eq(BpmRecord::getInstanceId, instanceId).orderByAsc(BpmRecord::getCreateTime));
        List<BpmRecord> safeRecords = records == null ? new ArrayList<>() : records;
        fillNickNames(safeRecords.stream().map(BpmRecord::getOperator).distinct().toList(),
                safeRecords, BpmRecord::getOperator, BpmRecord::setOperatorName);
        Map<String, Object> data = new HashMap<>();
        data.put("instance", instance);
        data.put("records", safeRecords);
        return data;
    }

    @Override
    public Map<String, Object> diagramState(String instanceId) {
        BpmInstance instance = this.getById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在！");
        }
        BpmDefinition definition = definitionService.getById(instance.getDefinitionId());
        // 已走节点 = 产生过记录的节点；当前节点 = 实例 current_node_key
        List<BpmRecord> records = recordMapper.selectList(new LambdaQueryWrapper<BpmRecord>()
                .eq(BpmRecord::getInstanceId, instanceId));
        Set<String> passedNodes = new HashSet<>();
        if (records != null) {
            for (BpmRecord record : records) {
                if (StringUtil.isNotEmpty(record.getNodeKey())) {
                    passedNodes.add(record.getNodeKey());
                }
            }
        }
        List<BpmTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<BpmTask>()
                .eq(BpmTask::getInstanceId, instanceId).eq(BpmTask::getStatus, BpmEngine.TASK_PENDING));
        Map<String, Object> data = new HashMap<>();
        data.put("bpmnXml", definition == null ? null : definition.getBpmnXml());
        data.put("currentNodeKey", instance.getCurrentNodeKey());
        data.put("passedNodeKeys", passedNodes);
        data.put("pendingNodeKeys", tasks == null ? new ArrayList<>()
                : tasks.stream().map(BpmTask::getNodeKey).distinct().toList());
        data.put("status", instance.getStatus());
        return data;
    }

    @Override
    public String revoke(String userName, String instanceId) {
        return engine.revoke(userName, instanceId);
    }

    @Override
    public String urge(String userName, String instanceId) {
        return engine.urge(userName, instanceId);
    }

    /** 登录名 → 昵称回填（通用反射取值方法引用写法） */
    private <T> void fillNickNames(List<String> userNames, List<T> records,
                                   java.util.function.Function<T, String> getter,
                                   java.util.function.BiConsumer<T, String> setter) {
        if (records == null || records.isEmpty() || userNames == null || userNames.isEmpty()) {
            return;
        }
        List<Map<String, Object>> pairs = supportMapper.mapNickNames(new ArrayList<>(new HashSet<>(userNames)));
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
        for (T record : records) {
            String userName = getter.apply(record);
            if (userName != null && nameMap.containsKey(userName)) {
                setter.accept(record, nameMap.get(userName));
            }
        }
    }
}
