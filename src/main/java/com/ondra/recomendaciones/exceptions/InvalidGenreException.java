package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando se intenta operar con un género musical inválido.
 *
 * <p>Utilizada cuando el género especificado no existe o no es válido
 * en el sistema.</p>
 */
public class InvalidGenreException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del error relacionado con el género
     */
    public InvalidGenreException(String message) {
        super(message);
    }
}