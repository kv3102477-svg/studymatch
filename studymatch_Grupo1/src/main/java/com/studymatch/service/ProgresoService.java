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
    private final PdfService pdf;

    public ProgresoService(
        EstudianteDAO estudianteDAO,
        InsigniaDAO insigniaDAO,
        SesionDAO sesionDAO,
        PdfService pdf
    ) {
        this.estudianteDAO = estudianteDAO;
        this.insigniaDAO = insigniaDAO;
        this.sesionDAO = sesionDAO;
        this.pdf=pdf;
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
        var horas=sesionDAO.horasAcumuladas(student);
        if (Boolean.TRUE.equals(horas.get("en_revision")) || ((Number)horas.get("horas_ensenadas")).doubleValue()<0)
            throw new ResponseStatusException(CONFLICT,"Las horas están en revisión. No se pueden otorgar insignias.");
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
        var resultado = new java.util.ArrayList<Map<String,Object>>();
        double anterior=Double.NaN;
        int puesto=0;
        for(var fila : insigniaDAO.ranking()) {
            var copia=new java.util.LinkedHashMap<String,Object>(fila);
            double horas=((Number)fila.get("horas_ensenadas")).doubleValue();
            if(Double.compare(horas,anterior)!=0) puesto=resultado.size()+1;
            copia.put("posicion",puesto);
            resultado.add(copia);
            anterior=horas;
        }
        return resultado;
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

    public byte[] constanciaPdf(long estudiante){
        var h=hours(estudiante);
        double ensenadas=((Number)h.get("horas_ensenadas")).doubleValue();
        double aprendidas=((Number)h.get("horas_aprendidas")).doubleValue();
        if(Boolean.TRUE.equals(h.get("en_revision"))||ensenadas<0||aprendidas<0)
            throw new ResponseStatusException(CONFLICT,"Tus horas están en revisión. Podrás descargar tu constancia cuando se validen");
        if(ensenadas+aprendidas<=0)throw new ResponseStatusException(CONFLICT,"Aún no tienes horas acumuladas para emitir una constancia.");
        var perfil=estudianteDAO.perfil(estudiante);
        return pdf.documento("Constancia de horas de mentoría StudyMatch",List.of("Estudiante: "+perfil.get("nombre"),
            "Carrera: "+perfil.get("carrera"),"Horas enseñadas: "+ensenadas,"Horas aprendidas: "+aprendidas,
            "Fecha: "+LocalDate.now(ZoneId.of("America/Bogota")),"Documento de participación en la plataforma de estudiantes."));
    }

    public byte[] reportePersonalPdf(long estudiante,Long materia){
        var lineas=new java.util.ArrayList<String>();
        lineas.add("Estudiante: "+estudianteDAO.perfil(estudiante).get("nombre"));
        for(var sesion:sesionDAO.listarDelEstudiante(estudiante)){
            if(!"COMPLETADA".equals(sesion.get("estado")))continue;
            if(materia!=null&&((Number)sesion.get("materia_id")).longValue()!=materia)continue;
            lineas.add(sesion.get("fecha_hora")+" | "+sesion.get("materia")+" | "+sesion.get("companero")+" | "+sesion.get("duracion_real_min")+" min");
        }
        return pdf.documento("Reporte personal de mentorías StudyMatch",lineas);
    }
}
