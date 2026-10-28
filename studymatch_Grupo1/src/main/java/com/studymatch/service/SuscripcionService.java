package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static com.studymatch.service.Validaciones.number;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.SuscripcionDAO;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Gestiona planes y suscripciones. Los pagos de demostración no hacen cobros. */
@Service
public class SuscripcionService {

    private final EstudianteDAO estudianteDAO;
    private final SuscripcionDAO suscripcionDAO;
    private final PdfService pdf;

    public SuscripcionService(EstudianteDAO estudianteDAO, SuscripcionDAO suscripcionDAO, PdfService pdf) {
        this.estudianteDAO = estudianteDAO;
        this.suscripcionDAO = suscripcionDAO;
        this.pdf=pdf;
    }

    /** Activa el periodo de prueba si el estudiante todavía puede utilizarlo. */
    @Transactional
    public void trial(long student, Map<String, Object> input) {
        estudianteDAO.bloquear(student);
        if (
            !suscripcionDAO.buscarPruebaAnterior(student).isEmpty()
        ) throw new ResponseStatusException(CONFLICT, "Ya utilizaste el periodo de prueba.");
        long plan = number(input, "plan_id");
        suscripcionDAO.validarPlan(plan);
        if (!suscripcionDAO.buscarVigente(student).isEmpty()) throw new ResponseStatusException(
            CONFLICT,
            "Ya tienes una suscripción vigente."
        );
        suscripcionDAO.crearPrueba(student, plan);
    }

    public Object plans() {
        var rows = suscripcionDAO.planesActivos();
        rows.forEach(p ->
            p.put("beneficios", suscripcionDAO.beneficiosPlan(((Number) p.get("id")).longValue()))
        );
        return rows;
    }

    public Object subscriptions(long student) {
        return suscripcionDAO.listarDelEstudiante(student);
    }

    @Transactional
    public Object cancel(long student, long id) {
        estudianteDAO.bloquear(student);
        if (suscripcionDAO.cancelar(id, student) == 0) throw new ResponseStatusException(
            CONFLICT, "Esta suscripción no está activa o no te pertenece.");
        return Map.of("mensaje", "Ahora utilizas el Plan Gratuito.");
    }

    public Object payments(long student) {
        return suscripcionDAO.pagosDelEstudiante(student);
    }

    /** Simula el pago y activa Pro en una misma transacción. Nunca contacta a un banco. */
    @Transactional
    public Object paymentDemo(long student, Map<String, Object> input) {
        String scenario = Validaciones.text(input, "escenario", 20);
        if (
            !Set.of("APROBADO", "RECHAZADO", "TIMEOUT").contains(scenario)
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un escenario de prueba.");
        var plan = suscripcionDAO.buscarPlan(Validaciones.number(input, "plan_id"));
        var precio = new java.math.BigDecimal(plan.get("precio").toString());
        if (precio.signum() <= 0) throw new ResponseStatusException(BAD_REQUEST,
            "Selecciona el Plan Pro-Ciclo.");
        String periodo = java.util.Objects.toString(plan.get("periodo"), "").toLowerCase();
        boolean mensual = periodo.contains("mes") && !periodo.contains("seman");
        String modalidad = mensual ? "MENSUAL" : "SEMESTRAL";
        String referencia = text(input, "referencia", 36);
        try { java.util.UUID.fromString(referencia); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(BAD_REQUEST,
            "Vuelve a abrir el formulario de pago."); }
        if (scenario.equals("APROBADO") && !"4111111111111111".equals(input.get("tarjeta_prueba")))
            throw new ResponseStatusException(BAD_REQUEST,
                "Usa únicamente la tarjeta de prueba 4111 1111 1111 1111.");
        estudianteDAO.bloquear(student);
        String comprobante = "DEMO-" + referencia.replace("-", "").substring(0,15);
        var anterior = suscripcionDAO.pagoDemoAnterior(student, comprobante);
        if (!anterior.isEmpty()) {
            if (((Number) anterior.get(0).get("plan_id")).longValue() != number(input,"plan_id"))
                throw new ResponseStatusException(CONFLICT,"Vuelve a abrir el formulario para cambiar de plan.");
            String previo = anterior.get(0).get("estado").toString();
            return Map.of("modo","PRUEBA","estado",previo.equals("COMPLETADO") ? "APROBADO" : previo,"plan",plan,
                "pago_id",anterior.get(0).get("id"),"numero_comprobante",comprobante,
                "mensaje","Este intento ya fue procesado. Consulta tu suscripción.");
        }
        var vigentes = suscripcionDAO.buscarVigente(student);
        if (scenario.equals("APROBADO") && !Boolean.TRUE.equals(input.get("renovar")) && vigentes.stream()
            .anyMatch(s -> java.util.Objects.equals(s.get("plan_id"), plan.get("id"))))
            throw new ResponseStatusException(CONFLICT,"Ya tienes este plan vigente. Confirma su renovación para continuar.");
        // Rechazos y demoras se registran sin alterar la suscripción vigente.
        long pago = suscripcionDAO.registrarPagoDemo(student, number(input,"plan_id"), modalidad,
            precio, plan.get("moneda").toString(), scenario.equals("APROBADO") ? "COMPLETADO" : scenario,
            comprobante);
        if (scenario.equals("APROBADO")) suscripcionDAO.activarPlan(student, number(input,"plan_id"), mensual ? 30 : 126);
        return Map.of(
            "modo",
            "PRUEBA",
            "estado",
            scenario,
            "plan",
            plan,
            "pago_id", pago,
            "numero_comprobante", comprobante,
            "mensaje",
            switch (scenario) {
                case "APROBADO" -> "Plan Pro-Ciclo activado mediante pago simulado. No se cobró dinero real.";
                case "RECHAZADO" -> "Rechazo de demostración. No se realizó ningún cobro.";
                default -> "Demora de demostración. No se realizó ningún cobro.";
            }
        );
    }

    /** Beneficios comprobados con la suscripción vigente, sin confiar en el navegador. */
    public Map<String,Object> acceso(long student) {
        boolean pro = suscripcionDAO.tienePro(student);
        long usados = suscripcionDAO.emparejamientosDelMes(student);
        return Map.of("pro",pro,"plan",pro ? "Pro-Ciclo" : "Gratuito",
            "limite_mensual",pro ? -1 : 5,"usados",usados,
            "filtros_avanzados",pro,"ranking",pro);
    }

    public void exigirPro(long student) {
        if (!suscripcionDAO.tienePro(student)) throw new ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,
            "Esta función está incluida en el Plan Pro-Ciclo. Puedes activarlo en Plan Pro-Ciclo.");
    }

    public Object receipt(long student, long id) {
        return suscripcionDAO.comprobante(id, student);
    }

    public byte[] comprobantePdf(long estudiante,long pago){
        var registro=suscripcionDAO.comprobante(pago,estudiante);
        if(!"COMPLETADO".equals(registro.get("estado")))throw new ResponseStatusException(CONFLICT,"Solo se emiten comprobantes de pagos completados.");
        boolean demo = registro.get("numero_comprobante").toString().startsWith("DEMO-");
        return pdf.documento(demo ? "Comprobante de simulación StudyMatch" : "Comprobante de pago StudyMatch",java.util.List.of("Número: "+registro.get("numero_comprobante"),
            demo ? "DEMOSTRACIÓN: no se realizó ningún cobro real. No es un comprobante fiscal." : "Pago registrado.",
            "Monto: "+registro.get("monto")+" "+registro.get("moneda"),"Estado: "+registro.get("estado")));
    }
}
