package com.ondra.recomendaciones.services;

import com.ondra.recomendaciones.clients.ContenidosClient;
import com.ondra.recomendaciones.dto.AgregarPreferenciasDTO;
import com.ondra.recomendaciones.dto.PreferenciaGeneroDTO;
import com.ondra.recomendaciones.dto.PreferenciasResponseDTO;
import com.ondra.recomendaciones.exceptions.*;
import com.ondra.recomendaciones.models.dao.PreferenciaGenero;
import com.ondra.recomendaciones.repositories.PreferenciaGeneroRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de lógica de negocio para gestión de preferencias de géneros.
 *
 * <p>Gestiona las preferencias de géneros de los usuarios, incluyendo validación
 * de existencia de géneros mediante comunicación con el microservicio de contenidos.
 */
@Slf4j
@Service
public class PreferenciasService {

    @Autowired
    private PreferenciaGeneroRepository preferenciaGeneroRepository;

    @Autowired
    private ContenidosClient contenidosClient;

    /**
     * Obtiene las preferencias de géneros de un usuario con nombres enriquecidos.
     *
     * @param idUsuario ID del usuario
     * @return Lista de preferencias con IDs y nombres de géneros
     */
    public List<PreferenciaGeneroDTO> obtenerPreferencias(Long idUsuario) {
        log.debug("📋 Obteniendo preferencias del usuario {}", idUsuario);

        List<PreferenciaGenero> preferencias = preferenciaGeneroRepository.findByIdUsuario(idUsuario);
        List<PreferenciaGeneroDTO> resultado = new ArrayList<>();

        for (PreferenciaGenero preferencia : preferencias) {
            String nombreGenero = contenidosClient.obtenerNombreGenero(preferencia.getIdGenero());
            if (nombreGenero == null) {
                nombreGenero = "Género " + preferencia.getIdGenero();
            }

            resultado.add(PreferenciaGeneroDTO.builder()
                    .idGenero(preferencia.getIdGenero())
                    .nombreGenero(nombreGenero)
                    .build());
        }

        log.debug("✅ Obtenidas {} preferencias para usuario {}", resultado.size(), idUsuario);
        return resultado;
    }

    /**
     * Agrega nuevas preferencias de géneros a un usuario.
     *
     * <p>Valida que los géneros existan en el microservicio de contenidos,
     * elimina duplicados y gestiona géneros ya existentes.
     *
     * @param idUsuario ID del usuario
     * @param dto DTO con los IDs de géneros a agregar
     * @return Respuesta con estadísticas y preferencias actualizadas
     * @throws InvalidDataException si la lista de géneros está vacía
     * @throws InvalidGenreException si algún género no existe
     */
    @Transactional
    public PreferenciasResponseDTO agregarPreferencias(Long idUsuario, AgregarPreferenciasDTO dto) {
        log.info("➕ Agregando preferencias para usuario {}", idUsuario);

        if (dto.getIdsGeneros() == null || dto.getIdsGeneros().isEmpty()) {
            throw new InvalidDataException("La lista de géneros no puede estar vacía");
        }

        Set<Long> idsGenerosUnicos = new HashSet<>(dto.getIdsGeneros());

        // Validar existencia de géneros
        for (Long idGenero : idsGenerosUnicos) {
            if (!contenidosClient.existeGenero(idGenero)) {
                throw new InvalidGenreException("El género con ID " + idGenero + " no existe");
            }
        }

        int generosAgregados = 0;
        int generosDuplicados = 0;

        // Agregar géneros no existentes
        for (Long idGenero : idsGenerosUnicos) {
            boolean yaExiste = preferenciaGeneroRepository.existsByIdUsuarioAndIdGenero(idUsuario, idGenero);

            if (yaExiste) {
                generosDuplicados++;
            } else {
                PreferenciaGenero preferencia = PreferenciaGenero.builder()
                        .idUsuario(idUsuario)
                        .idGenero(idGenero)
                        .build();
                preferenciaGeneroRepository.save(preferencia);
                generosAgregados++;
            }
        }

        log.info("✅ Preferencias agregadas - Nuevos: {}, Duplicados: {}", generosAgregados, generosDuplicados);

        List<PreferenciaGeneroDTO> preferenciasActualizadas = obtenerPreferencias(idUsuario);

        return PreferenciasResponseDTO.builder()
                .mensaje("Preferencias añadidas exitosamente")
                .generosAgregados(generosAgregados)
                .generosDuplicados(generosDuplicados)
                .preferencias(preferenciasActualizadas)
                .build();
    }

    /**
     * Elimina una preferencia de género específica de un usuario.
     *
     * @param idUsuario ID del usuario
     * @param idGenero ID del género a eliminar
     * @throws PreferenciaNotFoundException si la preferencia no existe
     */
    @Transactional
    public void eliminarPreferencia(Long idUsuario, Long idGenero) {
        log.info("🗑️ Eliminando preferencia género {} del usuario {}", idGenero, idUsuario);

        PreferenciaGenero preferencia = preferenciaGeneroRepository
                .findByIdUsuarioAndIdGenero(idUsuario, idGenero)
                .orElseThrow(() -> new PreferenciaNotFoundException(
                        "El usuario no tiene el género con ID " + idGenero + " en sus preferencias"
                ));

        preferenciaGeneroRepository.delete(preferencia);
        log.info("✅ Preferencia eliminada correctamente");
    }

    /**
     * Elimina todas las preferencias de géneros de un usuario.
     *
     * @param idUsuario ID del usuario
     */
    @Transactional
    public void eliminarTodasPreferencias(Long idUsuario) {
        log.info("🗑️ Eliminando todas las preferencias del usuario {}", idUsuario);
        preferenciaGeneroRepository.deleteByIdUsuario(idUsuario);
        log.info("✅ Todas las preferencias eliminadas");
    }

    /**
     * Verifica que el usuario autenticado sea propietario del recurso.
     *
     * <p>Permite acceso sin validación si es una petición service-to-service.
     *
     * @param idUsuarioAutenticado ID del usuario autenticado (del JWT)
     * @param idUsuario ID del usuario del recurso
     * @param isServiceRequest true si es petición entre servicios
     * @throws ForbiddenAccessException si no es el propietario
     */
    public void verificarPropietario(Long idUsuarioAutenticado, Long idUsuario, boolean isServiceRequest) {
        if (isServiceRequest) {
            log.debug("🔓 Acceso permitido: petición service-to-service");
            return;
        }

        if (idUsuarioAutenticado == null || !idUsuarioAutenticado.equals(idUsuario)) {
            log.warn("🚫 Acceso denegado: usuario {} intentó modificar recursos de {}",
                    idUsuarioAutenticado, idUsuario);
            throw new ForbiddenAccessException("No tienes permiso para modificar las preferencias de otro usuario");
        }

        log.debug("🔓 Acceso permitido: usuario es propietario");
    }
}