package com.ondra.recomendaciones.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondra.recomendaciones.dto.AgregarPreferenciasDTO;
import com.ondra.recomendaciones.dto.PreferenciaGeneroDTO;
import com.ondra.recomendaciones.dto.PreferenciasResponseDTO;
import com.ondra.recomendaciones.exceptions.ForbiddenAccessException;
import com.ondra.recomendaciones.exceptions.InvalidGenreException;
import com.ondra.recomendaciones.exceptions.PreferenciaNotFoundException;
import com.ondra.recomendaciones.security.JwtAuthenticationFilter;
import com.ondra.recomendaciones.security.SecurityConfig;
import com.ondra.recomendaciones.security.ServiceTokenFilter;
import com.ondra.recomendaciones.services.PreferenciasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitarios para {@link PreferenciasController}
 */
@WebMvcTest(PreferenciasController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ServiceTokenFilter.class, TestJwtHelper.class})
class PreferenciasControllerTest {

    private static final String API_CONTEXT = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestJwtHelper testJwtHelper;

    @MockBean
    private PreferenciasService preferenciasService;

    private PreferenciaGeneroDTO preferenciaRock;
    private PreferenciasResponseDTO preferenciasResponseDTO;

    @BeforeEach
    void setUp() {
        preferenciaRock = PreferenciaGeneroDTO.builder()
                .idGenero(5L)
                .nombreGenero("Rock")
                .build();

        preferenciasResponseDTO = PreferenciasResponseDTO.builder()
                .mensaje("Preferencias agregadas exitosamente")
                .generosAgregados(2)
                .generosDuplicados(0)
                .preferencias(List.of(preferenciaRock))
                .build();
    }

    // ==================== TESTS OBTENER PREFERENCIAS ====================

    @Test
    @DisplayName("Obtener preferencias de usuario - exito")
    void obtenerPreferencias_Success() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        when(preferenciasService.obtenerPreferencias(1L))
                .thenReturn(List.of(preferenciaRock));

        mockMvc.perform(get("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idGenero").value(5))
                .andExpect(jsonPath("$[0].nombreGenero").value("Rock"));

        verify(preferenciasService, times(1)).obtenerPreferencias(1L);
    }

    // ==================== TESTS AGREGAR PREFERENCIAS ====================

    @Test
    @DisplayName("Agregar preferencias - exito")
    void agregarPreferencias_Success() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        AgregarPreferenciasDTO dto = new AgregarPreferenciasDTO(List.of(5L, 7L));

        doNothing().when(preferenciasService).verificarPropietario(1L, 1L, false);
        when(preferenciasService.agregarPreferencias(eq(1L), any(AgregarPreferenciasDTO.class)))
                .thenReturn(preferenciasResponseDTO);

        mockMvc.perform(post("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Preferencias agregadas exitosamente"))
                .andExpect(jsonPath("$.generosAgregados").value(2))
                .andExpect(jsonPath("$.generosDuplicados").value(0))
                .andExpect(jsonPath("$.preferencias", hasSize(1)));

        verify(preferenciasService, times(1)).verificarPropietario(1L, 1L, false);
        verify(preferenciasService, times(1)).agregarPreferencias(eq(1L), any(AgregarPreferenciasDTO.class));
    }

    @Test
    @DisplayName("Agregar preferencias de otro usuario - Forbidden")
    void agregarPreferencias_OtroUsuario() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(2L, "otro@example.com");

        AgregarPreferenciasDTO dto = new AgregarPreferenciasDTO(List.of(3L));

        doThrow(new ForbiddenAccessException("No puedes modificar otro usuario"))
                .when(preferenciasService).verificarPropietario(2L, 1L, false);

        mockMvc.perform(post("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(preferenciasService, times(1)).verificarPropietario(2L, 1L, false);
        verify(preferenciasService, never()).agregarPreferencias(anyLong(), any(AgregarPreferenciasDTO.class));
    }

    @Test
    @DisplayName("Agregar preferencias con lista vacia - Bad Request")
    void agregarPreferencias_ListaVacia() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        AgregarPreferenciasDTO dto = new AgregarPreferenciasDTO(List.of());

        mockMvc.perform(post("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(preferenciasService, never()).verificarPropietario(anyLong(), anyLong(), anyBoolean());
        verify(preferenciasService, never()).agregarPreferencias(anyLong(), any(AgregarPreferenciasDTO.class));
    }

    @Test
    @DisplayName("Agregar preferencias con genero invalido - Bad Request")
    void agregarPreferencias_GeneroInvalido() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        AgregarPreferenciasDTO dto = new AgregarPreferenciasDTO(List.of(9L));

        doNothing().when(preferenciasService).verificarPropietario(1L, 1L, false);
        when(preferenciasService.agregarPreferencias(eq(1L), any(AgregarPreferenciasDTO.class)))
                .thenThrow(new InvalidGenreException("Genero invalido"));

        mockMvc.perform(post("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(preferenciasService, times(1)).verificarPropietario(1L, 1L, false);
        verify(preferenciasService, times(1)).agregarPreferencias(eq(1L), any(AgregarPreferenciasDTO.class));
    }

    // ==================== TESTS ELIMINAR PREFERENCIAS ====================

    @Test
    @DisplayName("Eliminar todas las preferencias - exito")
    void eliminarTodasPreferencias_Success() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        doNothing().when(preferenciasService).verificarPropietario(1L, 1L, false);
        doNothing().when(preferenciasService).eliminarTodasPreferencias(1L);

        mockMvc.perform(delete("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(preferenciasService, times(1)).verificarPropietario(1L, 1L, false);
        verify(preferenciasService, times(1)).eliminarTodasPreferencias(1L);
    }

    @Test
    @DisplayName("Eliminar todas las preferencias de otro usuario - Forbidden")
    void eliminarTodasPreferencias_OtroUsuario() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(2L, "otro@example.com");

        doThrow(new ForbiddenAccessException("No puedes eliminar preferencias de otro usuario"))
                .when(preferenciasService).verificarPropietario(2L, 1L, false);

        mockMvc.perform(delete("/api/usuarios/1/preferencias")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(preferenciasService, times(1)).verificarPropietario(2L, 1L, false);
        verify(preferenciasService, never()).eliminarTodasPreferencias(anyLong());
    }

    @Test
    @DisplayName("Eliminar preferencia especifica - exito")
    void eliminarPreferencia_Success() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        doNothing().when(preferenciasService).verificarPropietario(1L, 1L, false);
        doNothing().when(preferenciasService).eliminarPreferencia(1L, 5L);

        mockMvc.perform(delete("/api/usuarios/1/preferencias/5")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(preferenciasService, times(1)).verificarPropietario(1L, 1L, false);
        verify(preferenciasService, times(1)).eliminarPreferencia(1L, 5L);
    }

    @Test
    @DisplayName("Eliminar preferencia inexistente - Not Found")
    void eliminarPreferencia_NoExiste() throws Exception {
        String token = testJwtHelper.generarTokenPrueba(1L, "user@example.com");

        doNothing().when(preferenciasService).verificarPropietario(1L, 1L, false);
        doThrow(new PreferenciaNotFoundException("No encontrada"))
                .when(preferenciasService).eliminarPreferencia(1L, 99L);

        mockMvc.perform(delete("/api/usuarios/1/preferencias/99")
                        .contextPath(API_CONTEXT)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        verify(preferenciasService, times(1)).verificarPropietario(1L, 1L, false);
        verify(preferenciasService, times(1)).eliminarPreferencia(1L, 99L);
    }
}
