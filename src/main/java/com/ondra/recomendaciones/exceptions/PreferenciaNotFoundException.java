package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando no se encuentra una preferencia de género.
 *
 * <p>Utilizada cuando se intenta acceder a una preferencia que no existe
 * en el sistema para el usuario especificado.</p>
 */
public class PreferenciaNotFoundException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del error de búsqueda
     */
    public PreferenciaNotFoundException(String message) {
        super(message);
    }
}