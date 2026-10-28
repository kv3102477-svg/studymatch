package com.studymatch.service;

import static com.studymatch.service.Validaciones.number;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.studymatch.dao.DisponibilidadDAO;
import com.studymatch.dao.EstudianteDAO;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Valida y guarda los bloques horarios del estudiante. */
@Service
public class DisponibilidadService {

    private final DisponibilidadDAO disponibilidadDAO;
    private final EstudianteDAO estudianteDAO;

    public DisponibilidadService(DisponibilidadDAO disponibilidadDAO, EstudianteDAO estudianteDAO) {
        this.disponibilidadDAO = disponibilidadDAO;
        this.estudianteDAO = estudianteDAO;
    }

    /** Consulta o incorpora disponibilidad; al guardar, comprueba los cruces. */
    @Transactional
    public void availability(long student, Map<String, Object> input) {
        int day = (int) number(input, "dia_semana");
        LocalTime start = LocalTime.parse(text(input, "hora_inicio", 8)),
            end = LocalTime.parse(text(input, "hora_fin", 8));
        if (day > 7 || !end.isAfter(start)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Revisa el día y el intervalo horario."
        );
        estudianteDAO.bloquear(student);
        if (
            !disponibilidadDAO.buscarCruces(student, day, end, start).isEmpty()
        ) throw new ResponseStatusException(
            CONFLICT,
            "Ese horario se superpone con otro existente."
        );
        disponibilidadDAO.crear(student, day, start, end);
    }

    /** Valida todos los bloques antes de reemplazar el horario guardado. */
    @Transactional
    public void replaceAvailability(long student, List<Map<String, Object>> slots) {
        if (slots.size() > 168) throw new ResponseStatusException(
            BAD_REQUEST,
            "Hay demasiados bloques horarios."
        );
        List<Object[]> rows = new ArrayList<>();
        Map<Integer, List<LocalTime[]>> days = new HashMap<>();
        for (var slot : slots) {
            long day = number(slot, "dia_semana");
            LocalTime start = LocalTime.parse(text(slot, "hora_inicio", 8)),
                end = LocalTime.parse(text(slot, "hora_fin", 8));
            if (day > 7 || !end.isAfter(start)) throw new ResponseStatusException(
                BAD_REQUEST,
                "Revisa el día y el intervalo horario."
            );
            var intervals = days.computeIfAbsent((int) day, key -> new ArrayList<>());
            if (
                intervals
                    .stream()
                    .anyMatch(range -> start.isBefore(range[1]) && end.isAfter(range[0]))
            ) throw new ResponseStatusException(CONFLICT, "Hay horarios superpuestos.");
            intervals.add(new LocalTime[] { start, end });
            rows.add(new Object[] { student, (int) day, start, end });
        }
        estudianteDAO.bloquear(student);
        disponibilidadDAO.eliminarDelEstudiante(student);
        if (!rows.isEmpty()) disponibilidadDAO.guardarBloques(rows);
    }

    /** Consulta o incorpora disponibilidad; al guardar, comprueba los cruces. */
    @Transactional
    public Object availability(long student) {
        return disponibilidadDAO.listar(student);
    }
}
