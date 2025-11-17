package com.ondra.recomendaciones.exceptions;

/**
 * Excepción lanzada cuando no se encuentra un usuario en el sistema.
 *
 * <p>Utilizada cuando se intenta acceder a un usuario que no existe
 * o no está registrado en la base de datos.</p>
 */
public class UsuarioNotFoundException extends RuntimeException {

    /**
     * Constructor con mensaje de error.
     *
     * @param message descripción del error de búsqueda del usuario
     */
    public UsuarioNotFoundException(String message) {
        super(message);
    }
}