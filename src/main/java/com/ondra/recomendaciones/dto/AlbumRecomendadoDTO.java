package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para álbumes recomendados.
 *
 * <p>Contiene información básica del álbum. El cliente debe consultar
 * el microservicio de contenidos para obtener detalles completos.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumRecomendadoDTO {

    /**
     * Identificador único del álbum.
     */
    private Long idAlbum;

    /**
     * Título del álbum.
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