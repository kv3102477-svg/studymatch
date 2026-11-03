package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.DisponibilidadDAO;
import com.studymatch.dao.MateriaDAO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Busca compañeros compatibles según materias y disponibilidad. */
@Service
public class BusquedaService {

    private final EstudianteDAO estudianteDAO;
    private final MateriaDAO materiaDAO;
    private final DisponibilidadDAO disponibilidadDAO;

    public BusquedaService(EstudianteDAO estudianteDAO, MateriaDAO materiaDAO, DisponibilidadDAO disponibilidadDAO) {
        this.estudianteDAO = estudianteDAO;
        this.materiaDAO = materiaDAO;
        this.disponibilidadDAO = disponibilidadDAO;
    }

    /** Aplica los filtros y reúne las materias y horarios compatibles. */
    public List<Map<String, Object>> search(long student, String query) {
        return search(student, query, false);
    }

    /** Aplica los filtros y reúne las materias y horarios compatibles. */
    public List<Map<String, Object>> search(long student, String query, boolean recommended) {
        return search(student, query, recommended, "", null, "", "");
    }

    /** Aplica los filtros y reúne las materias y horarios compatibles. */
    public List<Map<String, Object>> search(
        long student,
        String query,
        boolean recommended,
        String course,
        Integer day,
        String career,
        String role
    ) {
        if (recommended && (materiaDAO.listarPropias(student).isEmpty()
            || disponibilidadDAO.listar(student).isEmpty())) throw new ResponseStatusException(
                BAD_REQUEST, "Completa tus materias y disponibilidad para ver sugerencias");
        if (day != null && (day < 1 || day > 7)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona un día válido."
        );
        if (
            !role.isEmpty() && !Set.of("ENSENAR", "APRENDER").contains(role)
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un rol válido.");
        if (
            query.length() > 100 || course.length() > 100 || career.length() > 100
        ) throw new ResponseStatusException(BAD_REQUEST, "El filtro es demasiado largo.");
        String peerRole = role.equals("APRENDER")
            ? "ENSENAR"
            : role.equals("ENSENAR")
              ? "APRENDER"
              : "";

        var rows = estudianteDAO.buscarCompaneros(
            student,
            recommended,
            "%" + query + "%",
            career,
            course,
            "%" + course + "%",
            peerRole,
            role,
            day
        );
        rows.forEach(row -> {
            String days = Objects.toString(row.get("dias_disponibles"), "");
            row.put(
                "dias_disponibles",
                days.isEmpty()
                    ? List.of()
                    : Arrays.stream(days.split(",")).map(Integer::parseInt).toList()
            );
        });
        if (!rows.isEmpty()) {
            List<Object> params = new ArrayList<>();
            params.add(student);
            rows.forEach(row -> params.add(row.get("id")));
            var subjects = materiaDAO.listarParaComparacion(params.toArray());
            var mine = subjects
                .stream()
                .filter(subject -> id(subject, "estudiante_id") == student)
                .toList();
            for (var row : rows) {
                long other = id(row, "id");
                long complementary = mine
                    .stream()
                    .filter(own ->
                        subjects
                            .stream()
                            .anyMatch(
                                peer ->
                                    id(peer, "estudiante_id") == other &&
                                    peer.get("materia_id").equals(own.get("materia_id")) &&
                                    !peer.get("tipo_rol").equals(own.get("tipo_rol"))
                            )
                    )
                    .count();
                row.put(
                    "coincidencia_materias",
                    mine.isEmpty() ? null : Math.round((complementary * 100.0) / mine.size())
                );
                row.put(
                    "materias",
                    subjects
                        .stream()
                        .filter(peer -> id(peer, "estudiante_id") == other)
                        .map(peer -> peer.get("nombre"))
                        .distinct()
                        .toList()
                );
                for (String subjectRole : List.of("ENSENAR", "APRENDER"))
                    row.put(
                        subjectRole.equals("ENSENAR") ? "materias_ensenar" : "materias_aprender",
                        subjects
                            .stream()
                            .filter(
                                peer ->
                                    id(peer, "estudiante_id") == other &&
                                    subjectRole.equals(peer.get("tipo_rol"))
                            )
                            .map(peer -> peer.get("nombre"))
                            .distinct()
                            .toList()
                    );
            }
        }
        return rows;
    }
}
