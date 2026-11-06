package com.xuanchen.generator.loggenerator.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.loggenerator.entity.LogGenerator;

import java.util.List;

/**
 * Service接口-->代码生成记录
 *
 * @author XuanChen
 * @date 2026-10-19
 */
public interface ILogGeneratorService extends IService<LogGenerator> {

    /**
     * 记录一次生成（成功/失败都记）。
     *
     * @param table       表配置
     * @param generateType 生成方式（DISK/ZIP/PREVIEW）
     * @param template     模板
     * @param files        本次写出的文件（相对路径）
     * @param success      是否成功
     * @param message      结果说明/失败原因
     */
    void record(GenTable table, String generateType, String template, List<String> files,
                boolean success, String message);

    /**
     * 按表配置分页查询生成记录（时间倒序）。
     *
     * @param tableId  表配置 id
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<LogGenerator> pageByTable(String tableId, Integer pageNo, Integer pageSize);

    /**
     * 按表配置清空生成记录。
     *
     * @param tableId 表配置 id
     */
    void clearByTable(String tableId);

    /**
     * 系统监控用分页查询（不限定表配置，支持多条件筛选，生成时间倒序）。
     *
     * @param query    查询条件（tableName/tableComment/moduleName/generateType/createBy 模糊或精确、
     *                 success 精确、beginTime~endTime 时间区间）
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<LogGenerator> pageForMonitor(LogGenerator query, Integer pageNo, Integer pageSize);
}
