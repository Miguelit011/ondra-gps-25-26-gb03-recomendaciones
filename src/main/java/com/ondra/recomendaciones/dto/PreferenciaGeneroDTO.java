package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar una preferencia de género musical.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaGeneroDTO {

    /**
     * Identificador del género musical.
     */
    private Long idGenero;

    /**
     * Nombre del género musical.
     */
    private String nombreGenero;
}