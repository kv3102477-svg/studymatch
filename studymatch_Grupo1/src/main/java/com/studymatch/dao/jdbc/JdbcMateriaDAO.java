package com.studymatch.dao.jdbc;

import com.studymatch.dao.MateriaDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa MateriaDAO con consultas a PostgreSQL. */
@Repository
public class JdbcMateriaDAO implements MateriaDAO {

    private final SqlExecutor sql;

    public JdbcMateriaDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public Map<String, Object> contarBusqueda(String patron) {
        return sql.one("SELECT COUNT(*) AS total FROM materia WHERE nombre ILIKE ?", patron);
    }

    @Override
    public List<Map<String, Object>> buscarPagina(String patron, int desplazamiento) {
        return sql.list(
            "SELECT id,nombre FROM materia WHERE nombre ILIKE ? ORDER BY nombre,id LIMIT 20 OFFSET ?",
            patron,
            desplazamiento
        );
    }

    @Override
    public List<Map<String, Object>> listarPropias(long estudianteId) {
        return sql.list(
            "SELECT em.*,m.nombre FROM estudiante_materia em JOIN materia m ON m.id=em.materia_id " +
                "WHERE estudiante_id=? ORDER BY m.nombre",
            estudianteId
        );
    }

    @Override
    public int eliminarPropia(long relacionId, long estudianteId) {
        return sql.update(
            "DELETE FROM estudiante_materia WHERE id=? AND estudiante_id=?",
            relacionId,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> listarPublicas(long estudianteId) {
        return sql.list(
            "SELECT em.tipo_rol,em.nivel,m.id AS materia_id,m.nombre FROM estudiante_materia em " +
                "JOIN materia m ON m.id=em.materia_id WHERE estudiante_id=?",
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> listarParaComparacion(Object[] parametros) {
        String placeholders = String.join(
            ",",
            java.util.Collections.nCopies(parametros.length - 1, "?")
        );
        return sql.list(
            "SELECT em.estudiante_id,em.materia_id,em.tipo_rol,m.nombre FROM estudiante_materia " +
                "em JOIN materia m ON m.id=em.materia_id WHERE em.estudiante_id=? OR em.estudiante_id " +
                "IN (" +
                placeholders +
                ")",
            parametros
        );
    }

    @Override
    public int guardarPropia(long estudianteId, long materiaId, String rol, String nivel) {
        return sql.update(
            "INSERT INTO estudiante_materia(estudiante_id,materia_id,tipo_rol,nivel) VALUES (?,?,?,?) " +
                "ON CONFLICT (estudiante_id,materia_id,tipo_rol) DO UPDATE SET nivel=EXCLUDED.nivel",
            estudianteId,
            materiaId,
            rol,
            nivel
        );
    }

    @Override
    public List<Map<String, Object>> coincidencias(long estudianteId, long companeroId) {
        return sql.list(
            "SELECT DISTINCT m.id,m.nombre,CASE WHEN a.tipo_rol='ENSENAR' THEN ? ELSE ? END AS mentor_id " +
                "FROM estudiante_materia a JOIN estudiante_materia b ON a.materia_id=b.materia_id " +
                "JOIN materia m ON m.id=a.materia_id " +
                "WHERE a.estudiante_id=? AND b.estudiante_id=? AND ((a.tipo_rol='ENSENAR' AND " +
                "b.tipo_rol='APRENDER') OR (a.tipo_rol='APRENDER' AND b.tipo_rol='ENSENAR')) ORDER BY " +
                "m.nombre,mentor_id",
            estudianteId,
            companeroId,
            estudianteId,
            companeroId
        );
    }

    @Override
    public List<Map<String, Object>> validarRoles(long mentorId, long aprendizId, long materiaId) {
        return sql.list(
            "SELECT a.id FROM estudiante_materia a JOIN estudiante_materia b ON " +
                "a.materia_id=b.materia_id WHERE a.estudiante_id=? AND b.estudiante_id=? AND " +
                "a.materia_id=? AND a.tipo_rol='ENSENAR' AND b.tipo_rol='APRENDER'",
            mentorId,
            aprendizId,
            materiaId
        );
    }
}
