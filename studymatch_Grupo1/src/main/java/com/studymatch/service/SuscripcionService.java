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

    public SuscripcionService(EstudianteDAO estudianteDAO, SuscripcionDAO suscripcionDAO) {
        this.estudianteDAO = estudianteDAO;
        this.suscripcionDAO = suscripcionDAO;
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
        suscripcionDAO.cancelar(id, student);
        return Map.of("mensaje", "Suscripción cancelada.");
    }

    public Object payments(long student) {
        return suscripcionDAO.pagosDelEstudiante(student);
    }

    /** Devuelve un resultado de prueba sin cobrar ni registrar pagos reales. */
    public Object paymentDemo(Map<String, Object> input) {
        String scenario = Validaciones.text(input, "escenario", 20);
        if (
            !Set.of("APROBADO", "RECHAZADO", "TIMEOUT").contains(scenario)
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un escenario de prueba.");
        var plan = suscripcionDAO.buscarPlan(Validaciones.number(input, "plan_id"));
        return Map.of(
            "modo",
            "PRUEBA",
            "estado",
            scenario,
            "plan",
            plan,
            "mensaje",
            switch (scenario) {
                case "APROBADO" -> "Prueba aprobada. No se cobró dinero ni se activó una suscripción pagada.";
                case "RECHAZADO" -> "Rechazo de demostración. No se realizó ningún cobro.";
                default -> "Demora de demostración. No se realizó ningún cobro.";
            }
        );
    }

    public Object receipt(long student, long id) {
        return suscripcionDAO.comprobante(id, student);
    }
}
