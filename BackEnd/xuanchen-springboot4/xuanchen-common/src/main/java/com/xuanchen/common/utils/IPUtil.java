package com.xuanchen.common.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具类-->IP
 * <p>
 * 安全模型：
 * <ul>
 *   <li>{@code request.getRemoteAddr()}（TCP 连接对端）在应用层不可伪造，是唯一默认可信的来源；</li>
 *   <li>{@code X-Forwarded-For} / {@code X-Real-IP} 均为客户端可写请求头，<b>只有当直连对端属于
 *       显式配置的可信代理网段（xuanchen.security.trusted-proxies）时才允许采信</b>；</li>
 *   <li>采信 XFF 时从链尾（最后一跳）向左遍历，逐跳跳过可信代理，取第一个非可信地址，
 *       防止攻击者在 XFF 左侧塞入任意伪造值嫁祸他人或打散 IP 维度限流。</li>
 * </ul>
 * 未配置可信代理时（生产默认），即使经过反向代理也只取 RemoteAddr；部署在 Nginx 等反向代理之后时，
 * 必须把代理自身地址（如 127.0.0.1/8、10.0.0.0/8 等）通过环境变量 XUANCHEN_TRUSTED_PROXIES 显式声明。
 *
 * @author XuanChen
 * @date 2026-02-07
 */
public final class IPUtil {

    private IPUtil() {
    }

    /**
     * 可信代理网段，由 TrustedProxyInitializer 在启动时注入；空集合 = 不信任任何代理头
     */
    private static volatile List<IpRange> trustedRanges = List.of();

    /**
     * 初始化可信代理网段（逗号分隔的 CIDR，如 127.0.0.1/8,10.0.0.0/8,::1/128）
     */
    public static void initTrustedProxies(String cidrCsv) {
        List<IpRange> ranges = new ArrayList<>();
        if (cidrCsv != null && !cidrCsv.isBlank()) {
            for (String part : cidrCsv.split(",")) {
                String cidr = part.trim();
                if (cidr.isEmpty()) {
                    continue;
                }
                IpRange range = IpRange.parse(cidr);
                if (range != null) {
                    ranges.add(range);
                }
            }
        }
        trustedRanges = List.copyOf(ranges);
    }

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP 请求对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        byte[] remoteBytes = parseIp(remoteAddr);

        // 直连对端不是可信代理：任何代理头都可能是攻击者伪造的，一律只认 TCP 对端地址
        if (remoteBytes == null || !isTrusted(remoteBytes)) {
            return remoteBytes != null ? formatIp(remoteBytes) : remoteAddr;
        }

