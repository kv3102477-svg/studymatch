package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.InsigniaDAO;
import com.studymatch.dao.SesionDAO;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Calcula horas, insignias y ranking a partir de las sesiones registradas. */
@Service
public class ProgresoService {

    private final EstudianteDAO estudianteDAO;
    private final InsigniaDAO insigniaDAO;
    private final SesionDAO sesionDAO;

    public ProgresoService(
        EstudianteDAO estudianteDAO,
        InsigniaDAO insigniaDAO,
        SesionDAO sesionDAO
    ) {
        this.estudianteDAO = estudianteDAO;
        this.insigniaDAO = insigniaDAO;
        this.sesionDAO = sesionDAO;
    }

    /** Calcula las horas enseñadas y aprendidas de sesiones completadas. */
    public Map<String, Object> hours(long student) {
        if (!sesionDAO.tieneRoles()) throw new ResponseStatusException(
            CONFLICT,
            "Faltan mentor_id y aprendiz_id en la tabla sesion. Hay que actualizar el esquema."
        );
        return sesionDAO.horasAcumuladas(student);
    }

    /** Consulta las insignias obtenidas y el progreso real del estudiante. */
    public List<Map<String, Object>> badges(long student) {
        return insigniaDAO.listarConProgreso(student);
    }

    /** Registra las insignias cuya meta de horas ya fue alcanzada. */
    public void award(long student) {
        insigniaDAO.otorgarAlcanzadas(student);
    }

    /** Actualiza el resumen de horas y calificaciones del estudiante. */
    public void refreshStats(long student) {
        insigniaDAO.actualizarEstadisticas(student);
    }

    /** Ordena los mentores por su actividad del ciclo académico activo. */
    public List<Map<String, Object>> ranking() {
        if (!sesionDAO.tieneRoles()) throw new ResponseStatusException(
            CONFLICT,
            "Faltan los roles de sesión en el esquema."
        );
        return insigniaDAO.ranking();
    }

    /** Reúne los datos necesarios para la constancia de participación. */
    public Object certificate(long student) {
        long id = student;
        return Map.of(
            "perfil",
            estudianteDAO.perfil(id),
            "horas",
            hours(id),
            "resumen",
            summary(student),
            "emitido_en",
            java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).toString()
        );
    }

    /** Consulta las calificaciones y el número de compañeros del estudiante. */
    public Object summary(long student) {
        long id = student;
        return estudianteDAO.resumen(id);
    }
}
