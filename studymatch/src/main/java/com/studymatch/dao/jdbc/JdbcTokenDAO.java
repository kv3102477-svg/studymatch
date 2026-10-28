package com.studymatch.dao.jdbc;

import com.studymatch.dao.TokenDAO;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa TokenDAO con consultas a PostgreSQL. */
@Repository
public class JdbcTokenDAO implements TokenDAO {

    private final SqlExecutor sql;

    public JdbcTokenDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public int invalidarRecuperacion(long estudianteId) {
        return sql.update(
            "UPDATE token_seguridad SET usado=true WHERE estudiante_id=? AND tipo='RECUPERACION_CLAVE'",
            estudianteId
        );
    }

    @Override
    public int crearRecuperacion(long estudianteId, String codigoHash) {
        return sql.update(
            "INSERT INTO token_seguridad(estudiante_id,codigo,tipo,expira_en) VALUES " +
                "(?,?,'RECUPERACION_CLAVE',CURRENT_TIMESTAMP+INTERVAL '30 minutes')",
            estudianteId,
            codigoHash
        );
    }

    @Override
    public Map<String, Object> buscarRecuperacionVigente(String codigoHash) {
        return sql.one(
            "SELECT t.id,t.estudiante_id FROM token_seguridad t JOIN estudiante e ON " +
                "e.id=t.estudiante_id WHERE t.codigo=? " +
                "AND t.tipo='RECUPERACION_CLAVE' AND t.usado=false AND t.expira_en>CURRENT_TIMESTAMP " +
                "AND e.cuenta_activa=true FOR UPDATE OF t",
            codigoHash
        );
    }
}
