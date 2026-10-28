package com.studymatch.dao.jdbc;

import com.studymatch.dao.ConversacionDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa ConversacionDAO con consultas a PostgreSQL. */
@Repository
public class JdbcConversacionDAO implements ConversacionDAO {

    private final SqlExecutor sql;

    public JdbcConversacionDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listarDelEstudiante(long estudianteId) {
        return sql.list(
            "WITH pairs AS (SELECT LEAST(estudiante_a,estudiante_b) AS " +
                "a,GREATEST(estudiante_a,estudiante_b) AS b," +
                "COALESCE(MIN(id) FILTER(WHERE sesion_id IS NULL AND emparejamiento_id IS NULL),MIN(id)) AS id," +
                "BOOL_OR((sesion_id IS NULL AND emparejamiento_id IS NULL) OR EXISTS(SELECT 1 FROM " +
                "mensaje WHERE conversacion_id=conversacion.id)) AS visible " +
                "FROM conversacion WHERE estudiante_a=? OR estudiante_b=? GROUP BY " +
                "LEAST(estudiante_a,estudiante_b),GREATEST(estudiante_a,estudiante_b)) " +
                "SELECT p.id,p.a AS estudiante_a,p.b AS estudiante_b,'ACTIVA' AS estado,e.nombre," +
                "(SELECT COUNT(*) FROM mensaje m JOIN conversacion c ON c.id=m.conversacion_id WHERE " +
                "LEAST(c.estudiante_a,c.estudiante_b)=p.a AND " +
                "GREATEST(c.estudiante_a,c.estudiante_b)=p.b AND m.emisor_id<>? AND m.leido=false) AS " +
                "no_leidos " +
                "FROM pairs p JOIN estudiante e ON e.id=CASE WHEN p.a=? THEN p.b ELSE p.a END WHERE " +
                "p.visible ORDER BY p.id DESC",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public Map<String, Object> buscarParticipante(long conversacionId, long estudianteId) {
        return sql.one(
            "SELECT * FROM conversacion WHERE id=? AND (estudiante_a=? OR estudiante_b=?) FOR UPDATE",
            conversacionId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public long crearMensaje(long conversacionId, long emisorId, String texto) {
        return sql.insert(
            "INSERT INTO mensaje(conversacion_id,emisor_id,texto) VALUES (?,?,?)",
            conversacionId,
            emisorId,
            texto
        );
    }

    @Override
    public Map<String, Object> buscarAdjunto(
        long mensajeId,
        long conversacionId,
        long estudianteId
    ) {
        return sql.one(
            "SELECT m.texto FROM mensaje m JOIN conversacion c ON c.id=m.conversacion_id WHERE " +
                "m.id=? AND c.id=? AND (c.estudiante_a=? OR c.estudiante_b=?)",
            mensajeId,
            conversacionId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> buscarDirecta(long estudianteId, long companeroId) {
        return sql.list(
            "SELECT id FROM conversacion WHERE sesion_id IS NULL AND emparejamiento_id IS NULL AND " +
                "((estudiante_a=? AND estudiante_b=?) OR (estudiante_a=? AND estudiante_b=?)) ORDER BY id LIMIT 1",
            estudianteId,
            companeroId,
            companeroId,
            estudianteId
        );
    }

    @Override
    public long crearDirecta(long estudianteId, long companeroId) {
        return sql.insert(
            "INSERT INTO conversacion(estudiante_a,estudiante_b) VALUES (?,?)",
            estudianteId,
            companeroId
        );
    }

    @Override
    public int marcarMensajesLeidos(long primerId, long segundoId, long lectorId) {
        String pair =
            "SELECT id FROM conversacion WHERE (estudiante_a=? AND estudiante_b=?) OR " +
            "(estudiante_a=? AND estudiante_b=?)";
        return sql.update(
            "UPDATE mensaje SET leido=true WHERE conversacion_id IN (" +
                pair +
                ") AND emisor_id<>?",
            primerId,
            segundoId,
            segundoId,
            primerId,
            lectorId
        );
    }

    @Override
    public List<Map<String, Object>> listarMensajes(long primerId, long segundoId) {
        String pair =
            "SELECT id FROM conversacion WHERE (estudiante_a=? AND estudiante_b=?) OR " +
            "(estudiante_a=? AND estudiante_b=?)";
        return sql.list(
            "SELECT m.*,e.nombre AS autor FROM mensaje m JOIN estudiante e ON e.id=m.emisor_id " +
                "WHERE conversacion_id IN (" +
                pair +
                ") ORDER BY m.enviado_en,m.id",
            primerId,
            segundoId,
            segundoId,
            primerId
        );
    }
}
