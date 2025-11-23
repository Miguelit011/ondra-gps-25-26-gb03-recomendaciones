package com.ondra.recomendaciones.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de autenticación para comunicación entre microservicios.
 *
 * <p>Valida un token compartido en el header X-Service-Token para autenticar
 * peticiones service-to-service. Se ejecuta antes del JwtAuthenticationFilter,
 * permitiendo bypass de autenticación JWT de usuario.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTokenFilter extends OncePerRequestFilter {

    @Value("${microservices.service-token}")
    private String serviceToken;

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    /**
     * Procesa cada petición HTTP validando el token de servicio si está presente.
     *
     * <p>Si el token es válido, establece autenticación con rol ROLE_SERVICE.
     * Si es inválido, retorna 401.</p>
     *
     * @param request petición HTTP
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros
     * @throws ServletException si ocurre error en el servlet
     * @throws IOException si ocurre error de I/O
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestServiceToken = request.getHeader(SERVICE_TOKEN_HEADER);

        if (requestServiceToken == null || requestServiceToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (serviceToken.equals(requestServiceToken)) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    "SERVICE",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            request.setAttribute("isServiceRequest", true);

            log.debug("🔓 Autenticación service-to-service establecida");

        } else {
            log.warn("⚠️ Token de servicio inválido");
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_SERVICE_TOKEN", "Token de servicio inválido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Escribe una respuesta de error en formato JSON.
     *
     * @param response respuesta HTTP
     * @param status código de estado HTTP
     * @param error código de error
     * @param message mensaje de error
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