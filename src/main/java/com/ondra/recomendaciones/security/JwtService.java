package com.ondra.recomendaciones.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Servicio para validación y extracción de información de tokens JWT.
 *
 * <p>Valida tokens JWT generados por el microservicio de Usuarios.
 * No genera tokens, solo los valida y extrae claims.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Obtiene la clave de firma para validar tokens JWT.
     *
     * @return SecretKey para validación HMAC
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extrae todos los claims del token JWT.
     *
     * @param token Token JWT
     * @return Claims del token
     * @throws io.jsonwebtoken.JwtException si el token es inválido
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el ID de usuario del token JWT.
     *
     * <p>El userId está en los claims como Long, no en el subject.
     * Estructura esperada del token:
     * <ul>
     *   <li>claims.userId: Long (ID del usuario)</li>
     *   <li>claims.email: String</li>
     *   <li>subject: String (email)</li>
     * </ul>
     *
     * @param token Token JWT
     * @return ID del usuario
     * @throws IllegalArgumentException si el token no contiene userId
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");

        if (userIdObj == null) {
            throw new IllegalArgumentException("Token sin userId");
        }

        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        return Long.valueOf(userIdObj.toString());
    }

    /**
     * Extrae el email del usuario del token JWT.
     *
     * @param token Token JWT
     * @return Email del usuario
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extrae el subject del token JWT.
     *
     * @param token Token JWT
     * @return Subject del token (email)
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae el tipo de usuario si está presente en el token.
     *
     * @param token Token JWT
     * @return Tipo de usuario o null si no está presente
     */
    public String extractTipoUsuario(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("tipoUsuario", String.class);
    }

    /**
     * Valida si el token JWT es válido.
     *
     * <p>Verifica firma y estructura del token.
     *
     * @param token Token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("❌ Token inválido: {}", e.getMessage());
            return false;
        }
    }
}