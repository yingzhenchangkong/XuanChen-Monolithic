package com.xuanchen.generator.genprofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.genprofile.entity.GenProjectProfile;
import com.xuanchen.generator.genprofile.mapper.GenProjectProfileMapper;
import com.xuanchen.generator.genprofile.service.IGenProjectProfileService;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->代码生成项目预设
 *
 * @author XuanChen
 * @date 2026-10-20
 */
@Service
public class GenProjectProfileServiceImpl extends ServiceImpl<GenProjectProfileMapper, GenProjectProfile>
        implements IGenProjectProfileService {

    /** 预设名称最大长度，与 gen_project_profile.profile_name 列宽保持一致 */
    private static final int MAX_NAME_LENGTH = 64;

    private final GeneratorSafetyValidator validator;

    public GenProjectProfileServiceImpl(GeneratorSafetyValidator validator) {
        this.validator = validator;
    }

    @Override
    public List<GenProjectProfile> listAll() {
        return list(new QueryWrapper<GenProjectProfile>()
                .orderByAsc("order_no")
                .orderByDesc("create_time"));
    }

    @Override
    public String saveProfile(GenProjectProfile profile) {
        if (profile == null) {
            throw new BusinessException(400, "缺少预设信息！");
        }
        String name = profile.getProfileName() == null ? "" : profile.getProfileName().trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "预设名称不能为空！");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(400, "预设名称不能超过 " + MAX_NAME_LENGTH + " 个字符！");
        }
        profile.setProfileName(name);

        boolean isAdd = profile.getId() == null || profile.getId().isBlank();
        // 同名校验：下拉里出现两条同名预设，用户根本分不清该选哪个
        QueryWrapper<GenProjectProfile> sameName = new QueryWrapper<GenProjectProfile>()
                .eq("profile_name", name);
        if (!isAdd) {
            sameName.ne("id", profile.getId());
        }
        if (count(sameName) > 0) {
            throw new BusinessException(400, "预设名称已存在：" + name);
        }

        validator.validateOptionalIdentifier(profile.getModuleName(), "模块名");
        validator.validateOptionalIdentifier(profile.getPackageName(), "包名");
        validator.validateRelativeOutputPath(profile.getBackendRelPath(), "后端相对路径");
        validator.validateRelativeOutputPath(profile.getFrontendRelPath(), "前端相对路径");

        if (profile.getTemplate() == null || profile.getTemplate().isBlank()) {
            profile.setTemplate("one2many");
        }
        if (profile.getParentMenuId() == null || profile.getParentMenuId().isBlank()) {
            profile.setParentMenuId("0");
        }
        if (profile.getOrderNo() == null) {
            profile.setOrderNo(0);
        }

        if (isAdd) {
            save(profile);
        } else {
            updateById(profile);
        }
        return profile.getId();
    }
}
