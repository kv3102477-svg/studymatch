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

    /** Conserva un aviso pendiente cuando no se pudo entregar al proveedor. */
    int encolar(long estudianteId,String titulo,String mensaje,String evento,long referenciaId);

    /** Devuelve avisos cuyo reintento ya puede procesarse. */
    java.util.List<java.util.Map<String,Object>> pendientes();
    int marcarEnviado(long id);
    int registrarFallo(long id,String error,java.time.LocalDateTime proximoIntento);
}
