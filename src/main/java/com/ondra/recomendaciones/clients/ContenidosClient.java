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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContenidosClient {

    private static final String GENEROS_PATH = "/generos/";
    private static final String EXISTE_SUFFIX = "/existe";
    private static final String NOMBRE_SUFFIX = "/nombre";
    private static final String CANCIONES_PATH = "/canciones";
    private static final String ALBUMES_PATH = "/albumes";
    private static final String ARTIST_SEGMENT = "/artist/";
    private static final String GENRE_QUERY = "genreId=";
    private static final String LIMIT_PARAM = "&limit=";
    private static final String COMPRAS_USUARIO_PATH = "/compras?idUsuario=";
    private static final String FAVORITOS_USUARIO_PATH = "/favoritos?idUsuario=";
    private static final String TIPO_CANCION_LIMIT = "&tipo=CANCION&limit=1000";
    private static final String TIPO_ALBUM_LIMIT = "&tipo=ALBUM&limit=1000";
    private static final String COMPRAS_KEY = "compras";
    private static final String FAVORITOS_KEY = "favoritos";
    private static final String CANCIONES_KEY = "canciones";
    private static final String ALBUMES_KEY = "albumes";
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

    public boolean existeGenero(Long idGenero) {
        try {
            String url = buildGeneroExisteUrl(idGenero);
            log.debug("Verificando existencia de genero ID: {}", idGenero);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Boolean.class
            );

            boolean existe = response.getBody() != null && response.getBody();
            log.debug("Genero {} existe: {}", idGenero, existe);

            return existe;

        } catch (Exception e) {
            log.error("Error al verificar genero {}: {}", idGenero, e.getMessage());
            return false;
        }
    }

    public String obtenerNombreGenero(Long idGenero) {
        try {
            String url = buildGeneroNombreUrl(idGenero);
            log.debug("Obteniendo nombre de genero ID: {}", idGenero);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            String nombre = response.getBody();
            log.debug("Nombre del genero {}: {}", idGenero, nombre);

            return nombre;

        } catch (Exception e) {
            log.error("Error al obtener nombre del genero {}: {}", idGenero, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> obtenerGenero(Long idGenero) {
        try {
            String url = buildGeneroUrl(idGenero);
            log.debug("Obteniendo genero completo ID: {}", idGenero);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyMap();

        } catch (Exception e) {
            log.error("Error al obtener genero {}: {}", idGenero, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<CancionRecomendadaDTO> obtenerCancionesPorGenero(Long idGenero, int limite) {
        try {
            String url = buildCancionesPorGeneroUrl(idGenero, limite);
            log.debug("Obteniendo canciones del genero {} (limite: {})", idGenero, limite);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            List<CancionRecomendadaDTO> canciones = mapearCanciones(
                    obtenerElementos(response.getBody(), CANCIONES_KEY),
                    idGenero,
                    "genero " + idGenero
            );
            log.debug("Obtenidas {} canciones del genero {}", canciones.size(), idGenero);
            return canciones;

        } catch (Exception e) {
            log.error("Error al obtener canciones del genero {}: {}", idGenero, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<CancionRecomendadaDTO> obtenerCancionesPorArtista(Long idArtista) {
        try {
            String url = buildCancionesPorArtistaUrl(idArtista);
            log.debug("Obteniendo canciones del artista {}", idArtista);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<CancionRecomendadaDTO> canciones = mapearCanciones(
                    response.getBody(),
                    null,
                    "artista " + idArtista
            );
            log.debug("Obtenidas {} canciones del artista {}", canciones.size(), idArtista);
            return canciones;

        } catch (Exception e) {
            log.error("Error al obtener canciones del artista {}: {}", idArtista, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Long> obtenerCancionesUsuario(Long idUsuario) {
        List<Long> idsCompras = obtenerComprasCancionesUsuario(idUsuario);
        List<Long> idsFavoritos = obtenerFavoritosCancionesUsuario(idUsuario);

        Set<Long> idsUnicos = new HashSet<>();
        idsUnicos.addAll(idsCompras);
        idsUnicos.addAll(idsFavoritos);

        log.debug("Usuario {} - Canciones: {} compradas, {} favoritas, {} unicas",
                idUsuario, idsCompras.size(), idsFavoritos.size(), idsUnicos.size());

        return new ArrayList<>(idsUnicos);
    }

    private List<Long> obtenerComprasCancionesUsuario(Long idUsuario) {
        try {
            String url = buildComprasUrl(idUsuario, TIPO_CANCION_LIMIT);
            log.debug("Obteniendo compras de canciones del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeCompras(response.getBody());

        } catch (Exception e) {
            log.error("Error al obtener compras de canciones del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Long> obtenerFavoritosCancionesUsuario(Long idUsuario) {
        try {
            String url = buildFavoritosUrl(idUsuario, TIPO_CANCION_LIMIT);
            log.debug("Obteniendo favoritos de canciones del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeFavoritos(response.getBody());

        } catch (Exception e) {
            log.error("Error al obtener favoritos de canciones del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<AlbumRecomendadoDTO> obtenerAlbumesPorGenero(Long idGenero, int limite) {
        try {
            String url = buildAlbumesPorGeneroUrl(idGenero, limite);
            log.debug("Obteniendo albumes del genero {} (limite: {})", idGenero, limite);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            List<AlbumRecomendadoDTO> albumes = mapearAlbumes(
                    obtenerElementos(response.getBody(), ALBUMES_KEY),
                    idGenero,
                    "genero " + idGenero
            );
            log.debug("Obtenidos {} albumes del genero {}", albumes.size(), idGenero);
            return albumes;

        } catch (Exception e) {
            log.error("Error al obtener albumes del genero {}: {}", idGenero, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<AlbumRecomendadoDTO> obtenerAlbumesPorArtista(Long idArtista) {
        try {
            String url = buildAlbumesPorArtistaUrl(idArtista);
            log.debug("Obteniendo albumes del artista {}", idArtista);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<AlbumRecomendadoDTO> albumes = mapearAlbumes(
                    response.getBody(),
                    null,
                    "artista " + idArtista
            );
            log.debug("Obtenidos {} albumes del artista {}", albumes.size(), idArtista);
            return albumes;

        } catch (Exception e) {
            log.error("Error al obtener albumes del artista {}: {}", idArtista, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Long> obtenerAlbumesUsuario(Long idUsuario) {
        List<Long> idsCompras = obtenerComprasAlbumesUsuario(idUsuario);
        List<Long> idsFavoritos = obtenerFavoritosAlbumesUsuario(idUsuario);

        Set<Long> idsUnicos = new HashSet<>();
        idsUnicos.addAll(idsCompras);
        idsUnicos.addAll(idsFavoritos);

        log.debug("Usuario {} - Albumes: {} comprados, {} favoritos, {} unicos",
                idUsuario, idsCompras.size(), idsFavoritos.size(), idsUnicos.size());

        return new ArrayList<>(idsUnicos);
    }

    private List<Long> obtenerComprasAlbumesUsuario(Long idUsuario) {
        try {
            String url = buildComprasUrl(idUsuario, TIPO_ALBUM_LIMIT);
            log.debug("Obteniendo compras de albumes del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeCompras(response.getBody());

        } catch (Exception e) {
            log.error("Error al obtener compras de albumes del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Long> obtenerFavoritosAlbumesUsuario(Long idUsuario) {
        try {
            String url = buildFavoritosUrl(idUsuario, TIPO_ALBUM_LIMIT);
            log.debug("Obteniendo favoritos de albumes del usuario {}", idUsuario);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return extraerIdsDeFavoritos(response.getBody());

        } catch (Exception e) {
            log.error("Error al obtener favoritos de albumes del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings(UNCHECKED)
    private List<Long> extraerIdsDeCompras(Map<String, Object> data) {
        return extraerIdsDesdeColeccion(data, COMPRAS_KEY);
    }

    @SuppressWarnings(UNCHECKED)
    private List<Long> extraerIdsDeFavoritos(Map<String, Object> data) {
        return extraerIdsDesdeColeccion(data, FAVORITOS_KEY);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("No se pudo parsear '{}' a Long", value);
                return null;
            }
        }

        log.warn("Tipo no soportado para conversion a Long: {}", value.getClass().getName());
        return null;
    }

    private String buildGeneroUrl(Long idGenero) {
        return contenidosUrl + GENEROS_PATH + idGenero;
    }

    private String buildGeneroExisteUrl(Long idGenero) {
        return buildGeneroUrl(idGenero) + EXISTE_SUFFIX;
    }

    private String buildGeneroNombreUrl(Long idGenero) {
        return buildGeneroUrl(idGenero) + NOMBRE_SUFFIX;
    }

    private String buildCancionesPorGeneroUrl(Long idGenero, int limite) {
        return contenidosUrl + CANCIONES_PATH + "?" + GENRE_QUERY + idGenero + LIMIT_PARAM + limite;
    }

    private String buildAlbumesPorGeneroUrl(Long idGenero, int limite) {
        return contenidosUrl + ALBUMES_PATH + "?" + GENRE_QUERY + idGenero + LIMIT_PARAM + limite;
    }

    private String buildCancionesPorArtistaUrl(Long idArtista) {
        return contenidosUrl + CANCIONES_PATH + ARTIST_SEGMENT + idArtista;
    }

    private String buildAlbumesPorArtistaUrl(Long idArtista) {
        return contenidosUrl + ALBUMES_PATH + ARTIST_SEGMENT + idArtista;
    }

    private String buildComprasUrl(Long idUsuario, String tipoLimit) {
        return contenidosUrl + COMPRAS_USUARIO_PATH + idUsuario + tipoLimit;
    }

    private String buildFavoritosUrl(Long idUsuario, String tipoLimit) {
        return contenidosUrl + FAVORITOS_USUARIO_PATH + idUsuario + tipoLimit;
    }

    @SuppressWarnings(UNCHECKED)
    private List<Map<String, Object>> obtenerElementos(Map<String, Object> body, String key) {
        if (body == null) {
            return Collections.emptyList();
        }

        Object data = body.get(key);
        if (data instanceof List<?> lista) {
            return (List<Map<String, Object>>) lista;
        }

        return Collections.emptyList();
    }

    private List<CancionRecomendadaDTO> mapearCanciones(List<Map<String, Object>> cancionesData,
                                                       Long idGenero,
                                                       String contexto) {
        List<CancionRecomendadaDTO> canciones = new ArrayList<>();
        if (cancionesData == null) {
            return canciones;
        }

        for (Map<String, Object> cancionData : cancionesData) {
            Long idCancion = parseLong(cancionData.get(ID_CANCION_KEY));
            if (idCancion == null) {
                log.warn("ID de cancion no encontrado en respuesta del {}", contexto);
                continue;
            }

            canciones.add(CancionRecomendadaDTO.builder()
                    .idCancion(idCancion)
                    .titulo((String) cancionData.get(TITULO_CANCION_KEY))
                    .idGenero(idGenero)
                    .nombreGenero((String) cancionData.get(GENERO_KEY))
                    .build());
        }

        return canciones;
    }

    private List<AlbumRecomendadoDTO> mapearAlbumes(List<Map<String, Object>> albumesData,
                                                   Long idGenero,
                                                   String contexto) {
        List<AlbumRecomendadoDTO> albumes = new ArrayList<>();
        if (albumesData == null) {
            return albumes;
        }

        for (Map<String, Object> albumData : albumesData) {
            Long idAlbum = parseLong(albumData.get(ID_ALBUM_KEY));
            if (idAlbum == null) {
                log.warn("ID de album no encontrado en respuesta del {}", contexto);
                continue;
            }

            albumes.add(AlbumRecomendadoDTO.builder()
                    .idAlbum(idAlbum)
                    .titulo((String) albumData.get(TITULO_ALBUM_KEY))
                    .idGenero(idGenero)
                    .nombreGenero((String) albumData.get(GENERO_KEY))
                    .build());
        }

        return albumes;
    }

    @SuppressWarnings(UNCHECKED)
    private List<Long> extraerIdsDesdeColeccion(Map<String, Object> data, String coleccionKey) {
        List<Long> ids = new ArrayList<>();

        if (data == null) {
            return ids;
        }

        try {
            Object coleccionObj = data.get(coleccionKey);

            if (coleccionObj instanceof List<?> coleccion) {
                for (Object elemento : coleccion) {
                    if (elemento instanceof Map<?, ?> contenido) {
                        agregarIdContenido(ids, contenido);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al extraer IDs de {}: {}", coleccionKey, e.getMessage());
        }

        log.debug("Extraidos {} IDs de {}", ids.size(), coleccionKey);
        return ids;
    }

    private void agregarIdContenido(List<Long> ids, Map<?, ?> contenido) {
        Object cancionObj = contenido.get(CANCION_KEY);
        Object albumObj = contenido.get(ALBUM_KEY);

        if (cancionObj instanceof Map<?, ?> cancion) {
            Long idCancion = parseLong(cancion.get(ID_CANCION_KEY));
            if (idCancion != null) {
                ids.add(idCancion);
            }
        } else if (albumObj instanceof Map<?, ?> album) {
            Long idAlbum = parseLong(album.get(ID_ALBUM_KEY));
            if (idAlbum != null) {
                ids.add(idAlbum);
            }
        }
    }
}
