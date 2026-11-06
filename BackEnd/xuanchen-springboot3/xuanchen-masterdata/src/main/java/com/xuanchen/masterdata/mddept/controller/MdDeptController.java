package com.xuanchen.masterdata.mddept.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.masterdata.mddept.entity.MdDept;
import com.xuanchen.masterdata.mddept.entity.MdDeptTreeVO;
import com.xuanchen.masterdata.mddept.service.IMdDeptService;
import org.apache.shiro.authz.annotation.RequiresRoles;
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
@RequiresRoles("admin")
@RequestMapping("/system/dept")
public class MdDeptController {
    @Autowired
    private IMdDeptService mdDeptService;

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
        if (StringUtil.isEmpty(mdDept.getId())) {
            return Result.badRequest("缺少部门ID！");
        }
        // 影响行数为 0（id 不存在或已被删除）必须返回失败，避免前端误判成功
        if (!mdDeptService.updateById(mdDept)) {
            return Result.badRequest("部门不存在或已被删除！");
        }
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
