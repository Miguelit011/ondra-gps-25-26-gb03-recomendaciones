package com.ondra.recomendaciones.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondra.recomendaciones.dto.CancionRecomendadaDTO;
import com.ondra.recomendaciones.dto.RecomendacionesResponseDTO;
import com.ondra.recomendaciones.exceptions.InvalidParameterException;
import com.ondra.recomendaciones.security.JwtAuthenticationFilter;
import com.ondra.recomendaciones.security.SecurityConfig;
import com.ondra.recomendaciones.security.ServiceTokenFilter;
import com.ondra.recomendaciones.services.RecomendacionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitarios para {@link RecomendacionesArtistaController}
 */
@WebMvcTest(RecomendacionesArtistaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ServiceTokenFilter.class, TestJwtHelper.class})
class RecomendacionesArtistaControllerTest {

    private static final String API_CONTEXT = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestJwtHelper testJwtHelper;

    @MockBean
    private RecomendacionesService recomendacionesService;

    private RecomendacionesResponseDTO recomendacionesArtista;

    @BeforeEach
    void setUp() {
        CancionRecomendadaDTO cancionRock = CancionRecomendadaDTO.builder()
                .idCancion(101L)
                .titulo("Rumbo")
                .idGenero(5L)
                .nombreGenero("Rock")
                .build();

        recomendacionesArtista = RecomendacionesResponseDTO.builder()
                .idUsuario(2L)
                .totalRecomendaciones(1)
                .canciones(List.of(cancionRock))
                .albumes(List.of())
                .build();
    }

    // ==================== TESTS RECOMENDACIONES ARTISTA ====================

    @Test
    @DisplayName("Obtener recomendaciones de artista - exito")
    void obtenerRecomendacionesArtista_Success() throws Exception {
        String token = testJwtHelper.generarTokenPruebaArtista(2L, 10L, "artista@example.com");

        when(recomendacionesService.obtenerRecomendaciones(2L, 10L, "cancion", 5))
                .thenReturn(recomendacionesArtista);

        mockMvc.perform(get("/api/artistas/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token)
                        .param("tipo", "cancion")
                        .param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2))
                .andExpect(jsonPath("$.canciones", hasSize(1)))
                .andExpect(jsonPath("$.albumes", hasSize(0)));

        verify(recomendacionesService, times(1))
                .obtenerRecomendaciones(2L, 10L, "cancion", 5);
    }

    @Test
    @DisplayName("Obtener recomendaciones de artista sin artistId - Forbidden")
    void obtenerRecomendacionesArtista_SinArtistId() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(3L, "otro@example.com");

        mockMvc.perform(get("/api/artistas/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(recomendacionesService, never())
                .obtenerRecomendaciones(anyLong(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Obtener recomendaciones de artista con limite invalido - Bad Request")
    void obtenerRecomendacionesArtista_LimiteInvalido() throws Exception {
        String token = testJwtHelper.generarTokenPruebaArtista(2L, 10L, "artista@example.com");

        when(recomendacionesService.obtenerRecomendaciones(2L, 10L, "album", 0))
                .thenThrow(new InvalidParameterException("Limite invalido"));

        mockMvc.perform(get("/api/artistas/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token)
                        .param("tipo", "album")
                        .param("limite", "0"))
                .andExpect(status().isBadRequest());

        verify(recomendacionesService, times(1))
                .obtenerRecomendaciones(2L, 10L, "album", 0);
    }
}
