package com.ondra.recomendaciones.repositories;

import com.ondra.recomendaciones.models.dao.PreferenciaGenero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de preferencias de géneros musicales.
 *
 * <p>Proporciona operaciones CRUD y consultas personalizadas para
 * las relaciones usuario-género.
 */
@Repository
public interface PreferenciaGeneroRepository extends JpaRepository<PreferenciaGenero, Long> {

    /**
     * Busca todas las preferencias de un usuario.
     *
     * @param idUsuario ID del usuario
     * @return Lista de preferencias del usuario
     */
    List<PreferenciaGenero> findByIdUsuario(Long idUsuario);

    /**
     * Busca una preferencia específica por usuario y género.
     *
     * @param idUsuario ID del usuario
     * @param idGenero ID del género
     * @return Optional con la preferencia si existe
     */
    Optional<PreferenciaGenero> findByIdUsuarioAndIdGenero(Long idUsuario, Long idGenero);

    /**
     * Verifica si existe una preferencia específica.
     *
     * @param idUsuario ID del usuario
     * @param idGenero ID del género
     * @return true si existe la preferencia, false en caso contrario
     */
    boolean existsByIdUsuarioAndIdGenero(Long idUsuario, Long idGenero);

    /**
     * Elimina todas las preferencias de un usuario.
     *
     * @param idUsuario ID del usuario
     */
    void deleteByIdUsuario(Long idUsuario);

    /**
     * Obtiene solo los IDs de géneros preferidos por un usuario.
     *
     * <p>Consulta optimizada que retorna únicamente los IDs de géneros
     * sin cargar las entidades completas.
     *
     * @param idUsuario ID del usuario
     * @return Lista de IDs de géneros preferidos
     */
    @Query("SELECT p.idGenero FROM PreferenciaGenero p WHERE p.idUsuario = :idUsuario")
    List<Long> findGeneroIdsByIdUsuario(@Param("idUsuario") Long idUsuario);
}