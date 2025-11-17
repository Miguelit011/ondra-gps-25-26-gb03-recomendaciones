package com.ondra.recomendaciones.controllers;

import com.ondra.recomendaciones.dto.AgregarPreferenciasDTO;
import com.ondra.recomendaciones.dto.PreferenciaGeneroDTO;
import com.ondra.recomendaciones.dto.PreferenciasResponseDTO;
import com.ondra.recomendaciones.services.PreferenciasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de preferencias de géneros de usuarios.
 *
 * <p>Expone endpoints para consultar, agregar y eliminar preferencias de géneros
 * asociadas a un usuario. Incluye validación de propiedad del recurso.
 */
@RestController
@RequestMapping("/usuarios/{id}/preferencias")
@CrossOrigin(origins = "*")
public class PreferenciasController {

    @Autowired
    private PreferenciasService preferenciasService;

    /**
     * Obtiene las preferencias de géneros de un usuario.
     *
     * @param id ID del usuario
     * @return Lista de preferencias con información de géneros
     */
    @GetMapping
    public ResponseEntity<List<PreferenciaGeneroDTO>> obtenerPreferencias(
            @PathVariable Long id
    ) {
        List<PreferenciaGeneroDTO> preferencias = preferenciasService.obtenerPreferencias(id);
        return ResponseEntity.ok(preferencias);
    }

    /**
     * Agrega nuevas preferencias de géneros a un usuario.
     *
     * <p>Valida que el usuario autenticado sea el propietario o que sea
     * una petición entre servicios.
     *
     * @param id ID del usuario
     * @param dto DTO con los IDs de géneros a agregar
     * @param request HttpServletRequest para obtener atributos de autenticación
     * @return Respuesta con preferencias actualizadas y estadísticas
     */
    @PostMapping
    public ResponseEntity<PreferenciasResponseDTO> agregarPreferencias(
            @PathVariable Long id,
            @Valid @RequestBody AgregarPreferenciasDTO dto,
            HttpServletRequest request
    ) {
        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        PreferenciasResponseDTO response = preferenciasService.agregarPreferencias(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina todas las preferencias de géneros de un usuario.
     *
     * @param id ID del usuario
     * @param request HttpServletRequest para obtener atributos de autenticación
     * @return Respuesta vacía con status 200
     */
    @DeleteMapping
    public ResponseEntity<Void> eliminarTodasPreferencias(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        preferenciasService.eliminarTodasPreferencias(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina una preferencia de género específica de un usuario.
     *
     * @param id ID del usuario
     * @param idGenero ID del género a eliminar
     * @param request HttpServletRequest para obtener atributos de autenticación
     * @return Respuesta vacía con status 200
     */
    @DeleteMapping("/{idGenero}")
    public ResponseEntity<Void> eliminarPreferencia(
            @PathVariable Long id,
            @PathVariable Long idGenero,
            HttpServletRequest request
    ) {
        Long idUsuarioAutenticado = (Long) request.getAttribute("userId");
        boolean isServiceRequest = Boolean.TRUE.equals(request.getAttribute("isServiceRequest"));

        preferenciasService.verificarPropietario(idUsuarioAutenticado, id, isServiceRequest);

        preferenciasService.eliminarPreferencia(id, idGenero);
        return ResponseEntity.ok().build();
    }
}