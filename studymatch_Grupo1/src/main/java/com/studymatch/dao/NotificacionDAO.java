package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de notificacion. Sus consultas están en la implementación JDBC. */
public interface NotificacionDAO {
    /** Consulta los registros correspondientes al estudiante. */
    List<Map<String, Object>> listar(long estudianteId);

    /** Marca un aviso como leído solo si pertenece al estudiante. */
    int marcarLeida(long notificacionId, long estudianteId);

    /** Guarda un registro nuevo y devuelve su identificador. */
    void crear(long estudianteId, String titulo, String mensaje, String evento, long referenciaId);

    /** Intenta adquirir el bloqueo que evita recordatorios simultáneos. */
    Map<String, Object> bloquearRecordatorios();

    /** Genera avisos de próximas sesiones sin repetir los existentes. */
    int recordarSesiones();

    /** Genera avisos de vencimiento de suscripciones sin duplicarlos. */
    int recordarVencimientos();
}
