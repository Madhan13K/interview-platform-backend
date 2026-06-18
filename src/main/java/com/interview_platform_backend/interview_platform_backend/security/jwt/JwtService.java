package com.interview_platform_backend.interview_platform_backend.security.jwt;

import com.interview_platform_backend.interview_platform_backend.security.jwks.RsaKeyProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.System.currentTimeMillis;

@Service
public class JwtService {
    private final RsaKeyProperties rsaKeys;
    private final long jwtExpiration;
    private final String jwtRefreshSecret;
    private final long jwtRefreshExpiration;

    public JwtService(RsaKeyProperties rsaKeys,
                      @Value("${jwt.expiration}") long jwtExpiration,
                      @Value("${jwt.refresh-secret}") String jwtRefreshSecret,
                      @Value("${jwt.refresh-expiration}") long jwtRefreshExpiration) {
        this.rsaKeys = rsaKeys;
        this.jwtExpiration = jwtExpiration;
        this.jwtRefreshSecret = jwtRefreshSecret;
        this.jwtRefreshExpiration = jwtRefreshExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public <T> T extractRefreshClaims(String refreshToken, Function<Claims, T> resolver) {
        Claims claims = extractAllRefreshClaims(refreshToken);
        return resolver.apply(claims);
    }

    private Claims extractAllRefreshClaims(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getRefreshSigningKey())
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates an access token signed with the RSA private key.
     * Other microservices can verify this token using the JWKS endpoint.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(currentTimeMillis() + jwtExpiration))
                .setHeaderParam("access_token", "interview-platform-access-token")
                .signWith(rsaKeys.privateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Refresh tokens are still signed with HMAC (symmetric) since they are
     * never verified by external services — only this backend uses them.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .claim("type", "refresh")
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenActive(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && isTokenNotExpired(token);
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllRefreshClaims(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type) && !isRefreshTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractRefreshClaims(token, Claims::getSubject);
    }

    private boolean isTokenNotExpired(String token) {
        return extractExpiration(token).after(new Date());
    }

    private boolean isRefreshTokenExpired(String token) {
        Date exp = extractRefreshClaims(token, Claims::getExpiration);
        return exp.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Access tokens are verified with the RSA public key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(rsaKeys.publicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getRefreshSigningKey() {
        byte[] keyBytes = jwtRefreshSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
