package com.ondra.recomendaciones.controllers;

import com.ondra.recomendaciones.dto.RecomendacionesResponseDTO;
import com.ondra.recomendaciones.exceptions.ForbiddenAccessException;
import com.ondra.recomendaciones.services.RecomendacionesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para recomendaciones personalizadas de usuarios.
 *
 * <p>Genera recomendaciones de canciones y álbumes basadas en las preferencias
 * musicales del usuario, excluyendo contenido que ya posee o tiene en favoritos.</p>
 */
@Slf4j
@RestController
@RequestMapping("/usuarios/recomendaciones")
@RequiredArgsConstructor
public class RecomendacionesUsuarioController {

    private final RecomendacionesService recomendacionesService;

    /**
     * Obtiene recomendaciones personalizadas para el usuario autenticado.
     * El userId se extrae del token JWT.
     *
     * @param tipo tipo de contenido a recomendar: "cancion", "album" o "ambos"
     * @param limite número máximo de recomendaciones (1-50)
     * @param request contexto de la petición HTTP con datos de autenticación
     * @return respuesta con las recomendaciones generadas
     * @throws ForbiddenAccessException si el token no contiene userId
     */
    @GetMapping
    public ResponseEntity<RecomendacionesResponseDTO> obtenerRecomendaciones(
            @RequestParam(defaultValue = "ambos") String tipo,
            @RequestParam(defaultValue = "20") int limite,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        log.info("📨 GET /usuarios/recomendaciones - userId: {}, tipo: {}, limite: {}",
                userId, tipo, limite);

        if (!isServiceRequest && userId == null) {
            log.warn("⚠️ Acceso denegado: token sin userId");
            throw new ForbiddenAccessException("Token JWT inválido o ausente");
        }

        RecomendacionesResponseDTO recomendaciones =
                recomendacionesService.obtenerRecomendaciones(userId, null, tipo, limite);

        return ResponseEntity.ok(recomendaciones);
    }
}
