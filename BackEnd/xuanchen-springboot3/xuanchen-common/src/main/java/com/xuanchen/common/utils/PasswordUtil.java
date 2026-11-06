package com.xuanchen.common.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码工具：生成高强度随机临时密码。
 * 用于新增/导入用户时不再使用全系统统一的硬编码默认密码——每个（或每批）用户获得独立随机口令，
 * 配合 sys_user.pwd_reset_required=1 要求首次登录后立即修改。
 *
 * @author XuanChen
 * @date 2026-09-19
 */
public final class PasswordUtil {
    private PasswordUtil() {
    }

    /**
     * 易混字符（0/O、1/l/I 等）已剔除，便于管理员线下转交、用户手工输入
     */
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGIT = "23456789";
    /**
     * 特殊字符只取常见、无转义/注入争议的集合
     */
    private static final String SPECIAL = "@#$%&*-_=+?";
    private static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;

    /**
     * 临时密码固定长度（满足 8 位以上且含四类字符的常见口令策略）
     */
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 口令复杂度策略（与前端 ChangePassword/ForceChangePassword 表单规则保持一致）：
     * 长度 6-20，且必须同时包含字母、数字、特殊字符（非字母数字）各至少 1 个。
     * 所有设置口令的后端入口（本人改密、管理员重置、新增用户显式指定口令）都必须强制校验，
     * 不能只依赖前端校验。
     */
    private static final Pattern PASSWORD_POLICY_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,20}$");

    /**
     * 校验明文口令是否满足长度与复杂度策略
     *
     * @param rawPassword 明文口令
     * @return true 合规
     */
    public static boolean isValidPassword(String rawPassword) {
        return rawPassword != null && PASSWORD_POLICY_PATTERN.matcher(rawPassword).matches();
    }

    /**
     * 生成一个随机临时密码：保证至少含大写、小写、数字、特殊字符各 1 个，其余字符随机，
     * 并对整体位置做洗牌，避免类别顺序固定。
     *
     * @return 随机临时密码（明文，仅在生成当次通过 HTTPS 响应返回给操作者，数据库只存 BCrypt 哈希）
     */
    public static String generateTempPassword() {
        List<Character> chars = new ArrayList<>(TEMP_PASSWORD_LENGTH);
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGIT));
        chars.add(randomChar(SPECIAL));
        for (int i = chars.size(); i < TEMP_PASSWORD_LENGTH; i++) {
            chars.add(randomChar(ALL));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
