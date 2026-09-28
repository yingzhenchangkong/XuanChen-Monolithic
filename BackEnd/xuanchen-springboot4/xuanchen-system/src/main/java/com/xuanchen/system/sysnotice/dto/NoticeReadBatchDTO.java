package com.xuanchen.system.sysnotice.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量通知标记已读入参。
 *
 * @author XuanChen
 * @date 2026-09-24
 */
@Data
public class NoticeReadBatchDTO {
    /**
     * sys_notice_status 主键集合；为空时后端按幂等处理（直接返回成功，不执行 SQL）
     */
    private List<String> ids;
}
