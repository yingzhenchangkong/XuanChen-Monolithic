package com.xuanchen.report.rptreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.common.utils.StringUtil;
import com.xuanchen.report.rptreport.entity.RptReport;
import com.xuanchen.report.rptreport.mapper.RptReportMapper;
import com.xuanchen.report.rptreport.service.IRptReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service接口实现类-->报表
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Service
public class RptReportServiceImpl extends ServiceImpl<RptReportMapper, RptReport> implements IRptReportService {

    @Autowired
    private RptReportMapper rptReportMapper;

    @Override
    public IPage<RptReport> listReport(IPage<RptReport> page, RptReport report) {
        return rptReportMapper.listReport(page, report);
    }

    @Override
    public boolean ifExistsCode(RptReport report) {
        QueryWrapper<RptReport> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("report_code", report.getReportCode());
        if (StringUtil.isNotEmpty(report.getId())) {
            queryWrapper.ne("id", report.getId());
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String copyReport(String id, String newCode, String newName, String userName) {
        RptReport source = this.getById(id);
        if (source == null) {
            throw new BusinessException(400, "源报表不存在或已被删除！");
        }
        RptReport copy = new RptReport();
        copy.setReportCode(newCode);
        copy.setReportName(newName);
        copy.setReportType(source.getReportType());
        copy.setTemplateId(source.getTemplateId());
        copy.setConfigJson(source.getConfigJson());
        copy.setStatus(0);
        copy.setRemark(source.getRemark());
        copy.setCreateBy(userName);
        copy.setCreateTime(LocalDateTime.now());
        this.save(copy);
        // 数据集与字段绑定挂在模板上：报表复制只需复制 configJson，无需复制数据集
        return copy.getId();
    }
}
