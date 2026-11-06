package com.xuanchen.report.rptreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xuanchen.report.rptreport.entity.RptReport;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * Mapper-->报表
 *
 * @author XuanChen
 * @date 2026-10-22
 */
@Repository
public interface RptReportMapper extends BaseMapper<RptReport> {

    /**
     * 分页列表（带创建人/更新人姓名联查）
     *
     * @param page
     * @param report
     * @return
     */
    IPage<RptReport> listReport(@Param("page") IPage<RptReport> page, @Param("report") RptReport report);
}
