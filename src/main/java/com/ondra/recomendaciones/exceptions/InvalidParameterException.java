package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando un parámetro de entrada no es válido.
 *
 * <p>Utilizada para validaciones de parámetros en las peticiones HTTP
 * donde el valor proporcionado no cumple con los requisitos esperados.</p>
 */
public class InvalidParameterException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del parámetro inválido
     */
    public InvalidParameterException(String message) {
        super(message);
    }
}