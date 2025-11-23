package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de recomendaciones personalizadas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecomendacionesResponseDTO {

    /**
     * Identificador del usuario.
     */
    private Long idUsuario;

    /**
     * Total de recomendaciones generadas.
     */
    private Integer totalRecomendaciones;

    /**
     * Lista de canciones recomendadas.
     */
    private List<CancionRecomendadaDTO> canciones;

    /**
     * Lista de álbumes recomendados.
     */
    private List<AlbumRecomendadoDTO> albumes;
}