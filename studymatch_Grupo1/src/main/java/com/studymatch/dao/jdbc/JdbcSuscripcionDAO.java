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
        int actualizadas = sql.update(
            "UPDATE suscripcion SET estado='EXPIRADO',fecha_vencimiento=CURRENT_DATE WHERE id=? " +
                "AND estudiante_id=? AND estado IN ('ACTIVO','TRIAL')",
            suscripcionId,
            estudianteId
        );
        if (actualizadas > 0) crearGratuita(estudianteId);
        return actualizadas;
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
            "SELECT id,nombre,precio,moneda,periodo FROM plan WHERE id=? AND activo=true",
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
            "SELECT id,plan_id FROM suscripcion WHERE estudiante_id=? AND estado IN ('ACTIVO','TRIAL') " +
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

    @Override
    public long registrarPagoDemo(long estudiante, long plan, String modalidad,
        java.math.BigDecimal monto, String moneda, String estado, String comprobante) {
        return sql.insert("INSERT INTO pago(estudiante_id,plan_id,modalidad,monto,moneda,estado,"
            + "numero_comprobante,codigo_autorizacion,marca_tarjeta) VALUES (?,?,?,?,?,?,?,?,?)",
            estudiante, plan, modalidad, monto, moneda, estado, comprobante,
            "DEMO-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0,12), "SIMULADO");
    }

    @Override
    public void activarPlan(long estudiante, long plan, int dias) {
        var base = sql.one("SELECT COALESCE(MAX(fecha_vencimiento),CURRENT_DATE) AS fecha "
            + "FROM suscripcion WHERE estudiante_id=? AND plan_id=? AND estado IN ('ACTIVO','TRIAL') "
            + "AND fecha_vencimiento>=CURRENT_DATE", estudiante, plan).get("fecha");
        var inicio = java.time.LocalDate.parse(base.toString());
        var vencimiento = dias == 30 ? inicio.plusMonths(1) : inicio.plusDays(dias);
        sql.update("UPDATE suscripcion SET estado='EXPIRADO' WHERE estudiante_id=? "
            + "AND estado IN ('TRIAL','ACTIVO','GRATUITO')", estudiante);
        sql.update("INSERT INTO suscripcion(estudiante_id,plan_id,estado,es_trial,fecha_inicio,fecha_vencimiento) "
            + "VALUES (?,?,'ACTIVO',false,CURRENT_DATE,?)", estudiante, plan, java.sql.Date.valueOf(vencimiento));
    }

    @Override
    public boolean tienePro(long estudiante) {
        return (Boolean) sql.one("SELECT EXISTS(SELECT 1 FROM suscripcion WHERE estudiante_id=? "
            + "AND estado IN ('ACTIVO','TRIAL') AND fecha_inicio<=CURRENT_DATE "
            + "AND fecha_vencimiento>=CURRENT_DATE) AS pro", estudiante).get("pro");
    }

    @Override
    public long emparejamientosDelMes(long estudiante) {
        return ((Number) sql.one("SELECT count(*) AS total FROM emparejamiento WHERE proponente_id=? "
            + "AND creado_en>=date_trunc('month',CURRENT_TIMESTAMP)", estudiante).get("total")).longValue();
    }

    @Override
    public List<Map<String, Object>> pagoDemoAnterior(long estudiante, String comprobante) {
        return sql.list("SELECT id,plan_id,estado FROM pago WHERE estudiante_id=? AND numero_comprobante=?",
            estudiante, comprobante);
    }
}
