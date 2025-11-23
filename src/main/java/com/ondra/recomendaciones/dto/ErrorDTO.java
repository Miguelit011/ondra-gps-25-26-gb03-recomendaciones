package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de error estandarizadas.
 *
 * <p>Proporciona una estructura uniforme para errores de la API.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDTO {

    /**
     * Código de error.
     */
    private String error;

    /**
     * Mensaje descriptivo del error.
     */
    private String message;

    /**
     * Código de estado HTTP.
     */
    private Integer statusCode;

    /**
     * Marca de tiempo del error.
     */
    private String timestamp;
}