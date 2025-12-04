package com.ondra.recomendaciones.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondra.recomendaciones.dto.AlbumRecomendadoDTO;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitarios para {@link RecomendacionesUsuarioController}
 */
@WebMvcTest(RecomendacionesUsuarioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ServiceTokenFilter.class, TestJwtHelper.class})
class RecomendacionesUsuarioControllerTest {

    private static final String API_CONTEXT = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestJwtHelper testJwtHelper;

    @MockBean
    private RecomendacionesService recomendacionesService;

    private RecomendacionesResponseDTO recomendacionesUsuario;

    @BeforeEach
    void setUp() {
        CancionRecomendadaDTO cancionRock = CancionRecomendadaDTO.builder()
                .idCancion(101L)
                .titulo("Rumbo")
                .idGenero(5L)
                .nombreGenero("Rock")
                .build();

        AlbumRecomendadoDTO albumRock = AlbumRecomendadoDTO.builder()
                .idAlbum(201L)
                .titulo("Sonidos del Camino")
                .idGenero(5L)
                .nombreGenero("Rock")
                .build();

        recomendacionesUsuario = RecomendacionesResponseDTO.builder()
                .idUsuario(1L)
                .totalRecomendaciones(2)
                .canciones(List.of(cancionRock))
                .albumes(List.of(albumRock))
                .build();
    }

    // ==================== TESTS RECOMENDACIONES USUARIO ====================

    @Test
    @DisplayName("Obtener recomendaciones de usuario - exito")
    void obtenerRecomendacionesUsuario_Success() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        when(recomendacionesService.obtenerRecomendaciones(eq(1L), isNull(), eq("ambos"), eq(20)))
                .thenReturn(recomendacionesUsuario);

        mockMvc.perform(get("/api/usuarios/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.totalRecomendaciones").value(2))
                .andExpect(jsonPath("$.canciones", hasSize(1)))
                .andExpect(jsonPath("$.albumes", hasSize(1)))
                .andExpect(jsonPath("$.canciones[0].titulo").value("Rumbo"));

        verify(recomendacionesService, times(1))
                .obtenerRecomendaciones(1L, null, "ambos", 20);
    }

    @Test
    @DisplayName("Obtener recomendaciones sin token - Forbidden")
    void obtenerRecomendacionesUsuario_SinToken() throws Exception {
        mockMvc.perform(get("/api/usuarios/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .with(user("anon")))
                .andExpect(status().isForbidden());

        verify(recomendacionesService, never())
                .obtenerRecomendaciones(any(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Obtener recomendaciones con tipo invalido - Bad Request")
    void obtenerRecomendacionesUsuario_TipoInvalido() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        when(recomendacionesService.obtenerRecomendaciones(eq(1L), isNull(), eq("video"), eq(10)))
                .thenThrow(new InvalidParameterException("Tipo no valido"));

        mockMvc.perform(get("/api/usuarios/recomendaciones")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token)
                        .param("tipo", "video")
                        .param("limite", "10"))
                .andExpect(status().isBadRequest());

        verify(recomendacionesService, times(1))
                .obtenerRecomendaciones(1L, null, "video", 10);
    }
}
