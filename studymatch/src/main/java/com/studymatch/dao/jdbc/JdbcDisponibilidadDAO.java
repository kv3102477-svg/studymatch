package com.studymatch.dao.jdbc;

import com.studymatch.dao.DisponibilidadDAO;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa DisponibilidadDAO con consultas a PostgreSQL. */
@Repository
public class JdbcDisponibilidadDAO implements DisponibilidadDAO {

    private final SqlExecutor sql;

    public JdbcDisponibilidadDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listar(long estudianteId) {
        return sql.list(
            "SELECT * FROM disponibilidad_horaria WHERE estudiante_id=? ORDER BY dia_semana,hora_inicio",
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> listarPublica(long estudianteId) {
        return sql.list(
            "SELECT dia_semana,hora_inicio,hora_fin FROM disponibilidad_horaria WHERE " +
                "estudiante_id=? ORDER BY dia_semana,hora_inicio",
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> buscarCruces(
        long estudianteId,
        int dia,
        LocalTime fin,
        LocalTime inicio
    ) {
        return sql.list(
            "SELECT id FROM disponibilidad_horaria WHERE estudiante_id=? AND dia_semana=? AND " +
                "hora_inicio<? AND hora_fin>?",
            estudianteId,
            dia,
            fin,
            inicio
        );
    }

    @Override
    public int crear(long estudianteId, int dia, LocalTime inicio, LocalTime fin) {
        return sql.update(
            "INSERT INTO disponibilidad_horaria(estudiante_id,dia_semana,hora_inicio,hora_fin) VALUES (?,?,?,?)",
            estudianteId,
            dia,
            inicio,
            fin
        );
    }

    @Override
    public int eliminarDelEstudiante(long estudianteId) {
        return sql.update("DELETE FROM disponibilidad_horaria WHERE estudiante_id=?", estudianteId);
    }

    @Override
    public void guardarBloques(List<Object[]> bloques) {
        sql.batch(
            "INSERT INTO disponibilidad_horaria(estudiante_id,dia_semana,hora_inicio,hora_fin) VALUES (?,?,?,?)",
            bloques
        );
    }

    @Override
    public List<Map<String, Object>> bloquesDelDia(long estudianteId, int dia) {
        return sql.list(
            "SELECT hora_inicio,hora_fin FROM disponibilidad_horaria WHERE estudiante_id=? AND " +
                "dia_semana=? ORDER BY hora_inicio",
            estudianteId,
            dia
        );
    }
}
