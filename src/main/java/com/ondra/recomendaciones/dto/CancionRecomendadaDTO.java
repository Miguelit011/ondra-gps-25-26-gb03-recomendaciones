package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para canciones recomendadas.
 *
 * <p>Contiene información básica de la canción. El cliente debe consultar
 * el microservicio de contenidos para obtener detalles completos.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancionRecomendadaDTO {

    /**
     * Identificador único de la canción.
     */
    private Long idCancion;

    /**
     * Título de la canción.
     */
    private String titulo;

    /**
     * Identificador del género musical.
     */
    private Long idGenero;

    /**
     * Nombre del género musical.
     */
    private String nombreGenero;
}