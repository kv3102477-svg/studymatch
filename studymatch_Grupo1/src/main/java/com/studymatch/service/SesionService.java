package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static com.studymatch.service.Validaciones.number;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.studymatch.dao.CalificacionDAO;
import com.studymatch.dao.DisponibilidadDAO;
import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.MateriaDAO;
import com.studymatch.dao.NotificacionDAO;
import com.studymatch.dao.SesionDAO;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Valida propuestas y controla los cambios de estado de una sesión. */
@Service
public class SesionService {

    private final CalificacionDAO calificacionDAO;
    private final DisponibilidadDAO disponibilidadDAO;
    private final EstudianteDAO estudianteDAO;
    private final MateriaDAO materiaDAO;
    private final NotificacionDAO notificacionDAO;
    private final SesionDAO sesionDAO;
    private final ProgresoService progresoService;
    private final Clock clock = Clock.system(ZoneId.of("America/Bogota"));

    public SesionService(
        CalificacionDAO calificacionDAO,
        DisponibilidadDAO disponibilidadDAO,
        EstudianteDAO estudianteDAO,
        MateriaDAO materiaDAO,
        NotificacionDAO notificacionDAO,
        SesionDAO sesionDAO,
        ProgresoService progresoService
    ) {
        this.calificacionDAO = calificacionDAO;
        this.disponibilidadDAO = disponibilidadDAO;
        this.estudianteDAO = estudianteDAO;
        this.materiaDAO = materiaDAO;
        this.notificacionDAO = notificacionDAO;
        this.sesionDAO = sesionDAO;
        this.progresoService = progresoService;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Comprueba materia, roles y disponibilidad completa de los dos participantes. */
    private void validateProposal(
        long student,
        long other,
        long subject,
        long mentor,
        LocalDateTime date,
        int duration
    ) {
        long learner = mentor == student ? other : student;
        if (
            materiaDAO.validarRoles(mentor, learner, subject).isEmpty()
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "La materia debe coincidir entre quien enseña y quien necesita aprenderla."
        );
        LocalDateTime end = date.plusMinutes(duration);
        if (!date.toLocalDate().equals(end.toLocalDate())) throw new ResponseStatusException(
            BAD_REQUEST,
            "La sesión debe empezar y terminar el mismo día."
        );
        for (long participant : new long[] { Math.min(student, other), Math.max(student, other) }) {
            estudianteDAO.bloquear(participant);
            LocalTime covered = date.toLocalTime();
            for (var slot : disponibilidadDAO.bloquesDelDia(
                participant,
                date.getDayOfWeek().getValue()
            )) {
                LocalTime start = ((java.sql.Time) slot.get("hora_inicio")).toLocalTime(),
                    finish = ((java.sql.Time) slot.get("hora_fin")).toLocalTime();
                if (start.isAfter(covered)) break;
                if (finish.isAfter(covered)) covered = finish;
            }
            if (covered.isBefore(end.toLocalTime())) throw new ResponseStatusException(
                BAD_REQUEST,
                "La sesión completa debe estar dentro de la disponibilidad de ambos estudiantes."
            );
            if (
                !sesionDAO.buscarCruces(participant, end, date).isEmpty()
            ) throw new ResponseStatusException(
                CONFLICT,
                "El horario se cruza con otra sesión de uno de los estudiantes."
            );
        }
    }

    /** Crea la propuesta solo si los participantes y el horario son válidos. */
    @Transactional
    public long propose(long student, Map<String, Object> input) {
        if (!sesionDAO.tieneRoles()) throw new ResponseStatusException(
            CONFLICT,
            "Faltan los roles mentor_id y aprendiz_id en el esquema."
        );
        long other = number(input, "receptor_id"),
            subject = number(input, "materia_id");
        if (other == student) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona otro estudiante."
        );
        estudianteDAO.perfil(other);
        long mentor = number(input, "mentor_id");
        if (mentor != student && mentor != other) throw new ResponseStatusException(
            BAD_REQUEST,
            "El mentor debe participar en la sesión."
        );
        long requestedDuration = number(input, "duracion_programada_min");
        if (requestedDuration < 1 || requestedDuration > 480) throw new ResponseStatusException(
            BAD_REQUEST,
            "La sesión puede durar hasta 480 minutos."
        );
        int duration = (int) requestedDuration;
        LocalDateTime date = LocalDateTime.parse(text(input, "fecha_hora", 30));
        if (!date.isAfter(now())) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona una fecha futura."
        );
        validateProposal(student, other, subject, mentor, date, duration);
        long session = sesionDAO.crearPropuesta(
            student,
            other,
            subject,
            date,
            duration,
            mentor,
            mentor == student ? other : student
        );
        notificacionDAO.crear(
            other,
            "Nueva propuesta de sesión",
            (String) (estudianteDAO.perfil(student).get("nombre") + " te ha propuesto una sesión."),
            "PROPUESTA_SESION",
            session
        );
        return session;
    }

    /** Aplica las acciones permitidas según el estado y el participante. */
    @Transactional
    public Map<String, Object> sessionAction(
        long student,
        long session,
        String action,
        Map<String, Object> input
    ) {
        var row = sesionDAO.buscarParticipante(session, student);
        String state = row.get("estado").toString();
        if (action.equals("aceptar")) {
            if (
                id(row, "receptor_id") != student || !state.equals("PROPUESTA")
            ) throw new ResponseStatusException(CONFLICT, "La propuesta no se puede aceptar.");
            sesionDAO.confirmar(session);
            notificacionDAO.crear(
                id(row, "proponente_id"),
                "Sesión confirmada",
                "Tu compañero aceptó la sesión.",
                "SISTEMA",
                session
            );
        } else if (action.equals("cancelar")) {
            if (
                !Set.of("PROPUESTA", "CONFIRMADA").contains(state)
            ) throw new ResponseStatusException(CONFLICT, "Esta sesión ya no se puede cancelar.");
            String reason = text(input, "motivo", 1000);
            sesionDAO.cancelar(session);
            notificacionDAO.crear(
                id(row, "proponente_id") == student
                    ? id(row, "receptor_id")
                    : id(row, "proponente_id"),
                "Sesión cancelada",
                "Tu compañero canceló la sesión. Motivo: " + reason,
                "SISTEMA",
                session
            );
        } else if (action.equals("iniciar")) {
            if (
                !Set.of("CONFIRMADA", "EN_CURSO").contains(state)
            ) throw new ResponseStatusException(
                CONFLICT,
                "Solo se puede entrar a una sesión confirmada o en curso."
            );
            if (
                !Objects.toString(row.get("enlace_videollamada"), "").startsWith("https://")
            ) throw new ResponseStatusException(
                CONFLICT,
                "Añade el enlace de la reunión antes de iniciarla."
            );
            if (state.equals("EN_CURSO")) return row;
            if (
                ((java.sql.Timestamp) row.get("fecha_hora")).toLocalDateTime().isAfter(now())
            ) throw new ResponseStatusException(CONFLICT, "Todavía no es la hora de inicio.");
            sesionDAO.iniciar(now(), session);
        } else if (action.equals("finalizar")) {
            if (!state.equals("EN_CURSO")) throw new ResponseStatusException(
                CONFLICT,
                "La sesión no está en curso."
            );
            String column =
                id(row, "proponente_id") == student ? "marca_proponente" : "marca_receptor";
            sesionDAO.marcarFinalizacion(column, now(), session);
            var marked = sesionDAO.buscarParticipante(session, student);
            if (
                Boolean.TRUE.equals(marked.get("marca_proponente")) &&
                Boolean.TRUE.equals(marked.get("marca_receptor"))
            ) {
                long minutes = Duration.between(
                    ((java.sql.Timestamp) marked.get("inicio_real")).toLocalDateTime(),
                    ((java.sql.Timestamp) marked.get("fin_real")).toLocalDateTime()
                ).toMinutes();
                sesionDAO.completar(Math.max(0, minutes), session);
                progresoService.award(id(marked, "mentor_id"));
                progresoService.refreshStats(id(marked, "mentor_id"));
                progresoService.refreshStats(id(marked, "aprendiz_id"));
            }
        } else if (action.equals("enlace")) {
            if (
                !Set.of("PROPUESTA", "CONFIRMADA").contains(state)
            ) throw new ResponseStatusException(
                CONFLICT,
                "No se puede cambiar el enlace de esta sesión."
            );
            String link = text(input, "enlace", 255);
            java.net.URI uri = java.net.URI.create(link);
            if (
                !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            ) throw new ResponseStatusException(BAD_REQUEST, "Introduce un enlace HTTPS válido.");
            sesionDAO.guardarEnlace(link, session);
        } else throw new ResponseStatusException(NOT_FOUND, "Acción no encontrada.");
        return sesionDAO.buscarParticipante(session, student);
    }

    /** Permite calificar una sesión completada dentro del plazo establecido. */
    @Transactional
    public void rate(long student, long session, Map<String, Object> input) {
        var row = sesionDAO.buscarParticipante(session, student);
        if (!"COMPLETADA".equals(row.get("estado"))) throw new ResponseStatusException(
            CONFLICT,
            "La sesión debe estar completada."
        );
        if (
            row.get("fin_real") == null ||
            ((java.sql.Timestamp) row.get("fin_real")).toLocalDateTime().plusDays(7).isBefore(now())
        ) throw new ResponseStatusException(CONFLICT, "El plazo para calificar ha terminado.");
        long requestedStars = number(input, "estrellas");
        if (requestedStars > 5) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona de 1 a 5 estrellas."
        );
        int stars = (int) requestedStars;
        String comment = Objects.toString(input.get("texto"), "").trim();
        if (comment.length() > 500) throw new ResponseStatusException(
            BAD_REQUEST,
            "El comentario admite hasta 500 caracteres."
        );
        long rating = calificacionDAO.guardar(session, student, stars);
        calificacionDAO.eliminarComentario(rating);
        if (!comment.isBlank()) calificacionDAO.guardarComentario(rating, comment);
        calificacionDAO.marcarSesion(session);
        progresoService.refreshStats(
            id(row, "proponente_id") == student ? id(row, "receptor_id") : id(row, "proponente_id")
        );
    }

    public Object sessions(long student) {
        return sesionDAO.listarDelEstudiante(student);
    }
}
