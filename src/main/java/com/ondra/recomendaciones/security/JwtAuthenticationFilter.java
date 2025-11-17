package com.ondra.recomendaciones.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de autenticación JWT para validación de usuarios.
 *
 * <p>Valida tokens JWT generados por el microservicio de Usuarios y establece
 * la autenticación en el contexto de Spring Security. Se ejecuta después del
 * ServiceTokenFilter en la cadena de filtros.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Procesa cada petición HTTP validando el token JWT si está presente.
     *
     * <p>Flujo de validación:
     * <ol>
     *   <li>Extrae el token del header Authorization</li>
     *   <li>Valida firma y estructura del token</li>
     *   <li>Extrae userId y email del token</li>
     *   <li>Establece autenticación en SecurityContext</li>
     *   <li>Guarda userId y email en request attributes</li>
     * </ol>
     *
     * @param request Petición HTTP
     * @param response Respuesta HTTP
     * @param filterChain Cadena de filtros
     * @throws ServletException si ocurre error en el servlet
     * @throws IOException si ocurre error de I/O
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            if (jwtService.isTokenValid(jwt)) {
                final Long userId = jwtService.extractUserId(jwt);
                final String email = jwtService.extractEmail(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    request.setAttribute("userId", userId);
                    request.setAttribute("email", email);

                    log.debug("✅ Usuario {} autenticado correctamente", userId);
                }
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
                    "INVALID_TOKEN", e.getMessage());
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
     * Escribe una respuesta de error en formato JSON.
     *
     * @param response Respuesta HTTP
     * @param status Código de estado HTTP
     * @param error Código de error
     * @param message Mensaje de error
     * @throws IOException si ocurre error al escribir la respuesta
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