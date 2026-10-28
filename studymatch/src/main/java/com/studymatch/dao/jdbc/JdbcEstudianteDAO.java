package com.studymatch.dao.jdbc;

import com.studymatch.dao.EstudianteDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa EstudianteDAO con consultas a PostgreSQL. */
@Repository
public class JdbcEstudianteDAO implements EstudianteDAO {

    private final SqlExecutor sql;

    public JdbcEstudianteDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> validarSesion(long estudianteId) {
        return sql.list(
            "SELECT contrasena_hash,correo_verificado FROM estudiante WHERE id=? AND cuenta_activa=true",
            estudianteId
        );
    }

    @Override
    public Map<String, Object> perfil(long estudianteId) {
        return sql.one(
            "SELECT id, correo, nombre, carrera, ciclo, bio, foto_url, correo_verificado, " +
                "push_activo, creado_en " +
                "FROM estudiante WHERE id=? AND cuenta_activa=true",
            estudianteId
        );
    }

    @Override
    public int desactivar(long estudianteId) {
        return sql.update("UPDATE estudiante SET cuenta_activa=false WHERE id=?", estudianteId);
    }

    @Override
    public Map<String, Object> resumen(long estudianteId) {
        return sql.one(
            "SELECT (SELECT AVG(c.estrellas) FROM calificacion c JOIN sesion se ON se.id=c.sesion_id " +
                "WHERE (se.proponente_id=? AND c.calificador_id=se.receptor_id) OR (se.receptor_id=? " +
                "AND c.calificador_id=se.proponente_id)) AS calificacion_promedio, " +
                "(SELECT COUNT(DISTINCT CASE WHEN estudiante_a=? THEN estudiante_b ELSE estudiante_a " +
                "END) FROM conversacion WHERE estudiante_a=? OR estudiante_b=?) AS companeros",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId
        );
    }

    @Override
    public Map<String, Object> perfilPublico(long estudianteId) {
        return sql.one(
            "SELECT id,nombre,carrera,ciclo,bio,correo_verificado FROM estudiante WHERE id=? AND " +
                "cuenta_activa=true",
            estudianteId
        );
    }

    @Override
    public Map<String, Object> claveGuardada(long estudianteId) {
        return sql.one("SELECT contrasena_hash FROM estudiante WHERE id=?", estudianteId);
    }

    @Override
    public Map<String, Object> bloquearFoto(long estudianteId) {
        return sql.one("SELECT foto_url FROM estudiante WHERE id=? FOR UPDATE", estudianteId);
    }

    @Override
    public int cambiarFoto(String ubicacion, long estudianteId) {
        return sql.update("UPDATE estudiante SET foto_url=? WHERE id=?", ubicacion, estudianteId);
    }

    @Override
    public Map<String, Object> consultarFoto(long estudianteId) {
        return sql.one("SELECT foto_url FROM estudiante WHERE id=?", estudianteId);
    }

    @Override
    public List<Map<String, Object>> validarIdentidad(long estudianteId, String claveHash) {
        return sql.list(
            "SELECT id FROM estudiante WHERE id=? AND cuenta_activa=true AND contrasena_hash=?",
            estudianteId,
            claveHash
        );
    }

    @Override
    public int verificarCorreo(long estudianteId, String claveHash) {
        return sql.update(
            "UPDATE estudiante SET correo_verificado=true WHERE id=? AND cuenta_activa=true AND " +
                "contrasena_hash=?",
            estudianteId,
            claveHash
        );
    }

    @Override
    public List<Map<String, Object>> buscarPorCorreo(String correo) {
        return sql.list(
            "SELECT id FROM estudiante WHERE lower(correo)=? AND cuenta_activa=true",
            correo
        );
    }

    @Override
    public int cambiarClave(String claveHash, long estudianteId) {
        return sql.update(
            "UPDATE estudiante SET contrasena_hash=? WHERE id=?",
            claveHash,
            estudianteId
        );
    }

    @Override
    public long crear(
        String correo,
        String claveHash,
        String nombre,
        String carrera,
        String ciclo
    ) {
        return sql.insert(
            "INSERT INTO estudiante(correo,contrasena_hash,nombre,carrera,ciclo) VALUES (?,?,?,?,?)",
            correo,
            claveHash,
            nombre,
            carrera,
            ciclo
        );
    }

    @Override
    public List<Map<String, Object>> credenciales(String correo) {
        return sql.list(
            "SELECT id,contrasena_hash FROM estudiante WHERE lower(correo)=? AND cuenta_activa=true",
            correo
        );
    }

