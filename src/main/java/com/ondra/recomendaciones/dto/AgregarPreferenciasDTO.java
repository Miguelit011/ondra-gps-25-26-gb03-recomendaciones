package com.ondra.recomendaciones.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para agregar múltiples preferencias de géneros a un usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgregarPreferenciasDTO {

    /**
     * Lista de IDs de géneros musicales a agregar.
     */
    @NotEmpty(message = "La lista de géneros no puede estar vacía")
    private List<Long> idsGeneros;
}