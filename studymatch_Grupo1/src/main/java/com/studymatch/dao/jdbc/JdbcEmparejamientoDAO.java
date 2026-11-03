package com.studymatch.dao.jdbc;
import com.studymatch.dao.EmparejamientoDAO;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcEmparejamientoDAO implements EmparejamientoDAO {
    private final SqlExecutor sql;public JdbcEmparejamientoDAO(SqlExecutor sql){this.sql=sql;}
    public long crear(long proponente,long receptor,long materia,int porcentaje){
        sql.one("SELECT id FROM estudiante WHERE id=? FOR UPDATE",Math.min(proponente,receptor));
        var anteriores=sql.list("SELECT id FROM emparejamiento WHERE proponente_id=? AND receptor_id=? "
            + "AND materia_interes_id=? AND estado='PENDIENTE' ORDER BY id LIMIT 1",proponente,receptor,materia);
        if(!anteriores.isEmpty())return ((Number)anteriores.get(0).get("id")).longValue();
        sql.one("SELECT id FROM estudiante WHERE id=? FOR UPDATE",Math.max(proponente,receptor));
        var acceso=sql.one("SELECT EXISTS(SELECT 1 FROM suscripcion WHERE estudiante_id=? "
            + "AND estado IN ('ACTIVO','TRIAL') AND fecha_inicio<=CURRENT_DATE AND fecha_vencimiento>=CURRENT_DATE) AS pro, "
            + "(SELECT count(*) FROM emparejamiento WHERE proponente_id=? AND creado_en>=date_trunc('month',CURRENT_TIMESTAMP)) AS usados",
            proponente,proponente);
        if(!Boolean.TRUE.equals(acceso.get("pro")) && ((Number)acceso.get("usados")).longValue()>=5)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                "Alcanzaste los 5 emparejamientos de este mes del Plan Gratuito. Activa Pro-Ciclo para continuar sin límite.");
        return sql.insert("INSERT INTO emparejamiento(proponente_id,receptor_id,materia_interes_id,porcentaje_compatibilidad,estado) "
            + "VALUES (?,?,?,?,'PENDIENTE')",proponente,receptor,materia,porcentaje);
    }

    @Override public int responder(long estudiante,long companero,long materia,boolean aceptar){
        return sql.update("UPDATE emparejamiento SET estado=?,respondido_en=CURRENT_TIMESTAMP WHERE estado='PENDIENTE' "
            + "AND materia_interes_id=? AND ((proponente_id=? AND receptor_id=?) OR (proponente_id=? AND receptor_id=?))",
            aceptar?"ACEPTADO":"RECHAZADO",materia,estudiante,companero,companero,estudiante);
    }
}
