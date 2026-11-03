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
                "VALUES (?,?,?,?,?,'IN_APP','AMBOS','PENDIENTE_REINTENTO')",
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
                "prÃ³ximas 24 horas.','RECORDATORIO',s.id,'IN_APP','AMBOS','PENDIENTE_REINTENTO' " +
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
                "dÃ­as.','SISTEMA',su.id,'IN_APP','AMBOS','PENDIENTE_REINTENTO',su.fecha_vencimiento-CURRENT_DATE " +
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

    @Override
    public int encolar(long estudianteId,String titulo,String mensaje,String evento,long referenciaId) {
        return sql.update("INSERT INTO notificacion(receptor_id,titulo,mensaje,tipo_evento,referencia_id,tipo,canal,estado_envio) "
            + "VALUES (?,?,?,?,?,'IN_APP','AMBOS','PENDIENTE_REINTENTO')",estudianteId,titulo,mensaje,evento,referenciaId);
    }

    @Override public java.util.List<java.util.Map<String,Object>> pendientes(){
        return sql.list("SELECT * FROM notificacion WHERE estado_envio='PENDIENTE_REINTENTO' AND intentos<3 "
            + "AND (proximo_intento_en IS NULL OR proximo_intento_en<=CURRENT_TIMESTAMP) ORDER BY creada_en,id LIMIT 50 FOR UPDATE SKIP LOCKED");
    }
    @Override public int marcarEnviado(long id){
        return sql.update("UPDATE notificacion SET estado_envio='ENVIADO',ultimo_error=NULL,proximo_intento_en=NULL WHERE id=?",id);
    }
    @Override public int registrarFallo(long id,String error,java.time.LocalDateTime proximoIntento){
        return sql.update("UPDATE notificacion SET intentos=intentos+1,ultimo_error=?,proximo_intento_en=?, "
            + "estado_envio=CASE WHEN intentos+1>=3 THEN 'FALLIDO' ELSE 'PENDIENTE_REINTENTO' END WHERE id=?",error,proximoIntento,id);
    }
}
