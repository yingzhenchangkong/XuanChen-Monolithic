package com.xuanchen.masterdata.mddept.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.masterdata.mddept.entity.MdDept;
import com.xuanchen.masterdata.mddept.entity.MdDeptTreeVO;
import com.xuanchen.masterdata.mddept.service.IMdDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class MdDeptController {
    private final IMdDeptService mdDeptService;

    /**
     * 添加
     *
     * @param mdDept
     * @return
     */
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody MdDept mdDept) {
        mdDeptService.save(mdDept);
        return Result.success(TipConst.ADD_SUCC);
    }

    /**
     * 修改
     *
     * @param mdDept
     * @return
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody MdDept mdDept) {
        mdDeptService.updateById(mdDept);
        return Result.success(TipConst.EDIT_SUCC);
    }

    /**
     * 通过id删除
     *
     * @param deptCode
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "deptCode", required = true) String deptCode) {
        QueryWrapper<MdDept> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("dept_code", deptCode);
        mdDeptService.remove(queryWrapper);
        return Result.success(TipConst.DEL_SUCC);
    }

    /**
     * 获取部门树结构数据
     *
     * @return 部门树节点列表
     */
    @GetMapping("/getDeptTree")
    public List<MdDeptTreeVO> getDeptTree() {
        return mdDeptService.getDeptTree();
    }

    /**
     * 获取子部门树结构数据
     *
     * @param parentDeptCode 父部门编码
     * @return 子部门树节点列表
     */
    @GetMapping("/getChildDeptTree")
    public List<MdDeptTreeVO> getChildDeptTree(String parentDeptCode) {
        return mdDeptService.getChildDeptTree(parentDeptCode);
    }

    /**
     * 获取选中的部门信息
     *
     * @param deptCode
     * @return
     */
    @GetMapping("/getSelectedDept")
    public MdDept getSelectedDept(String deptCode) {
        QueryWrapper<MdDept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_code", deptCode);
        return mdDeptService.getOne(queryWrapper);
    }

    /**
     * 创建部门编码
     *
     * @param map
     * @return
     */
    @PostMapping("/createDeptCode")
    public String createDeptCode(@RequestBody Map<String, Object> map) {
        return mdDeptService.createDeptCode(map.get("parentDeptCode").toString());
    }

}
