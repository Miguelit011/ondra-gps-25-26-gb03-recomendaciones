package com.ondra.recomendaciones.models.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa la preferencia de un usuario por un género musical.
 *
 * <p>Almacena únicamente el ID del género, no la entidad completa, para evitar
 * dependencias directas con el microservicio de géneros.</p>
 */
@Entity
@Table(name = "preferencias_generos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_genero"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaGenero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    private Long idPreferencia;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_genero", nullable = false)
    private Long idGenero;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;

    /**
     * Establece la fecha de agregado antes de persistir la entidad.
     */
    @PrePersist
    protected void onCreate() {
        fechaAgregado = LocalDateTime.now();
    }
}