package com.xuanchen.generator.genprofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.common.constant.TipConst;
import com.xuanchen.common.entity.Result;
import com.xuanchen.generator.genprofile.entity.GenProjectProfile;
import com.xuanchen.generator.genprofile.service.IGenProjectProfileService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 控制器 --> 代码生成项目预设
 * <p>
 * 预设把「数据源 + 输出目录 + 相对路径 + 包名/模块名 + 模板」存成一条记录，
 * 表配置页选一下即可带出，省掉每次重复填写。
 *
 * @author XuanChen
 * @date 2026-10-20
 */
@RestController
@RequiresRoles("admin")
@RequestMapping("/tool/generator/profile")
public class GenProjectProfileController {

    @Autowired
    private IGenProjectProfileService genProjectProfileService;

    /**
     * 预设分页列表（独立「项目预设」页面使用）
     *
     * @param profile    查询条件（profileName 模糊）
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<IPage<GenProjectProfile>> list(GenProjectProfile profile,
                                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        QueryWrapper<GenProjectProfile> wrapper = new QueryWrapper<>();
        if (profile != null && profile.getProfileName() != null && !profile.getProfileName().isBlank()) {
            wrapper.like("profile_name", profile.getProfileName().trim());
        }
        wrapper.orderByAsc("order_no").orderByDesc("create_time");
        return Result.success(genProjectProfileService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    /**
     * 下拉选项：全部预设（表配置页「项目预设」用）
     *
     * @return 预设列表
     */
    @GetMapping("/select")
    public Result<List<GenProjectProfile>> select() {
        return Result.success(genProjectProfileService.listAll());
    }

    /**
     * 新增预设
     *
     * @param profile 预设
     * @return 预设主键
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody GenProjectProfile profile) {
        profile.setId(null);
        return Result.success(TipConst.ADD_SUCC, genProjectProfileService.saveProfile(profile));
    }

    /**
     * 修改预设
     *
     * @param profile 预设
     * @return 预设主键
     */
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody GenProjectProfile profile) {
        if (profile.getId() == null || profile.getId().isBlank()) {
            return Result.badRequest("缺少预设ID！");
        }
        return Result.success(TipConst.EDIT_SUCC, genProjectProfileService.saveProfile(profile));
    }

    /**
     * 删除预设（逻辑删除）
     *
     * @param id 主键
     * @return 结果
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        genProjectProfileService.removeById(id);
        return Result.success(TipConst.DEL_SUCC);
    }
}
