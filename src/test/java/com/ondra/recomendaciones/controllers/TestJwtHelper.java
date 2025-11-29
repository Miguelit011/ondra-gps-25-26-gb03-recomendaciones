package com.ondra.recomendaciones.controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper para generar tokens JWT reales en los tests.
 */
@Slf4j
@Component
public class TestJwtHelper {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Genera un token JWT de prueba para un usuario.
     *
     * @param userId id del usuario
     * @param email  email del usuario
     * @return token JWT valido
     */
    public String generarTokenPrueba(Long userId, String email) {
        log.debug("Generando token de prueba para userId={}, email={}", userId, email);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("tipoUsuario", "NORMAL");

        return createToken(claims, email);
    }

    /**
     * Genera un token JWT de prueba para un artista.
     *
     * @param userId   id del usuario
     * @param artistId id del artista
     * @param email    email del usuario
     * @return token JWT valido
     */
    public String generarTokenPruebaArtista(Long userId, Long artistId, String email) {
        log.debug("Generando token de prueba para artista userId={}, artistId={}", userId, artistId);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("artistId", artistId);
        claims.put("email", email);
        claims.put("tipoUsuario", "ARTISTA");

        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
