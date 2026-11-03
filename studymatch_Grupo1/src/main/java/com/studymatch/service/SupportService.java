package com.studymatch.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.SoporteDAO;
import com.studymatch.model.SolicitudSoporte;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Valida solicitudes de ayuda y reportes de compañeros. */
@Service
public class SupportService {

    private final EstudianteDAO estudianteDAO;
    private final SoporteDAO soporteDAO;

    public SupportService(EstudianteDAO estudianteDAO, SoporteDAO soporteDAO) {
        this.estudianteDAO = estudianteDAO;
        this.soporteDAO = soporteDAO;
    }

    @Transactional
    public Map<String, Object> create(long student, SolicitudSoporte input, boolean report) {
        String category = com.studymatch.service.Validaciones.required(
            input.categoria(),
            "categoría",
            100
        );
        String message = com.studymatch.service.Validaciones.required(
            input.mensaje(),
            "mensaje",
            2000
        );
        String subject = report
            ? "Reporte de compañero"
            : com.studymatch.service.Validaciones.required(input.asunto(), "asunto", 150);
        Long other = report ? input.reportadoId() : null;
        if (report) {
            if (other == null || other == student) throw new ResponseStatusException(
                BAD_REQUEST,
                "Selecciona otro estudiante para reportar."
            );
            estudianteDAO.perfil(other);
            if (
                !Set.of(
                    "Comportamiento inadecuado",
                    "Inasistencia o impuntualidad reiterada",
                    "Acoso, insultos o lenguaje ofensivo",
                    "Spam o contenido comercial no solicitado",
                    "Suplantación de identidad o perfil falso",
                    "Cobro indebido por tutorías (Incumplimiento de reciprocidad)",
                    "Otro motivo"
                ).contains(category)
            ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un motivo válido.");
        } else if (
            !Set.of(
                "Suscripciones y Pagos",
                "Emparejamientos y Sesiones",
                "Cuenta y Perfil",
                "Problemas Técnicos",
                "Sugerencias y Otros"
            ).contains(category)
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona una categoría válida.");
        estudianteDAO.bloquear(student);
        if (report) {
            var previous = soporteDAO.reportePendiente(student, other);
            if (!previous.isEmpty()) return result(
                ((Number) previous.get(0).get("id")).longValue(),
                true
            );
        }
        long id = soporteDAO.crear(
            student,
            other,
            report ? "REPORTE" : "SOPORTE",
            category,
            subject,
            message
        );
        if(report && soporteDAO.contarReportes(other)>=3)soporteDAO.priorizar(id);
        return result(id, false);
    }

    private Map<String, Object> result(long id, boolean existing) {
        return Map.of(
            "id",
            id,
            "codigo",
            "SM-" + String.format(Locale.ROOT, "%06d", id),
            "estado",
            "PENDIENTE",
            "existente",
            existing
        );
    }
}
