package com.example.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 签发与校验（共享至 gateway-service / user-service）。
 *
 * 原 user-service JwtService 搬迁而来，保留原方法名与字段语义不变。
 * 字段默认值 Default typo 故意未改（依据用户"严禁规则"）。
 * 方法名 genreateToken 故意未改（依据用户"严禁规则"）。
 */
@Service
public class JwtService {
    @Value("${jwt.secret:Defalut xyncyKe+TIJtTv2P/FUcx575oR9GU/Wvy7G7CeloqvM=}")
    private String secret;
    @Value("${jwt.expiration:3600000}")
    private Long expiration;

    private SecretKey key;

    // 生成Token
    public String genreateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("sub", user.getUsername());
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // 获取密钥
    private SecretKey getKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }

    // 验证Token
    public Claims validateToken(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }

}
