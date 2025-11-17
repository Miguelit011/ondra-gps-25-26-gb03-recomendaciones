package com.ondra.recomendaciones.controllers;

import com.ondra.recomendaciones.dto.RecomendacionesResponseDTO;
import com.ondra.recomendaciones.services.RecomendacionesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de recomendaciones personalizadas.
 *
 * <p>Expone endpoint para obtener recomendaciones de canciones y álbumes
 * basadas en las preferencias musicales del usuario.
 */
@RestController
@RequestMapping("/usuarios/{id}/recomendaciones")
@CrossOrigin(origins = "*")
public class RecomendacionesController {

    @Autowired
    private RecomendacionesService recomendacionesService;

    /**
     * Obtiene recomendaciones personalizadas para un usuario.
     *
     * <p>Genera recomendaciones basadas en los géneros preferidos del usuario,
     * excluyendo contenido que ya posee. Valida que el usuario autenticado
     * sea el propietario o que sea una petición entre servicios.
     *
     * @param id ID del usuario
     * @param tipo Tipo de contenido: "cancion", "album" o "ambos"
     * @param limite Número máximo de recomendaciones (1-50)
     * @param request HttpServletRequest para obtener atributos de autenticación
     * @return Respuesta con recomendaciones generadas
     */
    @GetMapping
    public ResponseEntity<RecomendacionesResponseDTO> obtenerRecomendaciones(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ambos") String tipo,
            @RequestParam(defaultValue = "20") int limite,
            HttpServletRequest request
    ) {
        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        recomendacionesService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        RecomendacionesResponseDTO recomendaciones =
                recomendacionesService.obtenerRecomendaciones(id, tipo, limite);

        return ResponseEntity.ok(recomendaciones);
    }
}