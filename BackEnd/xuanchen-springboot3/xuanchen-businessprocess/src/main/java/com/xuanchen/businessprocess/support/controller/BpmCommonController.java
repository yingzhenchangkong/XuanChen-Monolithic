package com.xuanchen.businessprocess.support.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.service.IAuthServiceCommon;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.businessprocess.definition.entity.BpmDefinition;
import com.xuanchen.businessprocess.definition.entity.BpmNode;
import com.xuanchen.businessprocess.definition.entity.BpmNodeRule;
import com.xuanchen.businessprocess.definition.mapper.BpmNodeMapper;
import com.xuanchen.businessprocess.definition.mapper.BpmNodeRuleMapper;
import com.xuanchen.businessprocess.definition.service.IBpmDefinitionService;
import com.xuanchen.businessprocess.support.entity.BpmDelegate;
import com.xuanchen.businessprocess.support.mapper.BpmSupportMapper;
import com.xuanchen.businessprocess.support.service.IBpmDelegateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制器-->工作流公共接口（登录即可）
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@RestController
@RequestMapping("/businessprocess/common")
public class BpmCommonController {

    @Autowired
    private BpmSupportMapper bpmSupportMapper;
    @Autowired
    private IBpmDefinitionService definitionService;
    @Autowired
    private BpmNodeMapper nodeMapper;
    @Autowired
    private BpmNodeRuleMapper ruleMapper;
    @Autowired
    private IBpmDelegateService delegateService;
    @Autowired
    private IAuthServiceCommon authServiceCommon;

    /**
     * 启用账号下拉（用户名 + 昵称；发起人自选审批人、角色规则值参考用）
     */
    @GetMapping("/userSelect")
    public Result<List<Map<String, Object>>> userSelect() {
        return Result.success(bpmSupportMapper.listEnabledAccounts());
    }

    /**
     * 发起页辅助信息：需要"发起人自选审批人"的节点列表（仅暴露节点 key/名称，不下发审批人规则）
     */
    @GetMapping("/chooserNodes")
    public Result<List<Map<String, Object>>> chooserNodes(@RequestParam(name = "definitionId") String definitionId) {
        BpmDefinition definition = definitionService.getById(definitionId);
        if (definition == null || definition.getStatus() == null || definition.getStatus() != 1) {
            return Result.badRequest("流程定义不存在或未启用！");
        }
        Map<String, String> nameMap = new HashMap<>();
        List<BpmNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<BpmNode>()
                .eq(BpmNode::getDefinitionId, definitionId));
        if (nodes != null) {
            nodes.forEach(n -> nameMap.put(n.getNodeKey(), n.getNodeName()));
        }
        List<BpmNodeRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<BpmNodeRule>()
                .eq(BpmNodeRule::getDefinitionId, definitionId)
                .eq(BpmNodeRule::getRuleType, 3));
        List<Map<String, Object>> data = new ArrayList<>();
        if (rules != null) {
            for (BpmNodeRule rule : rules) {
                // 节点不存在（脏规则）时跳过
                if (!nameMap.containsKey(rule.getNodeKey())) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("key", rule.getNodeKey());
                item.put("name", nameMap.get(rule.getNodeKey()));
                data.add(item);
            }
        }
        return Result.success(data);
    }

    // ==================== 委托/代理审批 ====================

    /**
     * 我的委托规则
     */
    @GetMapping("/delegate/list")
    public Result<List<BpmDelegate>> delegateList(HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        return Result.success(delegateService.listMine(userName));
    }

    /**
     * 新增委托规则（被委托人需存在且启用，不能委托给自己）
     */
    @PostMapping("/delegate/add")
    public Result<String> delegateAdd(@RequestBody BpmDelegate body, HttpServletRequest request) {
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        if (StringUtil.isEmpty(body.getDelegateUserName())) {
            return Result.badRequest("请选择被委托人！");
        }
        if (body.getDelegateUserName().equals(userName)) {
            return Result.badRequest("不能委托给自己！");
        }
        if (bpmSupportMapper.countEnabledAccount(body.getDelegateUserName()) <= 0) {
            return Result.badRequest("被委托人不存在或已停用！");
        }
        if (body.getStartTime() != null && body.getEndTime() != null
                && body.getEndTime().isBefore(body.getStartTime())) {
            return Result.badRequest("结束时间不能早于开始时间！");
        }
        body.setId(null);
        body.setUserName(userName);
        body.setStatus(1);
        body.setCreateBy(userName);
        body.setCreateTime(java.time.LocalDateTime.now());
        delegateService.save(body);
        return Result.success("委托规则已生效!");
    }

    /**
     * 委托规则启用/停用
     */
    @PostMapping("/delegate/changeStatus")
    public Result<String> delegateChangeStatus(@RequestBody BpmDelegate body, HttpServletRequest request) {
        if (body.getId() == null || body.getId().isBlank()) {
            return Result.badRequest("缺少委托规则ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        BpmDelegate delegate = delegateService.getById(body.getId());
        if (delegate == null || !userName.equals(delegate.getUserName())) {
            return Result.badRequest("委托规则不存在！");
        }
        delegate.setStatus(body.getStatus() != null && body.getStatus() == 1 ? 1 : 0);
        delegateService.updateById(delegate);
        return Result.success("已更新!");
    }

    /**
     * 删除委托规则
     */
    @PostMapping("/delegate/delete")
    public Result<String> delegateDelete(@RequestBody BpmDelegate body, HttpServletRequest request) {
        if (body.getId() == null || body.getId().isBlank()) {
            return Result.badRequest("缺少委托规则ID！");
        }
        String userName = authServiceCommon.getUserNameByToken(request.getHeader("XC-ACCESS-TOKEN"));
        BpmDelegate delegate = delegateService.getById(body.getId());
        if (delegate == null || !userName.equals(delegate.getUserName())) {
            return Result.badRequest("委托规则不存在！");
        }
        delegateService.removeById(body.getId());
        return Result.success("已删除!");
    }
}
