package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando los datos proporcionados no son válidos.
 *
 * <p>Utilizada para validaciones de negocio donde los datos no cumplen
 * con los requisitos establecidos.</p>
 */
public class InvalidDataException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del error de validación
     */
    public InvalidDataException(String message) {
        super(message);
    }
}