package com.ondra.recomendaciones.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración de RestTemplate para comunicación HTTP entre microservicios.
 *
 * <p>Configura timeouts, interceptores y manejo de errores para las llamadas HTTP.</p>
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate configurado para comunicación entre microservicios.
     *
     * @param builder RestTemplateBuilder proporcionado por Spring Boot
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("🔧 Configurando RestTemplate para comunicación entre microservicios");

        return builder
                // Timeouts
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))

                // Interceptor para logging
                .interceptors(loggingInterceptor())

                // Manejo de errores personalizado
                .build();
    }

    /**
     * Interceptor para logging de peticiones y respuestas HTTP.
     *
     * @return ClientHttpRequestInterceptor
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("📤 Request: {} {} - Body size: {} bytes",
                    request.getMethod(),
                    request.getURI(),
                    body.length);

            var response = execution.execute(request, body);

            log.debug("📥 Response: {} - Status: {}",
                    request.getURI(),
                    response.getStatusCode());

            return response;
        };
    }
}