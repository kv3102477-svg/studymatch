package com.studymatch.dao.jdbc;

import com.studymatch.dao.SuscripcionDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa SuscripcionDAO con consultas a PostgreSQL. */
@Repository
public class JdbcSuscripcionDAO implements SuscripcionDAO {

    private final SqlExecutor sql;

    public JdbcSuscripcionDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> planesActivos() {
        return sql.list("SELECT * FROM plan WHERE activo=true ORDER BY precio");
    }

    @Override
    public List<Map<String, Object>> beneficiosPlan(long planId) {
        return sql.list("SELECT texto FROM plan_beneficio WHERE plan_id=? ORDER BY id", planId);
    }

    @Override
    public List<Map<String, Object>> listarDelEstudiante(long estudianteId) {
        return sql.list(
            "SELECT su.*,p.nombre AS plan, CASE WHEN su.fecha_vencimiento<CURRENT_DATE THEN " +
                "'EXPIRADO' ELSE su.estado END AS estado_actual " +
                "FROM suscripcion su LEFT JOIN plan p ON p.id=su.plan_id WHERE estudiante_id=? ORDER " +
                "BY fecha_inicio DESC,su.id DESC",
            estudianteId
        );
    }

    @Override
    public int cancelar(long suscripcionId, long estudianteId) {
        return sql.update(
            "UPDATE suscripcion SET estado='EXPIRADO',fecha_vencimiento=CURRENT_DATE WHERE id=? " +
                "AND estudiante_id=? AND estado IN ('ACTIVO','TRIAL')",
            suscripcionId,
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> pagosDelEstudiante(long estudianteId) {
        return sql.list(
            "SELECT " +
                "id,modalidad,monto,moneda,ultimos_digitos,marca_tarjeta,estado,numero_comprobante,pagado_en FROM pago WHERE estudiante_id=? ORDER BY pagado_en DESC",
            estudianteId
        );
    }

    @Override
    public Map<String, Object> buscarPlan(long planId) {
        return sql.one(
            "SELECT id,nombre,precio,moneda FROM plan WHERE id=? AND activo=true",
            planId
        );
    }

    @Override
    public Map<String, Object> comprobante(long pagoId, long estudianteId) {
        return sql.one(
            "SELECT id,monto,moneda,estado,numero_comprobante,pagado_en FROM pago WHERE id=? AND " +
                "estudiante_id=? AND estado='COMPLETADO'",
            pagoId,
            estudianteId
        );
    }

    @Override
    public int crearGratuita(long estudianteId) {
        return sql.update(
            "INSERT INTO suscripcion(estudiante_id,estado,es_trial,fecha_inicio) VALUES " +
                "(?,'GRATUITO',false,CURRENT_DATE)",
            estudianteId
        );
    }

    @Override
    public List<Map<String, Object>> buscarPruebaAnterior(long estudianteId) {
        return sql.list(
            "SELECT id FROM suscripcion WHERE estudiante_id=? AND es_trial=true",
            estudianteId
        );
    }

    @Override
    public Map<String, Object> validarPlan(long planId) {
        return sql.one("SELECT id FROM plan WHERE id=? AND activo=true", planId);
    }

    @Override
    public List<Map<String, Object>> buscarVigente(long estudianteId) {
        return sql.list(
            "SELECT id FROM suscripcion WHERE estudiante_id=? AND estado IN ('ACTIVO','TRIAL') " +
                "AND fecha_vencimiento>=CURRENT_DATE",
            estudianteId
        );
    }

    @Override
    public int crearPrueba(long estudianteId, long planId) {
        return sql.update(
            "INSERT INTO " +
                "suscripcion(estudiante_id,plan_id,estado,es_trial,fecha_inicio,fecha_vencimiento) " +
                "VALUES (?,?,'TRIAL',true,CURRENT_DATE,CURRENT_DATE+60)",
            estudianteId,
            planId
        );
    }

    @Override
    public int expirarVencidas() {
        return sql.update(
            "UPDATE suscripcion SET estado='EXPIRADO' WHERE estado IN ('TRIAL','ACTIVO') AND " +
                "fecha_vencimiento<CURRENT_DATE"
        );
    }
}
