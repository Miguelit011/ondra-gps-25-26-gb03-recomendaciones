package com.ondra.recomendaciones.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración de RestTemplate para comunicación HTTP entre microservicios.
 *
 * <p>Configura timeouts, interceptores de autenticación y logging para las llamadas HTTP.</p>
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${microservices.service-token}")
    private String serviceToken;

    /**
     * Bean de RestTemplate configurado para comunicación entre microservicios.
     *
     * @param builder RestTemplateBuilder proporcionado por Spring Boot
     * @return RestTemplate configurado con timeouts e interceptores
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("🔧 Configurando RestTemplate para comunicación entre microservicios");

        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .interceptors(serviceTokenInterceptor(), loggingInterceptor())
                .build();
    }

    /**
     * Interceptor para autenticación service-to-service mediante token.
     * Agrega el header X-Service-Token a todas las peticiones salientes.
     *
     * @return ClientHttpRequestInterceptor configurado con token de servicio
     */
    private ClientHttpRequestInterceptor serviceTokenInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("X-Service-Token", serviceToken);
            log.debug("🔑 Agregando X-Service-Token a la petición: {}", request.getURI());
            return execution.execute(request, body);
        };
    }

    /**
     * Interceptor para logging de peticiones y respuestas HTTP.
     *
     * @return ClientHttpRequestInterceptor para registro de tráfico HTTP
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