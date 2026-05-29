package com.yc.Xcommon;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SimpleJwtUtil {

    // 密钥（实际项目中要存到配置文件，不要硬编码）
    private static final String SECRET_KEY = "my-secret-key-123456";
    // 过期时间：24小时
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 生成 JWT
     * @param userId 用户ID
     * @return JWT 字符串
     */
    public static String generateToken(String userId) throws Exception {
        // 1. 构建 Header（固定算法 HS256）
        Map<String, String> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String headerJson = toJson(header);
        String headerBase64 = base64UrlEncode(headerJson);

        // 2. 构建 Payload（自定义声明）
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", userId); // 主题：用户ID
        payload.put("iat", System.currentTimeMillis() / 1000); // 签发时间（秒）
        payload.put("exp", (System.currentTimeMillis() + EXPIRATION_TIME) / 1000); // 过期时间（秒）
        String payloadJson = toJson(payload);
        String payloadBase64 = base64UrlEncode(payloadJson);

        // 3. 生成 Signature
        String signature = hmacSha256(headerBase64 + "." + payloadBase64, SECRET_KEY);

        // 拼接成完整 JWT
        return headerBase64 + "." + payloadBase64 + "." + signature;
    }

    /**
     * 验证 JWT 并解析用户ID
     * @param token JWT 字符串
     * @return 解析出的用户ID，无效返回 null
     */
    public static String validateToken(String token) throws Exception {
        // 1. 分割三部分
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signature = parts[2];

        // 2. 重新计算签名，验证是否一致
        String expectedSignature = hmacSha256(headerBase64 + "." + payloadBase64, SECRET_KEY);
        if (!expectedSignature.equals(signature)) {
            return null;
        }

        // 3. 解析 Payload，验证过期时间
        String payloadJson = base64UrlDecode(payloadBase64);
        Map<String, Object> payload = parseJson(payloadJson);
        long exp = ((Number) payload.get("exp")).longValue();
        long now = System.currentTimeMillis() / 1000;
        if (exp < now) {
            return null;
        }

        // 4. 返回用户ID
        return (String) payload.get("sub");
    }

    // ---------------- 工具方法 ----------------
    // Base64Url 编码（去掉填充的 =，替换 +/ 为 -_）
    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8))
                .replace("=", "");
    }

    // Base64Url 解码
    private static String base64UrlDecode(String data) {
        // 补全填充的 =
        int padding = 4 - (data.length() % 4);
        if (padding < 4) {
            data += "=".repeat(padding);
        }
        return new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);
    }

    // HMAC-SHA256 签名
    private static String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().encodeToString(bytes).replace("=", "");
    }

    // 简单的 Map 转 JSON（仅支持 String/Number 类型，实际项目用 Jackson/Gson）
    private static String toJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            sb.append("\"").append(k).append("\":");
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                sb.append(v);
            }
            sb.append(",");
        });
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    // 简单的 JSON 解析（仅支持键值对，实际项目用 Jackson/Gson）
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.replace("{", "").replace("}", "").trim();
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            String key = kv[0].trim().replace("\"", "");
            String value = kv[1].trim();
            if (value.startsWith("\"")) {
                map.put(key, value.replace("\"", ""));
            } else {
                map.put(key, Long.parseLong(value));
            }
        }
        return map;
    }


}