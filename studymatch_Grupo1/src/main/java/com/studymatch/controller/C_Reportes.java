package com.studymatch.controller;
import com.studymatch.service.*;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reportes")
public class C_Reportes {
    private final ReportesService reportes;private final EstudianteService estudiantes;private final PdfService pdf;
    @Value("${studymatch.reportes.delegados:}") private String delegados="";
    public C_Reportes(ReportesService reportes,EstudianteService estudiantes,PdfService pdf){this.reportes=reportes;this.estudiantes=estudiantes;this.pdf=pdf;}
    private boolean autorizado(HttpSession sesion){
        String correo=estudiantes.perfil((Long)sesion.getAttribute("studentId")).get("correo").toString();
        return Arrays.stream(delegados.split(",")).map(String::trim).anyMatch(correo::equalsIgnoreCase);
    }
    private LocalDate inicio(LocalDate fecha){return fecha==null?LocalDate.now().withDayOfMonth(1):fecha;}
    private LocalDate fin(LocalDate fecha){return fecha==null?LocalDate.now():fecha;}
    @GetMapping("/permiso") public Object permiso(HttpSession s){return Map.of("autorizado",autorizado(s));}
    @GetMapping("/asesorias") public Object asesorias(HttpSession s,@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,@RequestParam(required=false) Long materia){return reportes.asesorias(autorizado(s),inicio(desde),fin(hasta),materia);}
    @GetMapping("/emparejamientos") public Object emparejamientos(HttpSession s,@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta){return reportes.emparejamientos(autorizado(s),inicio(desde),fin(hasta));}
    @GetMapping("/satisfaccion") public Object satisfaccion(HttpSession s,@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta){return reportes.satisfaccion(autorizado(s),inicio(desde),fin(hasta));}
    @GetMapping("/retencion") public Object retencion(HttpSession s,@RequestParam LocalDate inicio1,@RequestParam LocalDate fin1,@RequestParam LocalDate inicio2,@RequestParam LocalDate fin2){return reportes.retencion(autorizado(s),inicio1,fin1,inicio2,fin2);}
    @GetMapping("/asesorias/pdf") public ResponseEntity<byte[]> descargar(HttpSession s,@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,@RequestParam(required=false) Long materia){
        var datos=reportes.asesorias(autorizado(s),inicio(desde),fin(hasta),materia);
        List<String> lineas=new ArrayList<>();lineas.add("Asesorías: "+datos.get("asesorias"));lineas.add("Horas: "+datos.get("horas"));lineas.add("Participantes: "+datos.get("participantes"));
        for(var fila:(List<Map<String,Object>>)datos.get("detalle"))lineas.add(fila.get("fecha_hora")+" | "+fila.get("materia")+" | "+fila.get("proponente")+" / "+fila.get("receptor")+" | "+fila.get("duracion_real_min")+" min");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=Reporte_StudyMatch.pdf").body(pdf.documento("Reporte de mentorías StudyMatch",lineas));
    }
}
