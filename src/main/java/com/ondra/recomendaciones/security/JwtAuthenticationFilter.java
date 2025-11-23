package com.ondra.recomendaciones.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro de autenticación JWT para validación de tokens de acceso.
 *
 * <p>Valida tokens JWT del header Authorization y establece la autenticación
 * en el contexto de seguridad. Se ejecuta después de {@link ServiceTokenFilter}
 * y excluye automáticamente endpoints públicos y peticiones ya autenticadas.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Determina si el filtro debe omitir la validación para la petición actual.
     *
     * @param request petición HTTP entrante
     * @return true si el filtro debe omitirse, false en caso contrario
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            return true;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("✅ Petición ya autenticada por ServiceTokenFilter");
            return true;
        }

        return false;
    }

    /**
     * Procesa la petición validando el token JWT y estableciendo la autenticación.
     * Extrae userId, email, tipoUsuario y artistId (si aplica) del token.
     *
     * @param request petición HTTP
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros
     * @throws ServletException si ocurre un error de servlet
     * @throws IOException si ocurre un error de entrada/salida
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        log.debug("🔍 JwtAuthenticationFilter procesando: {} {}", request.getMethod(), request.getRequestURI());

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && validateToken(token)) {
                Claims claims = extractAllClaims(token);

                Long userId = claims.get("userId", Long.class);
                String email = claims.get("email", String.class);
                String tipoUsuario = claims.get("tipoUsuario", String.class);
                Object artistIdObj = claims.get("artistId");

                if (userId == null) {
                    log.warn("⚠️ Token JWT sin userId");
                    writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "INVALID_TOKEN", "Token no contiene userId");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList()
                        );

                Map<String, Object> additionalDetails = new HashMap<>();
                additionalDetails.put("userId", userId);
                additionalDetails.put("email", email);
                additionalDetails.put("tipoUsuario", tipoUsuario);

                if (artistIdObj != null) {
                    Long artistId = Long.valueOf(String.valueOf(artistIdObj));
                    additionalDetails.put("artistId", artistId);
                    request.setAttribute("artistId", artistId);
                    log.debug("✅ Usuario {} autenticado (tipo: {}, artistId: {})",
                            userId, tipoUsuario, artistId);
                } else {
                    log.debug("✅ Usuario {} autenticado (tipo: {})", userId, tipoUsuario);
                }

                authentication.setDetails(additionalDetails);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
                request.setAttribute("email", email);
                request.setAttribute("tipoUsuario", tipoUsuario);

            } else if (token == null) {
                log.debug("⚠️ No se encontró token JWT en la petición");
            }

        } catch (ExpiredJwtException e) {
            log.warn("❌ Token expirado: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_EXPIRED", "El token ha expirado");
            return;

        } catch (SignatureException e) {
            log.warn("❌ Firma del token inválida: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_SIGNATURE", "Token con firma inválida");
            return;

        } catch (MalformedJwtException e) {
            log.warn("❌ Token malformado: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "MALFORMED_TOKEN", "Token malformado");
            return;

        } catch (UnsupportedJwtException e) {
            log.warn("❌ Token no soportado: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "UNSUPPORTED_TOKEN", "Token no soportado");
            return;

        } catch (IllegalArgumentException e) {
            log.warn("❌ Token inválido: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN", "Token inválido");
            return;

        } catch (Exception e) {
            log.error("❌ Error inesperado validando JWT", e);
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Error al procesar el token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization.
     *
     * @param request petición HTTP
     * @return token JWT sin el prefijo Bearer, o null si no existe
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Valida la firma y estructura del token JWT.
     *
     * @param token token JWT a validar
     * @return true si el token es válido
     */
    private boolean validateToken(String token) {
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
        return true;
    }

    /**
     * Extrae todos los claims del token JWT.
     *
     * @param token token JWT
     * @return claims contenidos en el token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Genera la clave de firma a partir del secreto configurado.
     *
     * @return clave secreta para firma HMAC
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Escribe una respuesta de error en formato JSON.
     *
     * @param response respuesta HTTP
     * @param status código de estado HTTP
     * @param error código de error
     * @param message mensaje descriptivo del error
     * @throws IOException si ocurre un error al escribir la respuesta
     */
    private void writeErrorResponse(HttpServletResponse response, int status,
                                    String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"message\":\"%s\"}", error, message
        ));
    }
}