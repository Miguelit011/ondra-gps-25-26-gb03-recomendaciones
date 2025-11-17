package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simplificado para canciones recomendadas.
 *
 * <p>Contiene información básica de la canción. El frontend debe consultar
 * el microservicio de Contenidos para obtener detalles completos.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancionRecomendadaDTO {

    private Long idCancion;
    private String titulo;
    private Long idGenero;
    private String nombreGenero;
}