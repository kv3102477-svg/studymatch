package com.studymatch.dao.jdbc;

import com.studymatch.dao.SoporteDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa SoporteDAO con consultas a PostgreSQL. */
@Repository
public class JdbcSoporteDAO implements SoporteDAO {

    private final SqlExecutor sql;

    public JdbcSoporteDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> reportePendiente(long estudianteId, Long reportadoId) {
        return sql.list(
            "SELECT id FROM solicitud_soporte WHERE estudiante_id=? AND reportado_id=? AND " +
                "tipo='REPORTE' AND estado='PENDIENTE' ORDER BY id LIMIT 1",
            estudianteId,
            reportadoId
        );
    }

    @Override
    public long crear(
        long estudianteId,
        Long reportadoId,
        String tipo,
        String categoria,
        String asunto,
        String mensaje
    ) {
        return sql.insert(
            "INSERT INTO " +
                "solicitud_soporte(estudiante_id,reportado_id,tipo,categoria,asunto,mensaje) VALUES " +
                "(?,?,?,?,?,?)",
            estudianteId,
            reportadoId,
            tipo,
            categoria,
            asunto,
            mensaje
        );
    }

    @Override public int contarReportes(long reportadoId){return ((Number)sql.one("SELECT COUNT(*) AS cantidad FROM solicitud_soporte WHERE tipo='REPORTE' AND reportado_id=?",reportadoId).get("cantidad")).intValue();}
    @Override public int priorizar(long solicitudId){return sql.update("UPDATE solicitud_soporte SET prioridad='ALTA' WHERE id=?",solicitudId);}
}
