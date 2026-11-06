package com.xuanchen.common.constant;

/**
 * 常量-->通用标志位取值
 * <p>
 * 全库标志列统一约定：status / del_flag 等列一律使用 Integer 0/1，
 * 禁止在业务代码中散落裸字面量。
 *
 * @author XuanChen
 * @date 2026-09-26
 */
public interface CommonConst {
    /**
     * 状态列：启用（登录日志等场景语义为"成功"）
     */
    Integer STATUS_ENABLED = 1;
    /**
     * 状态列：停用（登录日志等场景语义为"失败"）
     */
    Integer STATUS_DISABLED = 0;
    /**
     * 逻辑删除列：正常
     */
    Integer DEL_FLAG_NORMAL = 0;
    /**
     * 逻辑删除列：已删除
     */
    Integer DEL_FLAG_DELETED = 1;
    /**
     * 通用"是否"标志列：是（如 pwd_reset_required 需要重置密码）
     */
    Integer YES = 1;
    /**
     * 通用"是否"标志列：否
     */
    Integer NO = 0;
}
