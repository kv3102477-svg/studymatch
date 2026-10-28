package com.studymatch.dao.jdbc;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Ejecuta consultas parametrizadas. Los DAO definen qué datos consultar. */
@Repository
public class SqlExecutor {

    private final JdbcTemplate jdbc;

    public SqlExecutor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String sql, Object... values) {
        return jdbc.queryForList(sql, values);
    }

    public Map<String, Object> one(String sql, Object... values) {
        var rows = list(sql, values);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Registro no encontrado.");
        return rows.get(0);
    }

    public int update(String sql, Object... values) {
        return jdbc.update(sql, values);
    }

    public void batch(String sql, List<Object[]> rows) {
        jdbc.batchUpdate(sql, rows);
    }

    public long insert(String sql, Object... values) {
        return ((Number) one(sql + " RETURNING id", values).get("id")).longValue();
    }
}
