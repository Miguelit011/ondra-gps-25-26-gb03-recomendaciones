package com.ondra.recomendaciones.services;

import com.ondra.recomendaciones.clients.ContenidosClient;
import com.ondra.recomendaciones.dto.AlbumRecomendadoDTO;
import com.ondra.recomendaciones.dto.CancionRecomendadaDTO;
import com.ondra.recomendaciones.dto.RecomendacionesResponseDTO;
import com.ondra.recomendaciones.exceptions.ForbiddenAccessException;
import com.ondra.recomendaciones.exceptions.InvalidParameterException;
import com.ondra.recomendaciones.repositories.PreferenciaGeneroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para generación de recomendaciones personalizadas.
 *
 * <p>Genera recomendaciones de canciones y álbumes basadas en los géneros
 * preferidos del usuario, excluyendo contenido que ya posee.
 */
@Slf4j
@Service
public class RecomendacionesService {

    @Autowired
    private PreferenciaGeneroRepository preferenciaGeneroRepository;

    @Autowired
    private ContenidosClient contenidosClient;

    /**
     * Genera recomendaciones personalizadas basadas en preferencias del usuario.
     *
     * <p>Obtiene contenido de los géneros preferidos, excluye lo que el usuario
     * ya posee y distribuye las recomendaciones según el tipo solicitado.
     *
     * @param idUsuario ID del usuario
     * @param tipo Tipo de recomendaciones: "cancion", "album", "ambos"
     * @param limite Número máximo de recomendaciones (1-50)
     * @return Respuesta con recomendaciones generadas
     * @throws InvalidParameterException si tipo o límite son inválidos
     */
    public RecomendacionesResponseDTO obtenerRecomendaciones(
            Long idUsuario,
            String tipo,
            int limite
    ) {
        log.info("🎵 Generando recomendaciones para usuario {} - Tipo: {}, Límite: {}",
                idUsuario, tipo, limite);

        validarParametros(tipo, limite);

        List<Long> generosPreferidos = preferenciaGeneroRepository.findGeneroIdsByIdUsuario(idUsuario);

        List<CancionRecomendadaDTO> canciones = new ArrayList<>();
        List<AlbumRecomendadoDTO> albumes = new ArrayList<>();

        if (generosPreferidos.isEmpty()) {
            log.warn("⚠️ Usuario {} no tiene preferencias configuradas", idUsuario);
            return RecomendacionesResponseDTO.builder()
                    .idUsuario(idUsuario)
                    .totalRecomendaciones(0)
                    .canciones(canciones)
                    .albumes(albumes)
                    .build();
        }

        Set<Long> cancionesExistentes = new HashSet<>(contenidosClient.obtenerCancionesUsuario(idUsuario));
        Set<Long> albumesExistentes = new HashSet<>(contenidosClient.obtenerAlbumesUsuario(idUsuario));

        int itemsPorGenero = Math.max(1, limite / generosPreferidos.size()) + 2;

        if (tipo.equals("cancion") || tipo.equals("ambos")) {
            canciones = generarRecomendacionesCanciones(
                    generosPreferidos,
                    cancionesExistentes,
                    itemsPorGenero,
                    limite
            );
        }

        if (tipo.equals("album") || tipo.equals("ambos")) {
            int limiteAlbumes = tipo.equals("ambos") ? limite / 2 : limite;
            albumes = generarRecomendacionesAlbumes(
                    generosPreferidos,
                    albumesExistentes,
                    itemsPorGenero,
                    limiteAlbumes
            );
        }

        if (tipo.equals("ambos")) {
            ajustarLimiteTotalAmbos(canciones, albumes, limite);
        }

        int totalRecomendaciones = canciones.size() + albumes.size();
        log.info("✅ Recomendaciones generadas - Canciones: {}, Álbumes: {}, Total: {}",
                canciones.size(), albumes.size(), totalRecomendaciones);

        return RecomendacionesResponseDTO.builder()
                .idUsuario(idUsuario)
                .totalRecomendaciones(totalRecomendaciones)
                .canciones(canciones)
                .albumes(albumes)
                .build();
    }

