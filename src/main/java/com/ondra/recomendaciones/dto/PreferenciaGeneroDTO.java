package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar una preferencia de género musical.
 *
 * <p>Contiene el ID del género y su nombre para facilitar
 * la visualización en el frontend.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaGeneroDTO {

    private Long idGenero;
    private String nombreGenero;
}