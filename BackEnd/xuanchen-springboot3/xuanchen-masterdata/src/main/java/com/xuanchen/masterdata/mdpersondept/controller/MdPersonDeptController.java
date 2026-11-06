package com.xuanchen.masterdata.mdpersondept.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.entity.Result;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.masterdata.mddept.entity.MdDept;
import com.xuanchen.masterdata.mddept.service.IMdDeptService;
import com.xuanchen.masterdata.mdperson.entity.MdPerson;
import com.xuanchen.masterdata.mdperson.mapper.MdPersonMapper;
import com.xuanchen.masterdata.mdpersondept.entity.MdPersonDept;
import com.xuanchen.masterdata.mdpersondept.service.IMdPersonDeptService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 控制器-->用户 部门 对应关系
 *
 * @author XuanChen
 * @date 2025-12-04
 */
@RestController
@RequiresRoles("admin")
@RequestMapping("/system/persondept")
public class MdPersonDeptController {
    @Autowired
    private IMdPersonDeptService mdPersonDeptService;
    @Autowired
    private IMdDeptService mdDeptService;
    @Autowired
    private MdPersonMapper mdPersonMapper;

    /**
     * 部门已授权用户
     *
     * @param mdPersonDept
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping("/listDeptUser")
    public Result<IPage<MdPersonDept>> listDeptUser(MdPersonDept mdPersonDept,
                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                               HttpServletRequest req) {
        Page<MdPersonDept> page = new Page<>(pageNo, pageSize);
        IPage<MdPersonDept> pageList = mdPersonDeptService.listDeptUser(page, mdPersonDept.getDeptCode());
        return Result.success(pageList);
    }

    /**
     * 为某部门关联人员（数据来源人员表 md_person，前端传人员ID集合）
     *
     * @param mdPersonDept
     * @return
     */
    @PostMapping("/link")
    public Result<String> link(@RequestBody MdPersonDept mdPersonDept) {
        StringBuilder linkedUser = new StringBuilder();
        StringBuilder linkSuccUser = new StringBuilder();
        List<String> missingPersons = new ArrayList<>();
        List<MdPersonDept> list = new ArrayList<>();
        QueryWrapper<MdDept> qwDept = new QueryWrapper<>();
        qwDept.eq("dept_code", mdPersonDept.getDeptCode());
        MdDept mdDept = mdDeptService.getOne(qwDept);
        if (mdDept == null) {
            return Result.badRequest("部门不存在！");
        }
        for (String personId : mdPersonDept.getPersonIds()) {
            MdPerson profile = mdPersonMapper.selectById(personId);
            if (profile == null) {
                missingPersons.add(personId);
                continue;
            }
            MdPersonDept mdPersonDeptTemp = new MdPersonDept();
            mdPersonDeptTemp.setPersonId(personId);
            mdPersonDeptTemp.setDeptId(mdDept.getDeptCode());
            QueryWrapper<MdPersonDept> qwUserDept = new QueryWrapper<>();
            qwUserDept.eq("person_id", mdPersonDeptTemp.getPersonId());
            qwUserDept.eq("dept_id", mdPersonDeptTemp.getDeptId());
            //人员维度展示名用真实姓名（档案无姓名时回退人员ID）
            String realName = StringUtil.isNotEmpty(profile.getRealName())
                    ? profile.getRealName() : personId;
            if (mdPersonDeptService.count(qwUserDept) > 0) {
                linkedUser.append(realName).append(",");
            } else {
                linkSuccUser.append(realName).append(",");
                list.add(mdPersonDeptTemp);
            }
        }
        mdPersonDeptService.saveBatch(list);
        String message = "";
        if (!linkedUser.isEmpty()) {
            linkedUser = new StringBuilder(linkedUser.substring(0, linkedUser.length() - 1));
        }
        if (!linkSuccUser.isEmpty()) {
            linkSuccUser = new StringBuilder(linkSuccUser.substring(0, linkSuccUser.length() - 1));
        }
        if ((!linkedUser.isEmpty()) && (linkSuccUser.isEmpty())) {
            message = linkedUser + "已关联!无人员关联成功!";
        } else if ((linkedUser.isEmpty()) && (!linkSuccUser.isEmpty())) {
            message = linkSuccUser + "关联成功!";
        } else if (!linkedUser.isEmpty()) {
            message = linkedUser + "已关联," + linkSuccUser + "关联成功!";
        }
        if (!missingPersons.isEmpty()) {
            String tip = String.join(",", missingPersons) + " 人员不存在，无法关联部门!";
            message = message.isEmpty() ? tip : message + "；" + tip;
        }
        return Result.success(message);
    }

    /**
     * 为某部门取消关联用户
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/unlink")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        mdPersonDeptService.removeById(id);
        return Result.success("取消关联成功!");
    }
}
