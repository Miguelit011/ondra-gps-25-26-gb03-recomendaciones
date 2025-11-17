package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando un usuario intenta acceder a un recurso sin los permisos necesarios.
 *
 * <p>Utilizada para operaciones donde el usuario no tiene autorización sobre
 * el recurso solicitado, como acceder a preferencias de otro usuario.</p>
 */
public class ForbiddenAccessException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del error de acceso
     */
    public ForbiddenAccessException(String message) {
        super(message);
    }
}