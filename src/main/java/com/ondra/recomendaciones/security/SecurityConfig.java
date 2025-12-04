package com.ondra.recomendaciones.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * ConfiguraciИn de seguridad para el microservicio de recomendaciones.
 *
 * <p>Establece la cadena de filtros de seguridad, polヴticas CORS y autenticaciИn
 * mediante tokens JWT y tokens de servicio para comunicaciИn entre microservicios.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenFilter serviceTokenFilter;

    /**
     * Configura las polヴticas CORS para permitir peticiones desde clientes especヴficos.
     *
     * @return CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configura la cadena de filtros de seguridad.
     *
     * <p>Establece polヴtica de sesiones stateless y orden de filtros de autenticaciИn.
     * Los endpoints /api/** son accesibles, delegando la autenticaciИn a filtros personalizados.</p>
     *
     * @param http HttpSecurity para configurar
     * @return SecurityFilterChain configurado
     * @throws Exception si hay error en la configuraciИn
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/health", "/error").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                log.debug("ĐY\"? AuthenticationEntryPoint: {}", request.getRequestURI()))
                )
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("胑\"' Cadena de seguridad configurada - Orden: ServiceTokenFilter -> JwtAuthenticationFilter");

        return http.build();
    }
}
