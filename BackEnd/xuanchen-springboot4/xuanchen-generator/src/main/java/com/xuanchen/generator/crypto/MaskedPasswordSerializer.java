package com.xuanchen.generator.crypto;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 数据源密码的 JSON 输出序列化器：任何非 null 值一律输出固定掩码 ******，
 * 从根上杜绝 list/详情/下拉/回收站等接口把真实密码（或密文）回传前端；
 * 反序列化（入参）不受影响，前端仍可提交新密码。
 * <p>
 * Spring Boot 4 的 HTTP 消息层使用 Jackson 3（{@code tools.jackson.*}），
 * 注意不要误用 Fesod 传递引入的 Jackson 2（{@code com.fasterxml.jackson.*}），
 * 否则注解不会被 Web 层识别，脱敏静默失效。
 *
 * @author XuanChen
 * @date 2026-09-24
 */
public class MaskedPasswordSerializer extends ValueSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(DatasourceCipher.MASK);
        }
    }
}
