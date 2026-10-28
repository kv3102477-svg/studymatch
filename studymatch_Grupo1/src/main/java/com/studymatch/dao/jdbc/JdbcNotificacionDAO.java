package com.studymatch.dao.jdbc;

import com.studymatch.dao.NotificacionDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa NotificacionDAO con consultas a PostgreSQL. */
@Repository
public class JdbcNotificacionDAO implements NotificacionDAO {

    private final SqlExecutor sql;

    public JdbcNotificacionDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listar(long estudianteId) {
        return sql.list(
            "SELECT * FROM notificacion WHERE receptor_id=? ORDER BY creada_en DESC LIMIT 100",
            estudianteId
        );
    }

    @Override
    public int marcarLeida(long notificacionId, long estudianteId) {
        return sql.update(
            "UPDATE notificacion SET leida=true WHERE id=? AND receptor_id=?",
            notificacionId,
            estudianteId
        );
    }

    @Override
    public void crear(
        long estudianteId,
        String titulo,
        String mensaje,
        String evento,
        long referenciaId
    ) {
        sql.update(
            "INSERT INTO " +
                "notificacion(receptor_id,titulo,mensaje,tipo_evento,referencia_id,tipo,canal,estado_envio) " +
                "VALUES (?,?,?,?,?,'IN_APP','IN_APP','ENVIADO')",
            estudianteId,
            titulo,
            mensaje,
            evento,
            referenciaId
        );
    }

    @Override
    public Map<String, Object> bloquearRecordatorios() {
        return sql.one("SELECT pg_try_advisory_xact_lock(72813491) AS acquired");
    }

    @Override
    public int recordarSesiones() {
        return sql.update(
            "INSERT INTO " +
                "notificacion(receptor_id,titulo,mensaje,tipo_evento,referencia_id,tipo,canal,estado_envio) " +
                "SELECT who.id,'Recordatorio de sesiÃ³n','Tienes una sesiÃ³n confirmada en las " +
                "prÃ³ximas 24 horas.','RECORDATORIO',s.id,'IN_APP','IN_APP','ENVIADO' " +
                "FROM sesion s CROSS JOIN LATERAL (VALUES(s.proponente_id),(s.receptor_id)) who(id) " +
                "JOIN estudiante e ON e.id=who.id AND e.cuenta_activa=true " +
                "WHERE s.estado='CONFIRMADA' AND s.fecha_hora>CURRENT_TIMESTAMP AND " +
                "s.fecha_hora<=CURRENT_TIMESTAMP+INTERVAL '24 hours' " +
                "AND NOT EXISTS(SELECT 1 FROM notificacion n WHERE n.receptor_id=who.id AND " +
                "n.referencia_id=s.id AND n.tipo_evento='RECORDATORIO')"
        );
    }

    @Override
    public int recordarVencimientos() {
        return sql.update(
            "INSERT INTO " +
                "notificacion(receptor_id,titulo,mensaje,tipo_evento,referencia_id,tipo,canal,estado_envio,dias_aviso) " +
                "SELECT su.estudiante_id,'Vencimiento de suscripciÃ³n','Tu suscripciÃ³n vence en " +
                "'||(su.fecha_vencimiento-CURRENT_DATE)||' " +
                "dÃ­as.','SISTEMA',su.id,'IN_APP','IN_APP','ENVIADO',su.fecha_vencimiento-CURRENT_DATE " +
                "FROM suscripcion su JOIN estudiante e ON e.id=su.estudiante_id AND e.cuenta_activa=true " +
                "WHERE su.estado IN ('TRIAL','ACTIVO') AND su.fecha_vencimiento-CURRENT_DATE IN (15,7,3,1,0) " +
                "AND NOT EXISTS(SELECT 1 FROM suscripcion newer WHERE " +
                "newer.estudiante_id=su.estudiante_id AND newer.id>su.id AND newer.estado IN " +
                "('TRIAL','ACTIVO') AND newer.fecha_vencimiento>=CURRENT_DATE) " +
                "AND NOT EXISTS(SELECT 1 FROM notificacion n WHERE n.receptor_id=su.estudiante_id AND " +
                "n.referencia_id=su.id AND n.titulo='Vencimiento de suscripciÃ³n' AND " +
                "n.dias_aviso=su.fecha_vencimiento-CURRENT_DATE)"
        );
    }
}
