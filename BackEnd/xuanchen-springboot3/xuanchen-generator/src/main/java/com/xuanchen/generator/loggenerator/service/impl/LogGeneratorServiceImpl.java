package com.xuanchen.generator.loggenerator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.loggenerator.entity.LogGenerator;
import com.xuanchen.generator.loggenerator.mapper.LogGeneratorMapper;
import com.xuanchen.generator.loggenerator.service.ILogGeneratorService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service接口实现类-->代码生成记录
 * <p>
 * create_by / create_time 不在本类手工赋值：全局
 * {@code MybatisInterceptor} 会在 INSERT 时按当前登录上下文自动填充审计字段，
 * 这也是项目内所有实体的统一约定。如此本类无需感知 Shiro（sb3）与
 * Spring Security（sb4）的差异，两版源码可保持完全一致。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
@Service
public class LogGeneratorServiceImpl extends ServiceImpl<LogGeneratorMapper, LogGenerator>
        implements ILogGeneratorService {

    /** file_list 为 text，留出余量避免超长写入失败 */
    private static final int MAX_FILE_LIST_LENGTH = 20000;
    /** message 列长度 */
    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Override
    public void record(GenTable table, String generateType, String template, List<String> files,
                       boolean success, String message) {
        try {
            LogGenerator log = new LogGenerator();
            log.setTableId(table == null ? null : table.getId());
            log.setTableName(table == null ? null : table.getTableName());
            log.setTableComment(table == null ? null : table.getTableComment());
            log.setModuleName(table == null ? null : table.getModuleName());
            log.setPackageName(table == null ? null : table.getPackageName());
            log.setGenerateType(generateType);
            log.setTemplate(template);
            log.setFileCount(files == null ? 0 : files.size());
            log.setFileList(truncate(files == null ? "" : String.join("\n", files), MAX_FILE_LIST_LENGTH));
            log.setSuccess(success);
            log.setMessage(truncate(message, MAX_MESSAGE_LENGTH));
            save(log);
        } catch (Exception ex) {
            // 记录日志失败不能影响生成主流程（如日志表尚未迁移时）
        }
    }

    @Override
    public IPage<LogGenerator> pageByTable(String tableId, Integer pageNo, Integer pageSize) {
        QueryWrapper<LogGenerator> wrapper = new QueryWrapper<>();
        if (tableId != null && !tableId.isBlank()) {
            wrapper.eq("table_id", tableId);
        }
        wrapper.orderByDesc("create_time");
        return page(new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
    }

    @Override
    public void clearByTable(String tableId) {
        QueryWrapper<LogGenerator> wrapper = new QueryWrapper<>();
        if (tableId != null && !tableId.isBlank()) {
            wrapper.eq("table_id", tableId);
        }
        remove(wrapper);
    }

    @Override
    public IPage<LogGenerator> pageForMonitor(LogGenerator query, Integer pageNo, Integer pageSize) {
        QueryWrapper<LogGenerator> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (notBlank(query.getTableName())) {
                wrapper.like("table_name", query.getTableName().trim());
            }
            if (notBlank(query.getTableComment())) {
                wrapper.like("table_comment", query.getTableComment().trim());
            }
            if (notBlank(query.getModuleName())) {
                wrapper.like("module_name", query.getModuleName().trim());
            }
            if (notBlank(query.getGenerateType())) {
                wrapper.eq("generate_type", query.getGenerateType().trim());
            }
            // success 为 tinyint(1)：false 是有效筛选值，不能按「非空即忽略」处理
            if (query.getSuccess() != null) {
                wrapper.eq("success", query.getSuccess());
            }
            if (notBlank(query.getCreateBy())) {
                wrapper.like("create_by", query.getCreateBy().trim());
            }
            if (notBlank(query.getBeginTime())) {
                wrapper.ge("create_time", query.getBeginTime());
            }
            if (notBlank(query.getEndTime())) {
                wrapper.le("create_time", query.getEndTime());
            }
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        return page(new Page<>(pageNo == null || pageNo < 1 ? 1 : pageNo,
                pageSize == null || pageSize < 1 ? 10 : pageSize), wrapper);
    }

    /** 空值判断（字符串 null 与空白一视同仁） */
    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 超长截断，避免写库失败 */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