    @Override
    public int actualizarPerfil(
        String nombre,
        String carrera,
        String ciclo,
        String biografia,
        boolean avisosActivos,
        long estudianteId
    ) {
        return sql.update(
            "UPDATE estudiante SET nombre=?,carrera=?,ciclo=?,bio=?,push_activo=? WHERE id=?",
            nombre,
            carrera,
            ciclo,
            biografia,
            avisosActivos,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> buscarCompaneros(
        long estudianteId,
        boolean recomendados,
        String patronBusqueda,
        String carrera,
        String curso,
        String patronCurso,
        String rolCompanero,
        String rolPropio,
        Integer dia
    ) {
        return sql.list(
            "WITH slots AS (SELECT estudiante_id,dia_semana,range_agg(tsrange(DATE " +
                "'2000-01-03'+hora_inicio,DATE '2000-01-03'+hora_fin,'[)')) AS tramos " +
                "FROM disponibilidad_horaria GROUP BY estudiante_id,dia_semana), shared_slots AS " +
                "(SELECT b.estudiante_id,a.dia_semana,unnest(a.tramos*b.tramos) AS tramo " +
                "FROM slots a JOIN slots b ON a.dia_semana=b.dia_semana WHERE a.estudiante_id=? AND " +
                "b.estudiante_id<>?), common AS (" +
                "SELECT estudiante_id,string_agg(DISTINCT dia_semana::text,',') AS " +
                "dias_en_comun,SUM(EXTRACT(EPOCH FROM (upper(tramo)-lower(tramo)))/60) AS " +
                "minutos_en_comun " +
                "FROM shared_slots WHERE upper(tramo)-lower(tramo)>=INTERVAL '1 hour' GROUP BY " +
                "estudiante_id), candidates AS (" +
                "SELECT " +
                "e.id,e.nombre,e.carrera,e.ciclo,e.bio,h.dias_en_comun,COALESCE(h.minutos_en_comun,0) " +
                "AS minutos_en_comun," +
                "(SELECT string_agg(DISTINCT d.dia_semana::text,',') FROM disponibilidad_horaria d " +
                "WHERE d.estudiante_id=e.id) AS dias_disponibles," +
                "EXISTS(SELECT 1 FROM estudiante_materia a JOIN estudiante_materia b ON " +
                "a.materia_id=b.materia_id WHERE a.estudiante_id=? AND b.estudiante_id=e.id " +
                "AND ((a.tipo_rol='ENSENAR' AND b.tipo_rol='APRENDER') OR (a.tipo_rol='APRENDER' AND b.tipo_rol='ENSENAR'))) AS materia_compatible " +
                "FROM estudiante e LEFT JOIN common h ON h.estudiante_id=e.id WHERE " +
                "e.cuenta_activa=true AND e.id<>?) " +
                "SELECT e.* FROM candidates e WHERE (?=false OR (e.materia_compatible AND e.minutos_en_comun>=60)) " +
                "AND (e.nombre ILIKE ? OR e.carrera ILIKE ? OR EXISTS(SELECT 1 FROM " +
                "estudiante_materia em JOIN materia m ON m.id=em.materia_id WHERE " +
                "em.estudiante_id=e.id AND m.nombre ILIKE ?)) " +
                "AND (?='' OR e.carrera=?) " +
                "AND (?='' OR EXISTS(SELECT 1 FROM estudiante_materia cr JOIN materia cm ON " +
                "cm.id=cr.materia_id WHERE cr.estudiante_id=e.id AND cm.nombre ILIKE ? AND (?='' OR " +
                "cr.tipo_rol=?))) " +
                "AND (?='' OR EXISTS(SELECT 1 FROM estudiante_materia rr WHERE rr.estudiante_id=e.id " +
                "AND rr.tipo_rol=?)) " +
                "AND (?=false OR ?='' OR EXISTS(SELECT 1 FROM estudiante_materia own JOIN " +
                "estudiante_materia peer ON own.materia_id=peer.materia_id WHERE own.estudiante_id=? " +
                "AND peer.estudiante_id=e.id AND own.tipo_rol=? AND peer.tipo_rol=? AND (?='' OR " +
                "EXISTS(SELECT 1 FROM materia rm WHERE rm.id=own.materia_id AND rm.nombre ILIKE ?)))) " +
                "AND (CAST(? AS integer) IS NULL OR EXISTS(SELECT 1 FROM shared_slots sd WHERE " +
                "sd.estudiante_id=e.id AND sd.dia_semana=? AND " +
                "upper(sd.tramo)-lower(sd.tramo)>=INTERVAL '1 hour')) " +
                "ORDER BY e.materia_compatible DESC,e.minutos_en_comun DESC,e.nombre,e.id LIMIT 50",
            estudianteId,
            estudianteId,
            estudianteId,
            estudianteId,
            recomendados,
            patronBusqueda,
            patronBusqueda,
            patronBusqueda,
            carrera,
            carrera,
            curso,
            patronCurso,
            rolCompanero,
            rolCompanero,
            rolCompanero,
            rolCompanero,
            recomendados,
            rolPropio,
            estudianteId,
            rolPropio,
            rolCompanero,
            curso,
            patronCurso,
            dia,
            dia
        );
    }

    @Override
    public Map<String, Object> bloquear(long estudianteId) {
        return sql.one("SELECT id FROM estudiante WHERE id=? FOR UPDATE", estudianteId);
    }
}
