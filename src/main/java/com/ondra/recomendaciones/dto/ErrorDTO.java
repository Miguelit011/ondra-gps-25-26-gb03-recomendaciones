package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de error estandarizadas.
 *
 * <p>Proporciona una estructura uniforme para todos los errores de la API,
 * incluyendo código de estado, mensaje descriptivo y timestamp.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDTO {

    private String error;
    private String message;
    private Integer statusCode;
    private String timestamp;
}