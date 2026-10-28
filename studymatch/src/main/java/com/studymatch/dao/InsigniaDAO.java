package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de insignia. Sus consultas están en la implementación JDBC. */
public interface InsigniaDAO {
    /** Consulta las insignias y las horas usadas para calcular su progreso. */
    List<Map<String, Object>> listarConProgreso(long estudianteId);

    /** Registra las insignias alcanzadas sin duplicar las ya obtenidas. */
    int otorgarAlcanzadas(long estudianteId);

    /** Actualiza las estadísticas del estudiante para el ciclo activo. */
    int actualizarEstadisticas(long estudianteId);

    /** Consulta los mentores ordenados por sus horas enseñadas. */
    List<Map<String, Object>> ranking();
}
