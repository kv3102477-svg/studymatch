package com.studymatch.dao.jdbc;

import com.studymatch.dao.SesionDAO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa SesionDAO con consultas a PostgreSQL. */
@Repository
public class JdbcSesionDAO implements SesionDAO {

    private final SqlExecutor sql;
    private volatile boolean rolesPresentes;

    public JdbcSesionDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listarDelEstudiante(long estudianteId) {
        return sql.list(
            "SELECT s.*, m.nombre AS materia, CASE WHEN s.proponente_id=? THEN r.nombre ELSE " +
                "p.nombre END AS companero, " +
                "c.estrellas, co.texto AS comentario FROM sesion s JOIN materia m ON m.id=s.materia_id " +
                "JOIN estudiante p ON p.id=s.proponente_id JOIN estudiante r ON r.id=s.receptor_id " +
                "LEFT JOIN calificacion c ON c.sesion_id=s.id AND c.calificador_id=? " +
                "LEFT JOIN comentario co ON co.calificacion_id=c.id " +
                "WHERE s.proponente_id=? OR s.receptor_id=? ORDER BY s.fecha_hora DESC",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public boolean tieneRoles() {
        if (!rolesPresentes) rolesPresentes = !sql
            .list(
                "SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND " +
                    "table_name='sesion' AND column_name='mentor_id'"
            )
            .isEmpty();
        return rolesPresentes;
    }

    @Override
    public Map<String, Object> horasAcumuladas(long estudianteId) {
        return sql.one(
            "SELECT COALESCE(SUM(CASE WHEN mentor_id=? THEN duracion_real_min ELSE 0 END),0)/60.0 " +
                "AS horas_ensenadas, " +
                "COALESCE(SUM(CASE WHEN aprendiz_id=? THEN duracion_real_min ELSE 0 END),0)/60.0 AS horas_aprendidas, " +
                "COUNT(*) AS sesiones_completadas FROM sesion WHERE estado='COMPLETADA' AND " +
                "(mentor_id=? OR aprendiz_id=?)",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> buscarCruces(
        long estudianteId,
        LocalDateTime fin,
        LocalDateTime inicio
    ) {
        return sql.list(
            "SELECT id FROM sesion WHERE estado IN ('PROPUESTA','CONFIRMADA','EN_CURSO') AND " +
                "(proponente_id=? OR receptor_id=?) AND fecha_hora<? AND fecha_hora + " +
                "duracion_programada_min * INTERVAL '1 minute'>?",
            estudianteId,
            estudianteId,
            fin,
            inicio
        );
    }

    @Override
    public long crearPropuesta(
        long proponenteId,
        long receptorId,
        long materiaId,
        LocalDateTime fecha,
        int duracionMinutos,
        long mentorId,
        long aprendizId
    ) {
        return sql.insert(
            "INSERT INTO " +
                "sesion(proponente_id,receptor_id,materia_id,fecha_hora,duracion_programada_min,estado,mentor_id,aprendiz_id,ciclo_id) " +
                "VALUES (?,?,?,?,?,'PROPUESTA',?,?,(SELECT id FROM ciclo_academico WHERE activo=true " +
                "ORDER BY id DESC LIMIT 1))",
            proponenteId,
            receptorId,
            materiaId,
            fecha,
            duracionMinutos,
            mentorId,
            aprendizId
        );
    }

    @Override
    public Map<String, Object> buscarParticipante(long sesionId, long estudianteId) {
        return sql.one(
            "SELECT s.*, m.nombre AS materia, p.nombre AS proponente, r.nombre AS receptor " +
                "FROM sesion s JOIN materia m ON m.id=s.materia_id JOIN estudiante p ON p.id=s.proponente_id " +
                "JOIN estudiante r ON r.id=s.receptor_id WHERE s.id=? AND (s.proponente_id=? OR " +
                "s.receptor_id=?) FOR UPDATE OF s",
            sesionId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public int confirmar(long sesionId) {
        return sql.update("UPDATE sesion SET estado='CONFIRMADA' WHERE id=?", sesionId);
    }

    @Override
    public int cancelar(long sesionId) {
        return sql.update("UPDATE sesion SET estado='CANCELADA' WHERE id=?", sesionId);
    }

    @Override
    public int iniciar(LocalDateTime inicio, long sesionId) {
        return sql.update(
            "UPDATE sesion SET estado='EN_CURSO',inicio_real=? WHERE id=?",
            inicio,
            sesionId
        );
    }

    @Override
    public int marcarFinalizacion(String columna, LocalDateTime fin, long sesionId) {
        if (
            !java.util.Set.of("marca_proponente", "marca_receptor").contains(columna)
        ) throw new IllegalArgumentException("Marca de asistencia no válida.");
        return sql.update(
            "UPDATE sesion SET " + columna + "=true,fin_real=COALESCE(fin_real,?) WHERE id=?",
            fin,
            sesionId
        );
    }

    @Override
    public int completar(long duracionMinutos, long sesionId) {
        return sql.update(
            "UPDATE sesion SET estado='COMPLETADA',duracion_real_min=?,origen_duracion='REAL' WHERE id=?",
            duracionMinutos,
            sesionId
        );
    }

    @Override
    public int guardarEnlace(String enlace, long sesionId) {
        return sql.update("UPDATE sesion SET enlace_videollamada=? WHERE id=?", enlace, sesionId);
    }
}
