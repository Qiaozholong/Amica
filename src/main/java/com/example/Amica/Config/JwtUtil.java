package com.example.Amica.Config;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret-key}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long expiration;

    //方法内令牌获取方法
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    //封装令牌
    public String generateToken(long userId, String account) {
        String token = Jwts.builder()
                .claim("userId",userId)
                .claim("account",account)
                .expiration(new Date(System.currentTimeMillis()+ expiration))
                .signWith(getKey())
                .compact();
        return token;
    }

    //类内私密解析令牌方法，用于调用
    private Claims getClaims(String token){
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }

    //令牌验证方法(单验证)
    public void validateToken(String token){getClaims(token);}

    //令牌提取userId方法
    public long extractUserId(String token){
        Claims claims = getClaims(token);
        long userId = claims.get("userId", Long.class);
        return userId;
    }

}
