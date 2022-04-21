package com.anhui.fabricbaasweb.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@AllArgsConstructor
@Data
public class JwtUtils {
    private final long expiration;
    private final String secret;
    private final String header;

    /**
     * @param authorities 权限列表
     * @return 包含所有权限名称的字符串（用逗号分隔开）
     */
    private static String convertAuthoritiesToString(Collection<? extends GrantedAuthority> authorities) {
        List<String> authorityNames = new ArrayList<>();
        for (GrantedAuthority authority : authorities) {
            authorityNames.add(authority.getAuthority());
        }
        return String.join(",", authorityNames);
    }

    /**
     * @param userDetails 用户信息
     * @return Token（包含用户名、用户权限、时间戳等信息）
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(2);
        claims.put("username", userDetails.getUsername());
        claims.put("authorities", convertAuthoritiesToString(userDetails.getAuthorities()));

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * @return Token所包含的Claims信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * @return Token是否已经过期（对比当前的系统时间）
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getClaimsFromToken(token).getExpiration();
        return expiration.after(new Date());
    }

    /**
     * @return 主要检查两项：
     * 1. Token是否与UserDetails匹配
     * 2. Token是否已经过期
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        String username = (String) claims.get("username");
        String authorities = (String) claims.get("authorities");
        return expiration.after(new Date()) &&
                username.equals(userDetails.getUsername()) &&
                authorities.equals(convertAuthoritiesToString(userDetails.getAuthorities()));
    }

    /**
     * @return Token所对应的用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get("username");
    }

    /**
     * @return Token所有的权限名称
     */
    public String[] getAuthorityNamesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String authorities = (String) claims.get("authorities");
        return authorities.split(",");
    }
}