    /**
     * Genera recomendaciones de canciones de los géneros preferidos.
     *
     * @param generosPreferidos Lista de IDs de géneros preferidos
     * @param cancionesExistentes Set de IDs de canciones que el usuario ya posee
     * @param itemsPorGenero Cantidad de items a solicitar por género
     * @param limiteTotal Límite máximo de canciones a retornar
     * @return Lista de canciones recomendadas
     */
    private List<CancionRecomendadaDTO> generarRecomendacionesCanciones(
            List<Long> generosPreferidos,
            Set<Long> cancionesExistentes,
            int itemsPorGenero,
            int limiteTotal
    ) {
        List<CancionRecomendadaDTO> todasCanciones = new ArrayList<>();

        for (Long idGenero : generosPreferidos) {
            List<CancionRecomendadaDTO> cancionesGenero =
                    contenidosClient.obtenerCancionesPorGenero(idGenero, itemsPorGenero);

            List<CancionRecomendadaDTO> cancionesFiltradas = cancionesGenero.stream()
                    .filter(c -> !cancionesExistentes.contains(c.getIdCancion()))
                    .collect(Collectors.toList());

            todasCanciones.addAll(cancionesFiltradas);

            if (todasCanciones.size() >= limiteTotal) {
                break;
            }
        }

        return todasCanciones.stream()
                .limit(limiteTotal)
                .collect(Collectors.toList());
    }

    /**
     * Genera recomendaciones de álbumes de los géneros preferidos.
     *
     * @param generosPreferidos Lista de IDs de géneros preferidos
     * @param albumesExistentes Set de IDs de álbumes que el usuario ya posee
     * @param itemsPorGenero Cantidad de items a solicitar por género
     * @param limiteTotal Límite máximo de álbumes a retornar
     * @return Lista de álbumes recomendados
     */
    private List<AlbumRecomendadoDTO> generarRecomendacionesAlbumes(
            List<Long> generosPreferidos,
            Set<Long> albumesExistentes,
            int itemsPorGenero,
            int limiteTotal
    ) {
        List<AlbumRecomendadoDTO> todosAlbumes = new ArrayList<>();

        for (Long idGenero : generosPreferidos) {
            List<AlbumRecomendadoDTO> albumesGenero =
                    contenidosClient.obtenerAlbumesPorGenero(idGenero, itemsPorGenero);

            List<AlbumRecomendadoDTO> albumesFiltrados = albumesGenero.stream()
                    .filter(a -> !albumesExistentes.contains(a.getIdAlbum()))
                    .collect(Collectors.toList());

            todosAlbumes.addAll(albumesFiltrados);

            if (todosAlbumes.size() >= limiteTotal) {
                break;
            }
        }

        return todosAlbumes.stream()
                .limit(limiteTotal)
                .collect(Collectors.toList());
    }

    /**
     * Ajusta las listas de canciones y álbumes para mantener el límite total.
     *
     * <p>Reduce las listas si la suma supera el límite, distribuyendo
     * proporcionalmente entre canciones y álbumes.
     *
     * @param canciones Lista de canciones a ajustar
     * @param albumes Lista de álbumes a ajustar
     * @param limite Límite total de recomendaciones
     */
    private void ajustarLimiteTotalAmbos(
            List<CancionRecomendadaDTO> canciones,
            List<AlbumRecomendadoDTO> albumes,
            int limite
    ) {
        int total = canciones.size() + albumes.size();
        if (total > limite) {
            int cancionesMax = limite / 2;
            int albumesMax = limite - cancionesMax;

            if (canciones.size() > cancionesMax) {
                canciones.subList(cancionesMax, canciones.size()).clear();
            }
            if (albumes.size() > albumesMax) {
                albumes.subList(albumesMax, albumes.size()).clear();
            }
        }
    }

    /**
     * Valida los parámetros de entrada para generación de recomendaciones.
     *
     * @param tipo Tipo de contenido solicitado
     * @param limite Límite de recomendaciones
     * @throws InvalidParameterException si los parámetros son inválidos
     */
    private void validarParametros(String tipo, int limite) {
        if (!tipo.equals("cancion") && !tipo.equals("album") && !tipo.equals("ambos")) {
            throw new InvalidParameterException("El tipo debe ser 'cancion', 'album' o 'ambos'");
        }

        if (limite < 1 || limite > 50) {
            throw new InvalidParameterException("El límite debe estar entre 1 y 50");
        }
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
            log.warn("🚫 Acceso denegado: usuario {} intentó acceder a recomendaciones de {}",
                    idUsuarioAutenticado, idUsuario);
            throw new ForbiddenAccessException("No tienes permiso para acceder a las recomendaciones de otro usuario");
        }

        log.debug("🔓 Acceso permitido: usuario es propietario");
    }
}