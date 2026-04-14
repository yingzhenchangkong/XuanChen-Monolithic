package com.xuanchen.system.sysdept.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.system.sysdept.entity.SysDept;
import com.xuanchen.system.sysdept.entity.SysDeptTreeVO;
import com.xuanchen.system.sysdept.service.ISysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 控制器 --> 部门
 *
 * @author XuanChen
 * @date 2025-11-12
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {
    @Autowired
    private ISysDeptService sysDeptService;

    /**
     * 添加
     *
     * @param sysDept
     * @return
     */
    @PostMapping(value = "/add")
    public Result add(@RequestBody SysDept sysDept) {
        sysDeptService.save(sysDept);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param sysDept
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result edit(@RequestBody SysDept sysDept) {
        sysDeptService.updateById(sysDept);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param deptCode
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result delete(@RequestParam(name = "deptCode", required = true) String deptCode) {
        QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("dept_code", deptCode);
        sysDeptService.remove(queryWrapper);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 获取部门树结构数据
     *
     * @return 部门树节点列表
     */
    @GetMapping("/getDeptTree")
    public List<SysDeptTreeVO> getDeptTree() {
        return sysDeptService.getDeptTree();
    }

    /**
     * 获取子部门树结构数据
     *
     * @param parentDeptCode 父部门编码
     * @return 子部门树节点列表
     */
    @GetMapping("/getChildDeptTree")
    public List<SysDeptTreeVO> getChildDeptTree(String parentDeptCode) {
        return sysDeptService.getChildDeptTree(parentDeptCode);
    }

    /**
     * 获取选中的部门信息
     *
     * @param deptCode
     * @return
     */
    @GetMapping("/getSelectedDept")
    public SysDept getSelectedDept(String deptCode) {
        QueryWrapper<SysDept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_code", deptCode);
        return sysDeptService.getOne(queryWrapper);
    }

    /**
     * 创建部门编码
     *
     * @param map
     * @return
     */
    @PostMapping("/createDeptCode")
    public String createDeptCode(@RequestBody Map<String, Object> map) {
        return sysDeptService.createDeptCode(map.get("parentDeptCode").toString());
    }

}
