package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static com.studymatch.service.Validaciones.number;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.MateriaDAO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Gestiona las materias que un estudiante enseña o quiere aprender. */
@Service
public class MateriaService {

    private final EstudianteDAO estudianteDAO;
    private final MateriaDAO materiaDAO;

    public MateriaService(EstudianteDAO estudianteDAO, MateriaDAO materiaDAO) {
        this.estudianteDAO = estudianteDAO;
        this.materiaDAO = materiaDAO;
    }

    /** Valida el rol y el nivel antes de guardar una materia del estudiante. */
    @Transactional
    public void subject(long student, Map<String, Object> input) {
        String role = text(input, "tipo_rol", 10),
            level = text(input, "nivel", 15);
        if (
            !Set.of("ENSENAR", "APRENDER").contains(role) ||
            !Set.of("BASICO", "INTERMEDIO", "AVANZADO").contains(level)
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un rol y nivel válidos.");
        long subject = number(input, "materia_id");
        materiaDAO.guardarPropia(student, subject, role, level);
    }

    /** Devuelve materias en las que uno enseña y el otro necesita aprender. */
    public List<Map<String, Object>> matchingSubjects(long student, long other) {
        estudianteDAO.perfil(other);
        return materiaDAO.coincidencias(student, other);
    }

    public Object subjects(String q, int pagina) {
        if (q.length() > 100 || pagina < 1 || pagina > 1000000) throw new ResponseStatusException(
            BAD_REQUEST,
            "Revisa el texto o la página de búsqueda."
        );
        String query = q.trim();
        long total = (
            (Number) materiaDAO.contarBusqueda("%" + query + "%").get("total")
        ).longValue();
        var rows = materiaDAO.buscarPagina("%" + query + "%", (pagina - 1) * 20);
        return Map.of("items", rows, "total", total, "pagina", pagina, "tamano", 20);
    }

    public Object mine(long student) {
        return materiaDAO.listarPropias(student);
    }

    @Transactional
    public Object removeSubject(long student, long id) {
        materiaDAO.eliminarPropia(id, student);
        return Map.of("mensaje", "Materia eliminada.");
    }
}
