package com.xuanchen.businessprocess.engine;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 条件表达式求值器（SpEL，只读安全上下文）
 *
 * <p>上下文变量 = 业务表单字段 + applyUser/businessType/businessId；
 * 使用 {@link SimpleEvaluationContext#forReadOnlyDataBinding()}，
 * 禁用类型引用、构造器调用与任意方法执行，仅支持属性/变量比较运算。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
@Component
public class BpmExprEvaluator {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 求值条件表达式，返回是否成立
     *
     * <p>变量缺失/求值结果非 Boolean/抛异常时一律返回 false（走 default 流兜底）。</p>
     *
     * @param expr      表达式，如 amount &gt; 5000
     * @param variables 变量表
     * @return 是否成立
     */
    public boolean evaluate(String expr, Map<String, Object> variables) {
        if (expr == null || expr.isBlank()) {
            return false;
        }
        try {
            Expression expression = parser.parseExpression(expr);
            SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            if (variables != null) {
                variables.forEach((k, v) -> context.setVariable(k, v));
            }
            Object value = expression.getValue(context);
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            // 表达式求值失败按不成立处理（保存时已做语法校验，此处多为变量类型问题）
            return false;
        }
    }
}
