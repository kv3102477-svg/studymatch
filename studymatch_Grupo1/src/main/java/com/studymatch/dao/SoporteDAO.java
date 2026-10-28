package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de soporte. Sus consultas están en la implementación JDBC. */
public interface SoporteDAO {
    /** Busca un reporte pendiente del mismo estudiante sobre el mismo compañero. */
    List<Map<String, Object>> reportePendiente(long estudianteId, Long reportadoId);

    /** Guarda un registro nuevo y devuelve su identificador. */
    long crear(
        long estudianteId,
        Long reportadoId,
        String tipo,
        String categoria,
        String asunto,
        String mensaje
    );
}
