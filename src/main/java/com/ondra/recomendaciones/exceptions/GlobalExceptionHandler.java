package com.ondra.recomendaciones.exceptions;

import com.ondra.recomendaciones.dto.ErrorDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API de recomendaciones.
 *
 * <p>Centraliza el manejo de errores de la aplicación, transformando excepciones
 * en respuestas HTTP estandarizadas con formato ErrorDTO.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones cuando no se encuentra un usuario.
     *
     * @param ex excepción de usuario no encontrado
     * @return respuesta HTTP 404 con detalles del error
     */
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleUsuarioNotFound(UsuarioNotFoundException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("USER_NOT_FOUND")
                .message(ex.getMessage())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja excepciones cuando se proporciona un género inválido.
     *
     * @param ex excepción de género inválido
     * @return respuesta HTTP 400 con detalles del error
     */
    @ExceptionHandler(InvalidGenreException.class)
    public ResponseEntity<ErrorDTO> handleInvalidGenre(InvalidGenreException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("INVALID_GENRE")
                .message(ex.getMessage())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja excepciones cuando los datos proporcionados no son válidos.
     *
     * @param ex excepción de datos inválidos
     * @return respuesta HTTP 400 con detalles del error
     */
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorDTO> handleInvalidData(InvalidDataException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("INVALID_DATA")
                .message(ex.getMessage())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja excepciones cuando no se encuentra una preferencia.
     *
     * @param ex excepción de preferencia no encontrada
     * @return respuesta HTTP 404 con detalles del error
     */
    @ExceptionHandler(PreferenciaNotFoundException.class)
    public ResponseEntity<ErrorDTO> handlePreferenciaNotFound(PreferenciaNotFoundException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("PREFERENCE_NOT_FOUND")
                .message(ex.getMessage())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Maneja excepciones cuando un usuario intenta acceder a recursos sin permisos.
     *
     * @param ex excepción de acceso prohibido
     * @return respuesta HTTP 403 con detalles del error
     */
    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorDTO> handleForbiddenAccess(ForbiddenAccessException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("FORBIDDEN_ACCESS")
                .message(ex.getMessage())
                .statusCode(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Maneja excepciones cuando un parámetro de entrada no es válido.
     *
     * @param ex excepción de parámetro inválido
     * @return respuesta HTTP 400 con detalles del error
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorDTO> handleInvalidParameter(InvalidParameterException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("INVALID_PARAMETER")
                .message(ex.getMessage())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja excepciones cuando el token JWT ha expirado.
     *
     * @param ex excepción de token expirado
     * @return respuesta HTTP 401 con detalles del error
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorDTO> handleExpiredJwt(ExpiredJwtException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("TOKEN_EXPIRED")
                .message("El token JWT ha expirado")
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Maneja excepciones cuando el token JWT no es válido.
     *
     * @param ex excepción de token inválido
     * @return respuesta HTTP 401 con detalles del error
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorDTO> handleInvalidJwt(JwtException ex) {
        ErrorDTO error = ErrorDTO.builder()
                .error("INVALID_TOKEN")
                .message("Token JWT inválido")
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Maneja errores de validación en argumentos de métodos anotados con @Valid.
     *
     * @param ex excepción de validación de argumentos
     * @return respuesta HTTP 400 con detalles de los errores de validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("⚠️ Error de validación: {}", errors);

        ErrorDTO error = ErrorDTO.builder()
                .error("VALIDATION_ERROR")
                .message("Error de validación: " + errors)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja excepciones generales no controladas específicamente.
     *
     * @param ex excepción general
     * @return respuesta HTTP 500 con mensaje genérico de error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleException(Exception ex) {
        log.error("❌ Error interno no controlado: {}", ex.getMessage(), ex);

        ErrorDTO error = ErrorDTO.builder()
                .error("INTERNAL_ERROR")
                .message("Ha ocurrido un error interno en el servidor")
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}