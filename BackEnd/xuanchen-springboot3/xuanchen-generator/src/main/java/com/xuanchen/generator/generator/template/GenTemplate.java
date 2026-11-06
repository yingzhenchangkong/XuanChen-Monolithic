package com.xuanchen.generator.generator.template;

import com.xuanchen.common.exception.BusinessException;

/**
 * 生成模板：所有模板均生成完整 CRUD（后端 6 件套 + 前端页面 + menu.sql，固定不可选），
 * 模板字段只决定「是否按主子表（一对多）组织页面」：
 * <ul>
 *     <li>单表：标准 CRUD 列表页 + 弹窗，即使残留子表配置也不生成主子表代码；</li>
 *     <li>一对多：字典管理式——主表列表页（单选一行），页面下方维护该行记录的子表明细。</li>
 * </ul>
 * <p>
 * 对应 gen_table.template 字段；为空时按一对多处理。历史值 crud / backend / one2one
 * 统一回退一对多（完整 CRUD 已固定，一对一合并编辑模式已下线，历史子表配置继续按一对多生成）。
 * 未知值直接报错，避免配置写错后静默按默认模板生成。
 *
 * @author XuanChen
 * @date 2026-10-19
 */
public enum GenTemplate {

    /** 单表：标准 CRUD，不生成主子表页面 */
    SINGLE("single", "单表"),

    /** 一对多：字典管理式主子页面 */
    ONE2MANY("one2many", "一对多");

    private final String key;
    private final String description;

    GenTemplate(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    /**
     * 解析模板标识；空值与历史值（crud / backend / one2one）回退一对多。
     *
     * @param value gen_table.template
     * @return 模板
     */
    public static GenTemplate of(String value) {
        if (value == null || value.isBlank()) {
            return ONE2MANY;
        }
        String key = value.trim();
        // 历史值兼容：完整 CRUD 已固定为唯一生成方式，一对一合并编辑已下线，
        // 旧配置一律按一对多继续生成（子表配置不丢）
        if ("crud".equalsIgnoreCase(key) || "backend".equalsIgnoreCase(key)
                || "one2one".equalsIgnoreCase(key)) {
            return ONE2MANY;
        }
        for (GenTemplate template : values()) {
            if (template.key.equalsIgnoreCase(key)) {
                return template;
            }
        }
        throw new BusinessException(400, "不支持的生成模板：" + value + "（可选：single / one2many）");
    }
}
