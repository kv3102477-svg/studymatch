package com.studymatch.controller;

import com.studymatch.service.MateriaService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Materia {

    private final MateriaService materiaService;

    public C_Materia(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/materias")
    public Object subjects(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "1") int pagina
    ) {
        return materiaService.subjects(q, pagina);
    }

    @GetMapping("/perfil/materias")
    public Object mine(HttpSession s) {
        return materiaService.mine(student(s));
    }

    @PostMapping("/perfil/materias")
    public Object subject(HttpSession s, @RequestBody Map<String, Object> input) {
        materiaService.subject(student(s), input);
        return Map.of("mensaje", "Materia guardada.");
    }

    @DeleteMapping("/perfil/materias/{id}")
    public Object removeSubject(HttpSession s, @PathVariable long id) {
        return materiaService.removeSubject(student(s), id);
    }

    @GetMapping("/estudiantes/{id}/coincidencias")
    public Object matches(HttpSession s, @PathVariable long id) {
        return materiaService.matchingSubjects(student(s), id);
    }
}
