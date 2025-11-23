package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta al agregar preferencias de géneros.
 *
 * <p>Incluye estadísticas de la operación y lista actualizada de preferencias.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciasResponseDTO {

    /**
     * Mensaje descriptivo del resultado.
     */
    private String mensaje;

    /**
     * Cantidad de géneros agregados exitosamente.
     */
    private Integer generosAgregados;

    /**
     * Cantidad de géneros que ya existían.
     */
    private Integer generosDuplicados;

    /**
     * Lista actualizada de preferencias del usuario.
     */
    private List<PreferenciaGeneroDTO> preferencias;
}