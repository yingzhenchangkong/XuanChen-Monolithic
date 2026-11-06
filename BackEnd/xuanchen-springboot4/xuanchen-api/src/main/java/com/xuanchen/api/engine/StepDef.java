package com.xuanchen.api.engine;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 步骤定义（api.steps JSON 数组的元素）
 *
 * 四类步骤：
 * - sql  ：config = { sql: "SELECT ... WHERE x = :pName", params: { pName: "{{params.x}}" } }
 *          强制命名参数预编译，sql 必须以 SELECT 开头（防注入面收敛为只读）
 * - http ：config = { url, method(GET/POST), headers: {...}, params: {...}, body: {...} }
 *          url/headers/params/body 中均可用 {{路径}} 取上下文值
 * - map  ：config = { source: "步骤名", keepSource: true, fields: [{ from: "a.b", to: "c" }] }
 *          源为数组则逐行映射，源为对象则单对象映射
 * - save ：config = { source: "步骤名" }
 *          将源（数组/对象）JSON 序列化后逐条落 api_data
 *
 * 每个步骤输出写入上下文 context[step.name]，后续步骤可用 {{stepName.路径}} 引用。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Data
public class StepDef implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 步骤类型（sql/http/map/save）
     */
    private String type;

    /**
     * 步骤名（上下文键，字母数字下划线）
     */
    private String name;

    /**
     * 步骤配置（按 type 不同结构）
     */
    private Map<String, Object> config;

    /**
     * 校验步骤定义合法性（保存配置时调用）
     */
    public void validate() {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("步骤类型不能为空！");
        }
        if (!List.of("sql", "http", "map", "save").contains(type)) {
            throw new IllegalArgumentException("不支持的步骤类型：" + type + "（仅支持 sql/http/map/save）");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("步骤名不能为空！");
        }
        if (!name.matches("[a-zA-Z][a-zA-Z0-9_]{0,31}")) {
            throw new IllegalArgumentException("步骤名须以字母开头，仅含字母数字下划线，最长32字符：" + name);
        }
        if (config == null) {
            throw new IllegalArgumentException("步骤 [" + name + "] 缺少 config 配置！");
        }
        switch (type) {
            case "sql" -> requireConfigKeys(name, config, "sql");
            case "http" -> requireConfigKeys(name, config, "url", "method");
            case "map" -> requireConfigKeys(name, config, "source");
            case "save" -> requireConfigKeys(name, config, "source");
            default -> throw new IllegalArgumentException("不支持的步骤类型：" + type);
        }
    }

    private static void requireConfigKeys(String stepName, Map<String, Object> config, String... keys) {
        for (String key : keys) {
            if (config.get(key) == null || String.valueOf(config.get(key)).isBlank()) {
                throw new IllegalArgumentException("步骤 [" + stepName + "]（" + key + "）配置不完整！");
            }
        }
    }
}