        // 对端是可信代理：沿 X-Forwarded-For 从右向左跳过可信代理跳数，
        // 第一个非可信（且格式合法）的地址即真实客户端
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff.trim())) {
            String[] chain = xff.split(",");
            for (int i = chain.length - 1; i >= 0; i--) {
                byte[] candidate = parseIp(chain[i].trim());
                if (candidate == null) {
                    // 出现非法字面量：链路不可信，fail-safe 回退到连接对端，不采信更左侧的值
                    break;
                }
                if (isTrusted(candidate)) {
                    continue;
                }
                return formatIp(candidate);
            }
        }

        // XFF 缺失或全是可信代理：可信代理设置的 X-Real-IP 可作为补充信号
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp.trim())) {
            byte[] realIp = parseIp(xRealIp.trim());
            if (realIp != null && !isTrusted(realIp)) {
                return formatIp(realIp);
            }
        }

        return remoteBytes != null ? formatIp(remoteBytes) : remoteAddr;
    }

    /**
     * 格式化输出，唯一的出口：
     * 归一化回环地址——IPv6 ::1（字面量形如 0:0:0:0:0:0:0:1）与 IPv4 127.0.0.1
     * 都指向同一台本机。浏览器经 localhost 访问时 TCP 对端会在二者之间漂移，若不归一化，
     * "同一用户名+同一IP 不允许重复登录"的踢下线键会分裂成两个，同机重复登录便无法互踢，
     * 在线用户列表里出现同一用户的多条会话。仅归一化回环（本机唯一地址），不影响真实 IPv6 客户端。
     */
    static String formatIp(byte[] ip) {
        if (ip.length == 4) {
            return (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
        }
        if (isIpv6Loopback(ip)) {
            return "127.0.0.1";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(Integer.toHexString(((ip[i * 2] & 0xFF) << 8) | (ip[i * 2 + 1] & 0xFF)));
        }
        return sb.toString();
    }

    /**
     * 是否为 IPv6 回环 ::1（前 15 字节为 0，末字节为 1）
     */
    private static boolean isIpv6Loopback(byte[] b) {
        if (b.length != 16) {
            return false;
        }
        for (int i = 0; i < 15; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return b[15] == 1;
    }

    private static boolean isTrusted(byte[] ip) {
        for (IpRange range : trustedRanges) {
            if (range.contains(ip)) {
                return true;
            }
        }
        return false;
    }

    // ============================ IP 字面量/CIDR 纯字面量解析（不触发 DNS） ============================

    /**
     * 严格解析 IP 字面量；非数字字面量（主机名、垃圾值）一律返回 null。
     * IPv4 与 IPv4-mapped IPv6（::ffff:a.b.c.d）统一归一化为 4 字节，其余 IPv6 为 16 字节
     */
    static byte[] parseIp(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.indexOf(':') >= 0) {
            return parseIpv6(s);
        }
        return parseIpv4(s);
    }

    private static byte[] parseIpv4(String s) {
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            String p = parts[i];
            if (p.isEmpty() || p.length() > 3 || !p.chars().allMatch(Character::isDigit)) {
                return null;
            }
            int v = Integer.parseInt(p);
            if (v > 255) {
                return null;
            }
            bytes[i] = (byte) v;
        }
        return bytes;
    }

    private static byte[] parseIpv6(String s) {
        // 剥离 zone id（fe80::1%eth0）
        int zone = s.indexOf('%');
        if (zone >= 0) {
            s = s.substring(0, zone);
        }
        int dc = s.indexOf("::");
        String head = dc >= 0 ? s.substring(0, dc) : s;
        String tail = dc >= 0 ? s.substring(dc + 2) : "";
        // 不允许多个 ::
        if (tail.contains("::")) {
            return null;
        }
        List<Integer> headGroups = parseV6Groups(head, true);
        List<Integer> tailGroups = dc >= 0 ? parseV6Groups(tail, true) : new ArrayList<>();
        if (headGroups == null || tailGroups == null) {
            return null;
        }
        List<Integer> groups = new ArrayList<>(headGroups);
        if (dc >= 0) {
            int gap = 8 - headGroups.size() - tailGroups.size();
            if (gap < 0) {
                return null;
            }
            for (int i = 0; i < gap; i++) {
                groups.add(0);
            }
        }
        groups.addAll(tailGroups);
        if (groups.size() != 8) {
            return null;
        }
        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            int g = groups.get(i);
            bytes[i * 2] = (byte) (g >> 8);
            bytes[i * 2 + 1] = (byte) g;
        }
        // IPv4-mapped: ::ffff:a.b.c.d
        if (isV4Mapped(bytes)) {
            return new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
        }
        return bytes;
    }

    /**
     * @param allowEmbeddedV4 末段允许是点分 IPv4（占用 2 个 group）
     */
    private static List<Integer> parseV6Groups(String segment, boolean allowEmbeddedV4) {
        List<Integer> groups = new ArrayList<>();
        if (segment.isEmpty()) {
            return groups;
        }
        String[] tokens = segment.split(":", -1);
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            boolean last = i == tokens.length - 1;
            if (last && allowEmbeddedV4 && t.indexOf('.') >= 0) {
                byte[] v4 = parseIpv4(t);
                if (v4 == null) {
                    return null;
                }
                groups.add(((v4[0] & 0xFF) << 8) | (v4[1] & 0xFF));
                groups.add(((v4[2] & 0xFF) << 8) | (v4[3] & 0xFF));
                continue;
            }
            if (t.isEmpty() || t.length() > 4 || !t.chars().allMatch(IPUtil::isHex)) {
                return null;
            }
            groups.add(Integer.parseInt(t, 16));
        }
        return groups;
    }

    private static boolean isHex(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isV4Mapped(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) {
                return false;
            }
        }
        return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
    }

    /**
     * 不可变 CIDR 网段（支持 IPv4/IPv6，裸 IP 视为 /32 或 /128）
     */
    static final class IpRange {
        private final byte[] network;
        private final int prefix;

        private IpRange(byte[] network, int prefix) {
            this.network = network;
            this.prefix = prefix;
        }

        static IpRange parse(String cidr) {
            String addr = cidr;
            int prefix = -1;
            int slash = cidr.indexOf('/');
            if (slash >= 0) {
                addr = cidr.substring(0, slash);
                try {
                    prefix = Integer.parseInt(cidr.substring(slash + 1).trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            byte[] bytes = parseIp(addr.trim());
            if (bytes == null) {
                return null;
            }
            int maxPrefix = bytes.length * 8;
            if (prefix < 0) {
                prefix = maxPrefix;
            }
            if (prefix < 0 || prefix > maxPrefix) {
                return null;
            }
            // 规整为主机位清零的网络地址
            for (int i = 0; i < bytes.length; i++) {
                int keep = Math.min(8, Math.max(0, prefix - i * 8));
                int mask = keep == 0 ? 0 : (0xFF << (8 - keep)) & 0xFF;
                bytes[i] = (byte) (bytes[i] & mask);
            }
            return new IpRange(bytes, prefix);
        }

        boolean contains(byte[] ip) {
            if (ip.length != network.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remain = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (ip[i] != network[i]) {
                    return false;
                }
            }
            if (remain > 0 && fullBytes < network.length) {
                int mask = (0xFF << (8 - remain)) & 0xFF;
                if ((ip[fullBytes] & mask) != (network[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
