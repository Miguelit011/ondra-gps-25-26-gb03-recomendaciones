package com.ondra.recomendaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta al agregar preferencias de géneros.
 *
 * <p>Incluye estadísticas sobre la operación (géneros agregados y duplicados)
 * y la lista actualizada de preferencias del usuario.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciasResponseDTO {

    private String mensaje;
    private Integer generosAgregados;
    private Integer generosDuplicados;
    private List<PreferenciaGeneroDTO> preferencias;
}