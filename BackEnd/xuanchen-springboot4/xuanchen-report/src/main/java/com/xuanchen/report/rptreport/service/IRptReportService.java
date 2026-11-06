package com.xuanchen.report.rptreport.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.report.rptreport.entity.RptReport;

/**
 * Service接口-->报表
 *
 * @author XuanChen
 * @date 2026-10-22
 */
public interface IRptReportService extends IService<RptReport> {

    /**
     * 分页列表（带创建/更新人姓名）
     *
     * @param page
     * @param report
     * @return
     */
    IPage<RptReport> listReport(IPage<RptReport> page, RptReport report);

    /**
     * 报表编码唯一性校验（排除自身 id）
     *
     * @param report
     * @return true=已存在
     */
    boolean ifExistsCode(RptReport report);

    /**
     * 复制报表（配置 JSON 原样复制；数据集与绑定随模板维护）
     *
     * @param id        源报表ID
     * @param newCode   新报表编码
     * @param newName   新报表名称
     * @param userName  操作人
     * @return 新报表ID
     */
    String copyReport(String id, String newCode, String newName, String userName);
}
