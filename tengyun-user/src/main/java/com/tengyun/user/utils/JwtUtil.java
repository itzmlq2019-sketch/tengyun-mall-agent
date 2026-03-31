package com.tengyun.user.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // 1. 极其重要：定义一个防伪秘钥（必须大于等于256位/32个字符）。
    // 在分布式系统里，发证的（User服务）和验证的（网关）必须用同一把钥匙！
    public static final String SECRET_KEY = "TengYunMallSecretKeyMustBeAtLeast256BitsLong==";

    // 2. 生成加密 Key 对象
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // 3. 设置身份证过期时间（这里设置为 2 小时）
    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000;

    /**
     * 颁发 Token
     */
    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)                 // 证件所有人（用户名）
                .claim("userId", userId)              // 证件附带信息（用户ID）
                .setIssuedAt(new Date())              // 办证时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME)) // 到期时间
                .signWith(KEY)                        // 盖上防伪公章
                .compact();                           // 塑封打包成字符串
    }
}