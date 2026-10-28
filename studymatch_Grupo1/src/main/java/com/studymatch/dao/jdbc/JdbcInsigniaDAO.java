package com.studymatch.dao.jdbc;

import com.studymatch.dao.InsigniaDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa InsigniaDAO con consultas a PostgreSQL. */
@Repository
public class JdbcInsigniaDAO implements InsigniaDAO {

    private final SqlExecutor sql;

    public JdbcInsigniaDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listarConProgreso(long estudianteId) {
        return sql.list(
            "SELECT i.*, ei.obtenida_en, (SELECT COALESCE(SUM(duracion_real_min),0)/60.0 FROM " +
                "sesion WHERE mentor_id=? AND estado='COMPLETADA') AS horas_actuales " +
                "FROM insignia i LEFT JOIN estudiante_insignia ei ON ei.insignia_id=i.id AND " +
                "ei.estudiante_id=? ORDER BY i.meta_horas",
            estudianteId,
            estudianteId
        );
    }

    @Override
    public int otorgarAlcanzadas(long estudianteId) {
        return sql.update(
            "INSERT INTO estudiante_insignia(estudiante_id,insignia_id) SELECT ?,i.id FROM " +
                "insignia i WHERE i.meta_horas<=" +
                "(SELECT COALESCE(SUM(duracion_real_min),0)/60.0 FROM sesion WHERE mentor_id=? AND " +
                "estado='COMPLETADA') ON CONFLICT DO NOTHING",
            estudianteId,
            estudianteId
        );
    }

    @Override
    public int actualizarEstadisticas(long estudianteId) {
        return sql.update(
            "INSERT INTO " +
                "estadistica_horas(estudiante_id,ciclo_id,horas_ensenadas,horas_aprendidas,sesiones_completadas,calificacion_promedio) " +
                "SELECT ?,s.ciclo_id,COALESCE(SUM(CASE WHEN s.mentor_id=? THEN s.duracion_real_min " +
                "ELSE 0 END),0)/60.0, " +
                "COALESCE(SUM(CASE WHEN s.aprendiz_id=? THEN s.duracion_real_min ELSE 0 END),0)/60.0,COUNT(*), " +
                "(SELECT AVG(c.estrellas) FROM calificacion c JOIN sesion cs ON cs.id=c.sesion_id " +
                "WHERE cs.ciclo_id=s.ciclo_id AND " +
                "((cs.proponente_id=? AND c.calificador_id=cs.receptor_id) OR (cs.receptor_id=? AND c.calificador_id=cs.proponente_id))) " +
                "FROM sesion s WHERE s.estado='COMPLETADA' AND s.ciclo_id IS NOT NULL AND " +
                "(s.mentor_id=? OR s.aprendiz_id=?) GROUP BY s.ciclo_id " +
                "ON CONFLICT(estudiante_id,ciclo_id) DO UPDATE SET " +
                "horas_ensenadas=EXCLUDED.horas_ensenadas,horas_aprendidas=EXCLUDED.horas_aprendidas, " +
                "sesiones_completadas=EXCLUDED.sesiones_completadas,calificacion_promedio=EXCLUDED.calificacion_promedio",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> ranking() {
        return sql.list(
            "SELECT e.id,e.nombre,e.carrera,SUM(s.duracion_real_min)/60.0 AS " +
                "horas_ensenadas,COUNT(*) AS sesiones_completadas, " +
                "(SELECT i.nombre FROM estudiante_insignia ei JOIN insignia i ON i.id=ei.insignia_id " +
                "WHERE ei.estudiante_id=e.id ORDER BY i.meta_horas DESC LIMIT 1) AS insignia " +
                "FROM sesion s JOIN estudiante e ON e.id=s.mentor_id JOIN ciclo_academico ca ON " +
                "ca.id=s.ciclo_id AND ca.activo=true " +
                "WHERE s.estado='COMPLETADA' AND e.cuenta_activa=true GROUP BY e.id HAVING " +
                "SUM(s.duracion_real_min)>0 " +
                "ORDER BY horas_ensenadas DESC,sesiones_completadas DESC,e.id LIMIT 100"
        );
    }
}
