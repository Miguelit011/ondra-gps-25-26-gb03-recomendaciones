package com.ondra.recomendaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del microservicio de recomendaciones.
 *
 * <p>Microservicio encargado de generar recomendaciones personalizadas
 * de contenido musical basadas en las preferencias de géneros de los usuarios.
 */
@SpringBootApplication
public class RecomendacionesServiceApplication {

    /**
     * Punto de entrada de la aplicación Spring Boot.
     *
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(RecomendacionesServiceApplication.class, args);
    }
}