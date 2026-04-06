package ru.fil.auth_service.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.fil.auth_service.entity.UserDetailsImpl;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${auth.jwt.lifetime}")
    private Duration lifetime;

    @Value("${auth.jwt.secret}")
    private String secretKey;

    private SecretKey getSecretAsKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwt(UserDetailsImpl userDetails) {
        Map<String, Object> claims = initClaims(userDetails);
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + lifetime.toMillis());
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getId().toString())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSecretAsKey(secretKey))
                .compact();
    }

    private Map<String, Object> initClaims(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PLAYER"));
        return claims;
    }

    private Claims extractClaimsFromToken(String jwt) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSecretAsKey(secretKey))
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public String extractSubjectFromToken(String jwt) throws JwtException {
        return extractClaimsFromToken(jwt).getSubject();
    }

    public String extractRoleFromToken(String jwt) throws JwtException {
        return extractClaimsFromToken(jwt).get("role", String.class);
    }
}
