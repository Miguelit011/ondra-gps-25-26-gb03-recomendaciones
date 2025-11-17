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
import java.util.List;
import java.util.Map;

/**
 * Cliente para comunicación con el microservicio de Contenidos.
 * Maneja todas las peticiones relacionadas con géneros, canciones y álbumes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContenidosClient {

    private final RestTemplate restTemplate;

    @Value("${microservices.contenidos.url}")
    private String contenidosUrl;

    // ============================================
    // MÉTODOS DE GÉNEROS
    // ============================================

    /**
     * Verifica si un género existe en el microservicio de contenidos.
     * Llama a: GET /generos/{id}/existe
     *
     * @param idGenero ID del género a verificar
     * @return true si existe, false en caso contrario
     */
    public boolean existeGenero(Long idGenero) {
        try {
            String url = contenidosUrl + "/generos/" + idGenero + "/existe";
            log.debug("🔍 Verificando existencia de género ID: {} en URL: {}", idGenero, url);

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
     * Obtiene el nombre de un género desde el microservicio de contenidos.
     * Llama a: GET /generos/{id}/nombre
     *
     * @param idGenero ID del género
     * @return Nombre del género o null si no existe
     */
    public String obtenerNombreGenero(Long idGenero) {
        try {
            String url = contenidosUrl + "/generos/" + idGenero + "/nombre";
            log.debug("📋 Obteniendo nombre de género ID: {} en URL: {}", idGenero, url);

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
     * Obtiene información completa de un género (alternativo, usando el endpoint principal).
     * Llama a: GET /generos/{id}
     *
     * @param idGenero ID del género
     * @return Map con información del género o null si no existe
     */
    public Map<String, Object> obtenerGenero(Long idGenero) {
        try {
            String url = contenidosUrl + "/generos/" + idGenero;
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

    // ============================================
    // MÉTODOS DE CANCIONES
    // ============================================

    /**
     * Obtiene canciones por género desde el microservicio de contenidos.
     * Retorna solo información básica (id, titulo, id_genero, nombre_genero).
     *
     * @param idGenero ID del género musical
     * @param limite Número máximo de canciones a obtener
     * @return Lista de canciones recomendadas
     */
    public List<CancionRecomendadaDTO> obtenerCancionesPorGenero(Long idGenero, int limite) {
        try {
            String url = contenidosUrl + "/canciones?id_genero=" + idGenero + "&limite=" + limite;
            log.debug("🎵 Obteniendo canciones del género {} (límite: {})", idGenero, limite);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<CancionRecomendadaDTO> canciones = new ArrayList<>();
            if (response.getBody() != null) {
                for (Map<String, Object> cancionData : response.getBody()) {
                    CancionRecomendadaDTO cancion = CancionRecomendadaDTO.builder()
                            .idCancion(parseLong(cancionData.get("id_cancion")))
                            .titulo((String) cancionData.get("titulo"))
                            .idGenero(parseLong(cancionData.get("id_genero")))
                            .nombreGenero((String) cancionData.get("nombre_genero"))
                            .build();
                    canciones.add(cancion);
                }
                log.debug("✅ Obtenidas {} canciones del género {}", canciones.size(), idGenero);
            }

            return canciones;

        } catch (Exception e) {
            log.error("❌ Error al obtener canciones del género {}: {}", idGenero, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los IDs de canciones que el usuario ya posee o tiene en favoritos.
     *
     * @param idUsuario ID del usuario
     * @return Lista de IDs de canciones
     */
    public List<Long> obtenerCancionesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + "/usuarios/" + idUsuario + "/canciones/ids";
            log.debug("🔍 Obteniendo IDs de canciones del usuario {}", idUsuario);

            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            List<Long> ids = response.getBody() != null ? response.getBody() : new ArrayList<>();
            log.debug("Usuario {} tiene {} canciones", idUsuario, ids.size());

            return ids;

        } catch (Exception e) {
            log.error("❌ Error al obtener canciones del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================
    // MÉTODOS DE ÁLBUMES
    // ============================================

    /**
     * Obtiene álbumes por género desde el microservicio de contenidos.
     * Retorna solo información básica (id, titulo, id_genero, nombre_genero).
     *
     * @param idGenero ID del género musical
     * @param limite Número máximo de álbumes a obtener
     * @return Lista de álbumes recomendados
     */
    public List<AlbumRecomendadoDTO> obtenerAlbumesPorGenero(Long idGenero, int limite) {
        try {
            String url = contenidosUrl + "/albumes?id_genero=" + idGenero + "&limite=" + limite;
            log.debug("💿 Obteniendo álbumes del género {} (límite: {})", idGenero, limite);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<AlbumRecomendadoDTO> albumes = new ArrayList<>();
            if (response.getBody() != null) {
                for (Map<String, Object> albumData : response.getBody()) {
                    AlbumRecomendadoDTO album = AlbumRecomendadoDTO.builder()
                            .idAlbum(parseLong(albumData.get("id_album")))
                            .titulo((String) albumData.get("titulo"))
                            .idGenero(parseLong(albumData.get("id_genero")))
                            .nombreGenero((String) albumData.get("nombre_genero"))
                            .build();
                    albumes.add(album);
                }
                log.debug("✅ Obtenidos {} álbumes del género {}", albumes.size(), idGenero);
            }

            return albumes;

        } catch (Exception e) {
            log.error("❌ Error al obtener álbumes del género {}: {}", idGenero, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los IDs de álbumes que el usuario ya posee o tiene en favoritos.
     *
     * @param idUsuario ID del usuario
     * @return Lista de IDs de álbumes
     */
    public List<Long> obtenerAlbumesUsuario(Long idUsuario) {
        try {
            String url = contenidosUrl + "/usuarios/" + idUsuario + "/albumes/ids";
            log.debug("🔍 Obteniendo IDs de álbumes del usuario {}", idUsuario);

            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );

            List<Long> ids = response.getBody() != null ? response.getBody() : new ArrayList<>();
            log.debug("Usuario {} tiene {} álbumes", idUsuario, ids.size());

            return ids;

        } catch (Exception e) {
            log.error("❌ Error al obtener álbumes del usuario {}: {}", idUsuario, e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================

    /**
     * Convierte un objeto a Long de forma segura.
     * Maneja casos donde el valor puede venir como Integer, Long o String.
     *
     * @param value Valor a convertir
     * @return Long o null si no se puede convertir
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