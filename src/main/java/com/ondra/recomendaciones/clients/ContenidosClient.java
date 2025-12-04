package com.ondra.recomendaciones.clients;

import com.ondra.recomendaciones.dto.AlbumRecomendadoDTO;
import com.ondra.recomendaciones.dto.CancionRecomendadaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cliente para comunicación con el microservicio de contenidos.
 *
 * <p>Gestiona las operaciones relacionadas con géneros, canciones y álbumes,
 * incluyendo consultas de compras y favoritos de usuarios.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContenidosClient {

    private static final String GENEROS_PATH = "/generos/";
    private static final String LIMIT_PARAM = "&limit=";
    private static final String COMPRAS_USUARIO_PATH = "/compras?idUsuario=";
    private static final String FAVORITOS_USUARIO_PATH = "/favoritos?idUsuario=";
    private static final String TIPO_CANCION_LIMIT = "&tipo=CANCION&limit=1000";
    private static final String TIPO_ALBUM_LIMIT = "&tipo=ALBUM&limit=1000";
    private static final String ID_CANCION_KEY = "idCancion";
    private static final String TITULO_CANCION_KEY = "tituloCancion";
    private static final String GENERO_KEY = "genero";
    private static final String ID_ALBUM_KEY = "idAlbum";
    private static final String TITULO_ALBUM_KEY = "tituloAlbum";
    private static final String CANCION_KEY = "cancion";
    private static final String ALBUM_KEY = "album";
    private static final String UNCHECKED = "unchecked";

    private final RestTemplate restTemplate;

    @Value("${microservices.contenidos.url}")
    private String contenidosUrl;

    /**
     * Verifica la existencia de un género en el sistema.
     *
     * @param idGenero identificador del género
     * @return true si el género existe, false en caso contrario
     */
    public boolean existeGenero(Long idGenero) {
        try {
            String url = contenidosUrl + GENEROS_PATH + idGenero + "/existe";
            log.debug("🔍 Verificando existencia de género ID: {}", idGenero);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Boolean.class
            );

            boolean existe = response.getBody() != null && response.getBody();
            log.debug("Género {} existe: {}", idGenero, existe);

            return existe;

        } catch (Exception e) {
            log.error("❌ Error al verificar género {}: {}", idGenero, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el nombre de un género.
     *
     * @param idGenero identificador del género
     * @return nombre del género o null si no existe
     */
    public String obtenerNombreGenero(Long idGenero) {
        try {
            String url = contenidosUrl + GENEROS_PATH + idGenero + "/nombre";
            log.debug("📋 Obteniendo nombre de género ID: {}", idGenero);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            String nombre = response.getBody();
            log.debug("Nombre del género {}: {}", idGenero, nombre);

            return nombre;

        } catch (Exception e) {
            log.error("❌ Error al obtener nombre del género {}: {}", idGenero, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la información completa de un género.
     *
     * @param idGenero identificador del género
     * @return mapa con los datos del género o null si no existe
     */
    public Map<String, Object> obtenerGenero(Long idGenero) {
        try {
            String url = contenidosUrl + GENEROS_PATH + idGenero;
            log.debug("📋 Obteniendo género completo ID: {}", idGenero);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Error al obtener género {}: {}", idGenero, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene canciones filtradas por género.
     *
     * @param idGenero identificador del género musical
     * @param limite número máximo de canciones a retornar
     * @return lista de canciones del género especificado
     */
    public List<CancionRecomendadaDTO> obtenerCancionesPorGenero(Long idGenero, int limite) {
        try {
            String url = contenidosUrl + "/canciones?genreId=" + idGenero + LIMIT_PARAM + limite;
            log.debug("🎵 Obteniendo canciones del género {} (límite: {})", idGenero, limite);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            List<CancionRecomendadaDTO> canciones = new ArrayList<>();
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                @SuppressWarnings(UNCHECKED)
                List<Map<String, Object>> cancionesData = (List<Map<String, Object>>) body.get("canciones");

                if (cancionesData != null) {
                    for (Map<String, Object> cancionData : cancionesData) {
                        Long idCancion = parseLong(cancionData.get(ID_CANCION_KEY));
                        String titulo = (String) cancionData.get(TITULO_CANCION_KEY);
                        String nombreGenero = (String) cancionData.get(GENERO_KEY);

                        if (idCancion == null) {
                            log.warn("⚠️ ID de canción no encontrado en respuesta del género {}", idGenero);
                            continue;
                        }

                        CancionRecomendadaDTO cancion = CancionRecomendadaDTO.builder()
                                .idCancion(idCancion)
                                .titulo(titulo)
                                .idGenero(idGenero)
                                .nombreGenero(nombreGenero)
                                .build();
                        canciones.add(cancion);
                    }
                }
                log.debug("✅ Obtenidas {} canciones del género {}", canciones.size(), idGenero);
            }

            return canciones;

        } catch (Exception e) {
            log.error("❌ Error al obtener canciones del género {}: {}", idGenero, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene todas las canciones de un artista.
     *
     * @param idArtista identificador del artista
     * @return lista de canciones del artista
     */
    public List<CancionRecomendadaDTO> obtenerCancionesPorArtista(Long idArtista) {
        try {
            String url = contenidosUrl + "/canciones/artist/" + idArtista;
            log.debug("🎨 Obteniendo canciones del artista {}", idArtista);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<CancionRecomendadaDTO> canciones = new ArrayList<>();
            if (response.getBody() != null) {
                for (Map<String, Object> cancionData : response.getBody()) {
                    Long idCancion = parseLong(cancionData.get(ID_CANCION_KEY));
                    String genero = (String) cancionData.get(GENERO_KEY);

                    if (idCancion == null) {
                        log.warn("⚠️ ID de canción no encontrado en respuesta del artista {}", idArtista);
                        continue;
                    }

                    CancionRecomendadaDTO cancion = CancionRecomendadaDTO.builder()
                            .idCancion(idCancion)
                            .titulo((String) cancionData.get(TITULO_CANCION_KEY))
                            .idGenero(null)
                            .nombreGenero(genero)
                            .build();
                    canciones.add(cancion);
                }
                log.debug("✅ Obtenidas {} canciones del artista {}", canciones.size(), idArtista);
            }

            return canciones;

        } catch (Exception e) {
            log.error("❌ Error al obtener canciones del artista {}: {}", idArtista, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los identificadores de canciones asociadas al usuario.
     * Combina compras y favoritos eliminando duplicados.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores únicos de canciones
     */
    public List<Long> obtenerCancionesUsuario(Long idUsuario) {
        List<Long> idsCompras = obtenerComprasCancionesUsuario(idUsuario);
        List<Long> idsFavoritos = obtenerFavoritosCancionesUsuario(idUsuario);

        Set<Long> idsUnicos = new HashSet<>();
        idsUnicos.addAll(idsCompras);
        idsUnicos.addAll(idsFavoritos);

        log.debug("Usuario {} - Canciones: {} compradas, {} favoritas, {} únicas",
                idUsuario, idsCompras.size(), idsFavoritos.size(), idsUnicos.size());

        return new ArrayList<>(idsUnicos);
    }

    /**
     * Obtiene los identificadores de canciones compradas por el usuario.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores de canciones compradas
     */
    private List<Long> obtenerComprasCancionesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + COMPRAS_USUARIO_PATH + idUsuario + TIPO_CANCION_LIMIT;
            log.debug("🛒 Obteniendo compras de canciones del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeCompras(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error al obtener compras de canciones del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los identificadores de canciones marcadas como favoritas por el usuario.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores de canciones favoritas
     */
    private List<Long> obtenerFavoritosCancionesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + FAVORITOS_USUARIO_PATH + idUsuario + TIPO_CANCION_LIMIT;
            log.debug("⭐ Obteniendo favoritos de canciones del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeFavoritos(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error al obtener favoritos de canciones del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene álbumes filtrados por género.
     *
     * @param idGenero identificador del género musical
     * @param limite número máximo de álbumes a retornar
     * @return lista de álbumes del género especificado
     */
    public List<AlbumRecomendadoDTO> obtenerAlbumesPorGenero(Long idGenero, int limite) {
        try {
            String url = contenidosUrl + "/albumes?genreId=" + idGenero + LIMIT_PARAM + limite;
            log.debug("💿 Obteniendo álbumes del género {} (límite: {})", idGenero, limite);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            List<AlbumRecomendadoDTO> albumes = new ArrayList<>();
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                @SuppressWarnings(UNCHECKED)
                List<Map<String, Object>> albumesData = (List<Map<String, Object>>) body.get("albumes");

                if (albumesData != null) {
                    for (Map<String, Object> albumData : albumesData) {
                        Long idAlbum = parseLong(albumData.get(ID_ALBUM_KEY));
                        String titulo = (String) albumData.get(TITULO_ALBUM_KEY);
                        String nombreGenero = (String) albumData.get(GENERO_KEY);

                        if (idAlbum == null) {
                            log.warn("⚠️ ID de álbum no encontrado en respuesta del género {}", idGenero);
                            continue;
                        }

                        AlbumRecomendadoDTO album = AlbumRecomendadoDTO.builder()
                                .idAlbum(idAlbum)
                                .titulo(titulo)
                                .idGenero(idGenero)
                                .nombreGenero(nombreGenero)
                                .build();
                        albumes.add(album);
                    }
                }
                log.debug("✅ Obtenidos {} álbumes del género {}", albumes.size(), idGenero);
            }

            return albumes;

        } catch (Exception e) {
            log.error("❌ Error al obtener álbumes del género {}: {}", idGenero, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene todos los álbumes de un artista.
     *
     * @param idArtista identificador del artista
     * @return lista de álbumes del artista
     */
    public List<AlbumRecomendadoDTO> obtenerAlbumesPorArtista(Long idArtista) {
        try {
            String url = contenidosUrl + "/albumes/artist/" + idArtista;
            log.debug("🎨 Obteniendo álbumes del artista {}", idArtista);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<AlbumRecomendadoDTO> albumes = new ArrayList<>();
            if (response.getBody() != null) {
                for (Map<String, Object> albumData : response.getBody()) {
                    Long idAlbum = parseLong(albumData.get(ID_ALBUM_KEY));
                    String genero = (String) albumData.get(GENERO_KEY);

                    if (idAlbum == null) {
                        log.warn("⚠️ ID de álbum no encontrado en respuesta del artista {}", idArtista);
                        continue;
                    }

                    AlbumRecomendadoDTO album = AlbumRecomendadoDTO.builder()
                            .idAlbum(idAlbum)
                            .titulo((String) albumData.get(TITULO_ALBUM_KEY))
                            .idGenero(null)
                            .nombreGenero(genero)
                            .build();
                    albumes.add(album);
                }
                log.debug("✅ Obtenidos {} álbumes del artista {}", albumes.size(), idArtista);
            }

            return albumes;

        } catch (Exception e) {
            log.error("❌ Error al obtener álbumes del artista {}: {}", idArtista, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los identificadores de álbumes asociados al usuario.
     * Combina compras y favoritos eliminando duplicados.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores únicos de álbumes
     */
    public List<Long> obtenerAlbumesUsuario(Long idUsuario) {
        List<Long> idsCompras = obtenerComprasAlbumesUsuario(idUsuario);
        List<Long> idsFavoritos = obtenerFavoritosAlbumesUsuario(idUsuario);

        Set<Long> idsUnicos = new HashSet<>();
        idsUnicos.addAll(idsCompras);
        idsUnicos.addAll(idsFavoritos);

        log.debug("Usuario {} - Álbumes: {} comprados, {} favoritos, {} únicos",
                idUsuario, idsCompras.size(), idsFavoritos.size(), idsUnicos.size());

        return new ArrayList<>(idsUnicos);
    }

    /**
     * Obtiene los identificadores de álbumes comprados por el usuario.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores de álbumes comprados
     */
    private List<Long> obtenerComprasAlbumesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + COMPRAS_USUARIO_PATH + idUsuario + TIPO_ALBUM_LIMIT;
            log.debug("🛒 Obteniendo compras de álbumes del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeCompras(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error al obtener compras de álbumes del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los identificadores de álbumes marcados como favoritos por el usuario.
     *
     * @param idUsuario identificador del usuario
     * @return lista de identificadores de álbumes favoritos
     */
    private List<Long> obtenerFavoritosAlbumesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + FAVORITOS_USUARIO_PATH + idUsuario + TIPO_ALBUM_LIMIT;
            log.debug("⭐ Obteniendo favoritos de álbumes del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeFavoritos(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error al obtener favoritos de álbumes del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extrae identificadores de contenido desde una respuesta de compras.
     *
     * @param data mapa con la estructura de respuesta paginada
     * @return lista de identificadores extraídos
     */
    @SuppressWarnings(UNCHECKED)
    private List<Long> extraerIdsDeCompras(Map<String, Object> data) {
        List<Long> ids = new ArrayList<>();

        if (data == null) {
            return ids;
        }

        try {
            Object comprasObj = data.get("compras");

            if (comprasObj instanceof List) {
                List<Map<String, Object>> compras = (List<Map<String, Object>>) comprasObj;

                for (Map<String, Object> compra : compras) {
                    Object cancionObj = compra.get(CANCION_KEY);
                    Object albumObj = compra.get(ALBUM_KEY);

                    if (cancionObj instanceof Map) {
                        Map<String, Object> cancion = (Map<String, Object>) cancionObj;
                        Long idCancion = parseLong(cancion.get(ID_CANCION_KEY));
                        if (idCancion != null) {
                            ids.add(idCancion);
                        }
                    } else if (albumObj instanceof Map) {
                        Map<String, Object> album = (Map<String, Object>) albumObj;
                        Long idAlbum = parseLong(album.get(ID_ALBUM_KEY));
                        if (idAlbum != null) {
                            ids.add(idAlbum);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al extraer IDs de compras: {}", e.getMessage());
        }

        log.debug("📦 Extraídos {} IDs de compras", ids.size());
        return ids;
    }

    /**
     * Extrae identificadores de contenido desde una respuesta de favoritos.
     *
     * @param data mapa con la estructura de respuesta paginada
     * @return lista de identificadores extraídos
     */
    @SuppressWarnings(UNCHECKED)
    private List<Long> extraerIdsDeFavoritos(Map<String, Object> data) {
        List<Long> ids = new ArrayList<>();

        if (data == null) {
            return ids;
        }

        try {
            Object favoritosObj = data.get("favoritos");

            if (favoritosObj instanceof List) {
                List<Map<String, Object>> favoritos = (List<Map<String, Object>>) favoritosObj;

                for (Map<String, Object> favorito : favoritos) {
                    Object cancionObj = favorito.get(CANCION_KEY);
                    Object albumObj = favorito.get(ALBUM_KEY);

                    if (cancionObj instanceof Map) {
                        Map<String, Object> cancion = (Map<String, Object>) cancionObj;
                        Long idCancion = parseLong(cancion.get(ID_CANCION_KEY));
                        if (idCancion != null) {
                            ids.add(idCancion);
                        }
                    } else if (albumObj instanceof Map) {
                        Map<String, Object> album = (Map<String, Object>) albumObj;
                        Long idAlbum = parseLong(album.get(ID_ALBUM_KEY));
                        if (idAlbum != null) {
                            ids.add(idAlbum);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al extraer IDs de favoritos: {}", e.getMessage());
        }

        log.debug("⭐ Extraídos {} IDs de favoritos", ids.size());
        return ids;
    }

    /**
     * Convierte un objeto a tipo Long de forma segura.
     * Soporta conversión desde Integer, Long y String.
     *
     * @param value objeto a convertir
     * @return valor convertido a Long o null si la conversión falla
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("No se pudo parsear '{}' a Long", value);
                return null;
            }
        }

        log.warn("Tipo no soportado para conversión a Long: {}", value.getClass().getName());
        return null;
    }
}
