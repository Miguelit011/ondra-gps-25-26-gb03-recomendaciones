package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado para álbumes recomendados.
 *
 * <p>Contiene información básica del álbum. El frontend debe consultar
 * el microservicio de Contenidos para obtener detalles completos.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumRecomendadoDTO {

    private Long idAlbum;
    private String titulo;
    private Long idGenero;
    private String nombreGenero;
}