package com.xuanchen.system.sysdept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuanchen.system.sysdept.entity.SysDept;
import com.xuanchen.system.sysdept.entity.SysDeptTreeVO;
import com.xuanchen.system.sysdept.mapper.SysDeptMapper;
import com.xuanchen.system.sysdept.service.ISysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceImpl --> 部门
 *
 * @author XuanChen
 * @date 2025-11-12
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {
    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Override
    public List<SysDeptTreeVO> getDeptTree() {
        List<SysDept> listDept = this.list();
        return buildDeptTree(null, listDept);
    }

    @Override
    public List<SysDeptTreeVO> getChildDeptTree(String parentDeptCode) {
        // 获取某个部门下的所有子部门
        List<SysDept> listChildDepts = getAllChildDepts(parentDeptCode);
        return buildDeptTree(listChildDepts.get(0), listChildDepts);
    }

    @Override
    public String createDeptCode(String parentDeptCode) {
        QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
        if (parentDeptCode == null || parentDeptCode.isEmpty()) {
            queryWrapper.isNull("parent_dept_code");
            parentDeptCode = "XC";
        } else {
            queryWrapper.eq("parent_dept_code", parentDeptCode);
        }
        List<SysDept> listDepts = sysDeptMapper.selectList(queryWrapper);
        List<Integer> listDeptCodes = listDepts.stream()
                .map(SysDept::getDeptCode)
                .map(code -> code.substring(code.length() - 3))
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return 0; // 或者其他默认值
                    }
                })
                .sorted(Comparator.reverseOrder())  // 降序排列
                .toList();
        int nextCode = listDeptCodes.isEmpty() ? 1 : listDeptCodes.get(0) + 1;
        return parentDeptCode + String.format("%03d", nextCode);
    }

    /**
     * 获取指定父部门下的所有子部门（包括子孙部门）
     *
     * @param parentDeptCode 父部门编码
     * @return 所有子部门列表
     */
    public List<SysDept> getAllChildDepts(String parentDeptCode) {
        List<SysDept> allDepts = this.list(); // 获取所有部门
        List<SysDept> result = new ArrayList<>();
        QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_code", parentDeptCode);
        SysDept parentDept = sysDeptMapper.selectOne(queryWrapper);
        result.add(parentDept);
        findChildDeptsRecursive(parentDeptCode, allDepts, result);
        return result;
    }

    /**
     * 递归查找子部门
     *
     * @param parentDeptCode 父部门编码
     * @param allDepts       所有部门列表
     * @param result         结果集合
     */
    private void findChildDeptsRecursive(String parentDeptCode, List<SysDept> allDepts, List<SysDept> result) {
        // 查找直接子部门
        List<SysDept> directChildren = allDepts.stream()
                .filter(dept -> parentDeptCode.equals(dept.getParentDeptCode()))
                .toList();
        // 添加到结果中
        result.addAll(directChildren);
        // 递归查找孙子部门
        for (SysDept child : directChildren) {
            findChildDeptsRecursive(child.getDeptCode(), allDepts, result);
        }
    }


    /**
     * 构建部门树形结构
     *
     * @param deptList 部门列表
     * @return 树形结构节点列表
     */
    private List<SysDeptTreeVO> buildDeptTree(SysDept sysDept, List<SysDept> deptList) {
        List<SysDeptTreeVO> treeNodes = new ArrayList<>();
        List<SysDept> rootDepts = new ArrayList<>();
        if (sysDept == null) {
            // 找到根节点（parent_id为0或null的节点）
            rootDepts = deptList.stream()
                    .filter(dept -> dept.getParentDeptCode() == null || dept.getParentDeptCode().isEmpty())
                    .collect(Collectors.toList());
        } else {
            rootDepts = deptList.stream()
                    .filter(dept -> dept.getDeptCode().equals(sysDept.getDeptCode()))
                    .collect(Collectors.toList());
        }
        // 递归构建每个根节点的子树
        for (SysDept rootDept : rootDepts) {
            SysDeptTreeVO rootNode = buildDeptTreeNode(rootDept, deptList);
            treeNodes.add(rootNode);
        }
        return treeNodes;
    }

    /**
     * 递归构建部门树节点
     *
     * @param dept     当前部门
     * @param deptList 所有部门列表
     * @return 树节点
     */
    private SysDeptTreeVO buildDeptTreeNode(SysDept dept, List<SysDept> deptList) {
        SysDeptTreeVO node = new SysDeptTreeVO(dept.getDeptName(), dept.getDeptCode());
        // 查找当前部门的所有子部门
        List<SysDept> children = deptList.stream()
                .filter(d -> d.getParentDeptCode() != null && d.getParentDeptCode().equals(dept.getDeptCode()))
                .toList();
        // 如果有子部门，递归构建子节点
        if (!children.isEmpty()) {
            List<SysDeptTreeVO> childNodes = new ArrayList<>();
            for (SysDept child : children) {
                childNodes.add(buildDeptTreeNode(child, deptList));
            }
            node.setChildren(childNodes);
        }
        return node;
    }
}

