package com.xuanchen.system.sysnotice.dto;

import lombok.Data;

/**
 * 单条通知标记已读入参。
 * <p>
 * 不再复用 SysNotice 实体接参：标记已读只需要通知状态行 id，
 * 使用独立 DTO 避免批量赋值并收敛可提交字段。
 *
 * @author XuanChen
 * @date 2026-09-24
 */
@Data
public class NoticeReadDTO {
    /**
     * sys_notice_status 主键
     */
    private String noticeStatusId;
}
