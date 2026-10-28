package com.studymatch.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de sesion. Sus consultas están en la implementación JDBC. */
public interface SesionDAO {
    /** Consulta los registros asociados al estudiante. */
    List<Map<String, Object>> listarDelEstudiante(long estudianteId);

    /** Comprueba que el esquema de sesiones tenga los campos de roles. */
    boolean tieneRoles();

    /** Suma las horas de las sesiones completadas, separadas por rol. */
    Map<String, Object> horasAcumuladas(long estudianteId);

    /** Busca registros que se superponen con el intervalo indicado. */
    List<Map<String, Object>> buscarCruces(
        long estudianteId,
        LocalDateTime fin,
        LocalDateTime inicio
    );

    /** Guarda una propuesta con sus participantes y roles. */
    long crearPropuesta(
        long proponenteId,
        long receptorId,
        long materiaId,
        LocalDateTime fecha,
        int duracionMinutos,
        long mentorId,
        long aprendizId
    );

    /** Busca el registro solo si el estudiante pertenece a él. */
    Map<String, Object> buscarParticipante(long sesionId, long estudianteId);

    /** Cambia la sesión al estado confirmado. */
    int confirmar(long sesionId);

    /** Cancela el registro indicado dentro de los límites de su propietario. */
    int cancelar(long sesionId);

    /** Guarda la hora real de inicio de la sesión. */
    int iniciar(LocalDateTime inicio, long sesionId);

    /** Registra la marca de finalización del participante indicado. */
    int marcarFinalizacion(String columna, LocalDateTime fin, long sesionId);

    /** Guarda la duración real y marca la sesión como completada. */
    int completar(long duracionMinutos, long sesionId);

    /** Actualiza el enlace de reunión proporcionado por el usuario. */
    int guardarEnlace(String enlace, long sesionId);
}
