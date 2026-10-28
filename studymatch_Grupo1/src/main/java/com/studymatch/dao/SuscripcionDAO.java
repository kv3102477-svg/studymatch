package com.studymatch.dao;

import java.util.List;
import java.util.Map;

/** Define el acceso a los datos de suscripcion. Sus consultas están en la implementación JDBC. */
public interface SuscripcionDAO {
    /** Consulta los planes disponibles ordenados por precio. */
    List<Map<String, Object>> planesActivos();

    /** Consulta los beneficios del plan seleccionado. */
    List<Map<String, Object>> beneficiosPlan(long planId);

    /** Consulta los registros asociados al estudiante. */
    List<Map<String, Object>> listarDelEstudiante(long estudianteId);

    /** Cancela el registro indicado dentro de los límites de su propietario. */
    int cancelar(long suscripcionId, long estudianteId);

    /** Consulta los pagos registrados del estudiante. */
    List<Map<String, Object>> pagosDelEstudiante(long estudianteId);

    /** Consulta los datos de un plan activo. */
    Map<String, Object> buscarPlan(long planId);

    /** Consulta un pago completado que pertenece al estudiante. */
    Map<String, Object> comprobante(long pagoId, long estudianteId);

    /** Crea la suscripción gratuita inicial del estudiante. */
    int crearGratuita(long estudianteId);

    /** Comprueba si el estudiante ya utilizó el periodo de prueba. */
    List<Map<String, Object>> buscarPruebaAnterior(long estudianteId);

    /** Busca el plan únicamente si está activo. */
    Map<String, Object> validarPlan(long planId);

    /** Busca una suscripción que todavía esté vigente. */
    List<Map<String, Object>> buscarVigente(long estudianteId);

    /** Registra el periodo de prueba del plan seleccionado. */
    int crearPrueba(long estudianteId, long planId);

    /** Marca las suscripciones cuyo vencimiento ya pasó. */
    int expirarVencidas();

    /** Registra un pago de demostración y devuelve su identificador. */
    long registrarPagoDemo(long estudiante, long plan, String modalidad, java.math.BigDecimal monto,
        String moneda, String estado, String comprobante);

    /** Sustituye el plan vigente conservando su historial. */
    void activarPlan(long estudiante, long plan, int dias);

    /** Comprueba el acceso Pro con las fechas del servidor. */
    boolean tienePro(long estudiante);

    /** Cuenta los emparejamientos iniciados durante este mes. */
    long emparejamientosDelMes(long estudiante);

    List<Map<String, Object>> pagoDemoAnterior(long estudiante, String comprobante);
}
