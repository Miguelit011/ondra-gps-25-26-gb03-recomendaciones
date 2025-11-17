package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de recomendaciones personalizadas.
 *
 * <p>Contiene listas de canciones y álbumes recomendados basadas
 * en las preferencias musicales del usuario.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecomendacionesResponseDTO {

    private Long idUsuario;
    private Integer totalRecomendaciones;
    private List<CancionRecomendadaDTO> canciones;
    private List<AlbumRecomendadoDTO> albumes;
}