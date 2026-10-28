package com.studymatch.dao.jdbc;

import com.studymatch.dao.CalificacionDAO;
import org.springframework.stereotype.Repository;

/** Implementa CalificacionDAO con consultas a PostgreSQL. */
@Repository
public class JdbcCalificacionDAO implements CalificacionDAO {

    private final SqlExecutor sql;

    public JdbcCalificacionDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public long guardar(long sesionId, long estudianteId, int estrellas) {
        return sql.insert(
            "INSERT INTO calificacion(sesion_id,calificador_id,estrellas) VALUES (?,?,?) " +
                "ON CONFLICT(sesion_id,calificador_id) DO UPDATE SET " +
                "estrellas=EXCLUDED.estrellas,creada_en=CURRENT_TIMESTAMP",
            sesionId,
            estudianteId,
            estrellas
        );
    }

    @Override
    public int eliminarComentario(long calificacionId) {
        return sql.update("DELETE FROM comentario WHERE calificacion_id=?", calificacionId);
    }

    @Override
    public int guardarComentario(long calificacionId, String comentario) {
        return sql.update(
            "INSERT INTO comentario(calificacion_id,texto,estado) VALUES (?,?,'PENDIENTE_MODERACION')",
            calificacionId,
            comentario
        );
    }

    @Override
    public int marcarSesion(long sesionId) {
        return sql.update(
            "UPDATE sesion SET estado_calificacion='CALIFICADA' WHERE id=?",
            sesionId
        );
    }

    @Override
    public int expirarPlazo() {
        return sql.update(
            "UPDATE sesion SET estado_calificacion='EXPIRADA' WHERE estado='COMPLETADA' AND " +
                "fin_real<CURRENT_TIMESTAMP-INTERVAL '7 days' AND estado_calificacion<>'EXPIRADA'"
        );
    }
}
