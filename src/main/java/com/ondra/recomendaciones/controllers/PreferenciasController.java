package com.ondra.recomendaciones.controllers;

import com.ondra.recomendaciones.dto.AgregarPreferenciasDTO;
import com.ondra.recomendaciones.dto.PreferenciaGeneroDTO;
import com.ondra.recomendaciones.dto.PreferenciasResponseDTO;
import com.ondra.recomendaciones.services.PreferenciasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de preferencias musicales de usuarios.
 *
 * <p>Permite consultar, agregar y eliminar preferencias de géneros musicales
 * asociadas a cada usuario del sistema.</p>
 */
@Slf4j
@RestController
@RequestMapping("/usuarios")
public class PreferenciasController {

    @Autowired
    private PreferenciasService preferenciasService;

    /**
     * Obtiene las preferencias de géneros musicales de un usuario.
     *
     * @param id identificador del usuario
     * @return lista de preferencias del usuario
     */
    @GetMapping("/{id}/preferencias")
    public ResponseEntity<List<PreferenciaGeneroDTO>> obtenerPreferencias(
            @PathVariable Long id
    ) {
        log.info("📋 GET /api/usuarios/{}/preferencias", id);
        List<PreferenciaGeneroDTO> preferencias = preferenciasService.obtenerPreferencias(id);
        return ResponseEntity.ok(preferencias);
    }

    /**
     * Agrega nuevas preferencias de géneros musicales a un usuario.
     *
     * @param id identificador del usuario
     * @param dto objeto con las preferencias a agregar
     * @param request contexto de la petición HTTP para validación de permisos
     * @return respuesta con las preferencias agregadas y errores si los hubiera
     */
    @PostMapping("/{id}/preferencias")
    public ResponseEntity<PreferenciasResponseDTO> agregarPreferencias(
            @PathVariable Long id,
            @Valid @RequestBody AgregarPreferenciasDTO dto,
            HttpServletRequest request
    ) {
        log.info("➕ POST /api/usuarios/{}/preferencias - Body: {}", id, dto);

        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        log.debug("🔐 Usuario autenticado: {}, Service request: {}", idUsuarioAutenticado, isServiceRequest);

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        PreferenciasResponseDTO response = preferenciasService.agregarPreferencias(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina todas las preferencias de géneros de un usuario.
     *
     * @param id identificador del usuario
     * @param request contexto de la petición HTTP para validación de permisos
     * @return respuesta vacía con código 200
     */
    @DeleteMapping("/{id}/preferencias")
    public ResponseEntity<Void> eliminarTodasPreferencias(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        log.info("🗑️ DELETE /api/usuarios/{}/preferencias", id);

        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        preferenciasService.eliminarTodasPreferencias(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina una preferencia de género específica de un usuario.
     *
     * @param id identificador del usuario
     * @param idGenero identificador del género a eliminar
     * @param request contexto de la petición HTTP para validación de permisos
     * @return respuesta vacía con código 200
     */
    @DeleteMapping("/{id}/preferencias/{idGenero}")
    public ResponseEntity<Void> eliminarPreferencia(
            @PathVariable Long id,
            @PathVariable Long idGenero,
            HttpServletRequest request
    ) {
        log.info("🗑️ DELETE /api/usuarios/{}/preferencias/{}", id, idGenero);

        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        preferenciasService.eliminarPreferencia(id, idGenero);
        return ResponseEntity.ok().build();
    }
}