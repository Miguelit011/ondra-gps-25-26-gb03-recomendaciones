package com.ondra.recomendaciones.services;

import com.ondra.recomendaciones.clients.ContenidosClient;
import com.ondra.recomendaciones.dto.AlbumRecomendadoDTO;
import com.ondra.recomendaciones.dto.CancionRecomendadaDTO;
import com.ondra.recomendaciones.dto.RecomendacionesResponseDTO;
import com.ondra.recomendaciones.exceptions.ForbiddenAccessException;
import com.ondra.recomendaciones.exceptions.InvalidParameterException;
import com.ondra.recomendaciones.repositories.PreferenciaGeneroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de generación de recomendaciones personalizadas.
 *
 * <p>Genera recomendaciones de canciones y álbumes basadas en géneros preferidos,
 * excluyendo contenido que el usuario ya posee o que el artista ya creó.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecomendacionesService {

    private static final String TIPO_CANCION = "cancion";
    private static final String TIPO_ALBUM = "album";
    private static final String TIPO_AMBOS = "ambos";
    private static final String MENSAJE_TIPO_INVALIDO = "El tipo debe ser 'cancion', 'album' o 'ambos'";

    private final PreferenciaGeneroRepository preferenciaGeneroRepository;
    private final ContenidosClient contenidosClient;

    /**
     * Genera recomendaciones personalizadas basadas en preferencias del usuario.
     *
     * @param idUsuario ID del usuario para buscar preferencias
     * @param idArtista ID del artista para excluir contenido propio (null si no es artista)
     * @param tipo tipo de recomendaciones: "cancion", "album", "ambos"
     * @param limite número máximo de recomendaciones (1-50)
     * @return respuesta con recomendaciones generadas
     * @throws InvalidParameterException si tipo o límite son inválidos
     */
    public RecomendacionesResponseDTO obtenerRecomendaciones(
            Long idUsuario,
            Long idArtista,
            String tipo,
            int limite
    ) {
        boolean esArtista = (idArtista != null);

        log.info("🎵 Generando recomendaciones para usuario {} {} - Tipo: {}, Límite: {}",
                idUsuario, esArtista ? "(artista: " + idArtista + ")" : "", tipo, limite);

        validarParametros(tipo, limite);

        List<Long> generosPreferidos = preferenciaGeneroRepository.findGeneroIdsByIdUsuario(idUsuario);
        if (generosPreferidos.isEmpty()) {
            log.warn("⚠️ Usuario {} sin preferencias configuradas", idUsuario);
            return construirRespuestaSinPreferencias(idUsuario);
        }

        Set<Long> cancionesExistentes = obtenerCancionesAExcluir(idUsuario, idArtista);
        Set<Long> albumesExistentes = obtenerAlbumesAExcluir(idUsuario, idArtista);
        int itemsPorGenero = calcularItemsPorGenero(limite, generosPreferidos.size());

        List<CancionRecomendadaDTO> canciones = obtenerCancionesRecomendadasSiAplica(
                tipo,
                generosPreferidos,
                cancionesExistentes,
                itemsPorGenero,
                limite
        );

        List<AlbumRecomendadoDTO> albumes = obtenerAlbumesRecomendadosSiAplica(
                tipo,
                generosPreferidos,
                albumesExistentes,
                itemsPorGenero,
                limite
        );

        if (esTipoAmbos(tipo)) {
            ajustarLimiteTotalAmbos(canciones, albumes, limite);
        }

        int totalRecomendaciones = canciones.size() + albumes.size();
        log.info("✅ Canciones: {}, Álbumes: {}, Total: {}",
                canciones.size(), albumes.size(), totalRecomendaciones);

        return RecomendacionesResponseDTO.builder()
                .idUsuario(idUsuario)
                .totalRecomendaciones(totalRecomendaciones)
                .canciones(canciones)
                .albumes(albumes)
                .build();
    }

    /**
     * Obtiene las canciones a excluir de las recomendaciones.
     *
     * <p>Para usuarios: compras y favoritos. Para artistas: sus propias canciones.</p>
     *
     * @param idUsuario ID del usuario
     * @param idArtista ID del artista (null si no es artista)
     * @return set de IDs de canciones a excluir
     */
    private Set<Long> obtenerCancionesAExcluir(Long idUsuario, Long idArtista) {
        Set<Long> cancionesExistentes = new HashSet<>();

        if (idArtista != null) {
            log.debug("🎨 Excluyendo canciones del artista {}", idArtista);
            List<CancionRecomendadaDTO> cancionesArtista =
                    contenidosClient.obtenerCancionesPorArtista(idArtista);

            cancionesArtista.forEach(c -> cancionesExistentes.add(c.getIdCancion()));
            log.debug("{} canciones propias excluidas", cancionesExistentes.size());
        } else {
            log.debug("👤 Excluyendo compras y favoritos del usuario {}", idUsuario);
            cancionesExistentes.addAll(contenidosClient.obtenerCancionesUsuario(idUsuario));
            log.debug("{} canciones excluidas", cancionesExistentes.size());
        }

        return cancionesExistentes;
    }

    /**
     * Obtiene los álbumes a excluir de las recomendaciones.
     *
     * <p>Para usuarios: compras y favoritos. Para artistas: sus propios álbumes.</p>
     *
     * @param idUsuario ID del usuario
     * @param idArtista ID del artista (null si no es artista)
     * @return set de IDs de álbumes a excluir
     */
    private Set<Long> obtenerAlbumesAExcluir(Long idUsuario, Long idArtista) {
        Set<Long> albumesExistentes = new HashSet<>();

        if (idArtista != null) {
            log.debug("🎨 Excluyendo álbumes del artista {}", idArtista);
            List<AlbumRecomendadoDTO> albumesArtista =
                    contenidosClient.obtenerAlbumesPorArtista(idArtista);

            albumesArtista.forEach(a -> albumesExistentes.add(a.getIdAlbum()));
            log.debug("{} álbumes propios excluidos", albumesExistentes.size());
        } else {
            log.debug("👤 Excluyendo compras y favoritos del usuario {}", idUsuario);
            albumesExistentes.addAll(contenidosClient.obtenerAlbumesUsuario(idUsuario));
            log.debug("{} álbumes excluidos", albumesExistentes.size());
        }

        return albumesExistentes;
    }

    /**
     * Genera recomendaciones de canciones de los géneros preferidos.
     *
     * @param generosPreferidos lista de IDs de géneros preferidos
     * @param cancionesExistentes set de IDs de canciones a excluir
     * @param itemsPorGenero cantidad de items a solicitar por género
     * @param limiteTotal límite máximo de canciones a retornar
     * @return lista de canciones recomendadas
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
                    .toList();

            todasCanciones.addAll(cancionesFiltradas);

            if (todasCanciones.size() >= limiteTotal) {
                break;
            }
        }

        return new ArrayList<>(todasCanciones.stream()
                .limit(limiteTotal)
                .toList());
    }

    /**
     * Genera recomendaciones de álbumes de los géneros preferidos.
     *
     * @param generosPreferidos lista de IDs de géneros preferidos
     * @param albumesExistentes set de IDs de álbumes a excluir
     * @param itemsPorGenero cantidad de items a solicitar por género
     * @param limiteTotal límite máximo de álbumes a retornar
     * @return lista de álbumes recomendados
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
                    .toList();

            todosAlbumes.addAll(albumesFiltrados);

            if (todosAlbumes.size() >= limiteTotal) {
                break;
            }
        }

        return new ArrayList<>(todosAlbumes.stream()
                .limit(limiteTotal)
                .toList());
    }

    /**
     * Ajusta las listas para mantener el límite total.
     *
     * <p>Distribuye proporcionalmente entre canciones y álbumes si se supera el límite.</p>
     *
     * @param canciones lista de canciones a ajustar
     * @param albumes lista de álbumes a ajustar
     * @param limite límite total de recomendaciones
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
     * Valida los parámetros de entrada.
     *
     * @param tipo tipo de contenido solicitado
     * @param limite límite de recomendaciones
     * @throws InvalidParameterException si los parámetros son inválidos
     */
    private void validarParametros(String tipo, int limite) {
        if (!esTipoValido(tipo)) {
            throw new InvalidParameterException(MENSAJE_TIPO_INVALIDO);
        }

        if (limite < 1 || limite > 50) {
            throw new InvalidParameterException("El límite debe estar entre 1 y 50");
        }
    }

    /**
     * Verifica que el usuario autenticado sea propietario del recurso.
     *
     * <p>Permite acceso sin validación si es petición service-to-service.</p>
     *
     * @param idUsuarioAutenticado ID del usuario autenticado
     * @param idUsuario ID del usuario del recurso
     * @param isServiceRequest true si es petición entre servicios
     * @throws ForbiddenAccessException si no es el propietario
     */
    public void verificarPropietario(Long idUsuarioAutenticado, Long idUsuario, boolean isServiceRequest) {
        if (isServiceRequest) {
            log.debug("🔓 Acceso service-to-service");
            return;
        }

        if (idUsuarioAutenticado == null || !idUsuarioAutenticado.equals(idUsuario)) {
            log.warn("🚫 Usuario {} intentó acceder a recomendaciones de {}", idUsuarioAutenticado, idUsuario);
            throw new ForbiddenAccessException("No tienes permiso para acceder a las recomendaciones de otro usuario");
        }

        log.debug("🔓 Usuario es propietario");
    }

    private List<CancionRecomendadaDTO> obtenerCancionesRecomendadasSiAplica(
            String tipo,
            List<Long> generosPreferidos,
            Set<Long> cancionesExistentes,
            int itemsPorGenero,
            int limite
    ) {
        if (!incluyeCancion(tipo)) {
            return new ArrayList<>();
        }

        return generarRecomendacionesCanciones(
                generosPreferidos,
                cancionesExistentes,
                itemsPorGenero,
                limite
        );
    }

    private List<AlbumRecomendadoDTO> obtenerAlbumesRecomendadosSiAplica(
            String tipo,
            List<Long> generosPreferidos,
            Set<Long> albumesExistentes,
            int itemsPorGenero,
            int limite
    ) {
        if (!incluyeAlbum(tipo)) {
            return new ArrayList<>();
        }

        int limiteAlbumes = esTipoAmbos(tipo) ? limite / 2 : limite;
        return generarRecomendacionesAlbumes(
                generosPreferidos,
                albumesExistentes,
                itemsPorGenero,
                limiteAlbumes
        );
    }

    private int calcularItemsPorGenero(int limite, int cantidadGeneros) {
        return Math.max(1, limite / cantidadGeneros) + 2;
    }

    private boolean esTipoValido(String tipo) {
        return incluyeCancion(tipo) || incluyeAlbum(tipo);
    }

    private boolean incluyeCancion(String tipo) {
        return TIPO_CANCION.equals(tipo) || esTipoAmbos(tipo);
    }

    private boolean incluyeAlbum(String tipo) {
        return TIPO_ALBUM.equals(tipo) || esTipoAmbos(tipo);
    }

    private boolean esTipoAmbos(String tipo) {
        return TIPO_AMBOS.equals(tipo);
    }

    private RecomendacionesResponseDTO construirRespuestaSinPreferencias(Long idUsuario) {
        return RecomendacionesResponseDTO.builder()
                .idUsuario(idUsuario)
                .totalRecomendaciones(0)
                .canciones(new ArrayList<>())
                .albumes(new ArrayList<>())
                .build();
    }
}
