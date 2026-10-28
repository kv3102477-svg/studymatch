package com.studymatch.controller;

import com.studymatch.service.SesionService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Sesion {

    private final SesionService sesionService;

    public C_Sesion(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/sesiones")
    public Object sessions(HttpSession s) {
        return sesionService.sessions(student(s));
    }

    @PostMapping("/sesiones")
    public Object propose(HttpSession s, @RequestBody Map<String, Object> input) {
        return Map.of("id", sesionService.propose(student(s), input));
    }

    @PostMapping("/sesiones/{id}/{action}")
    public Object action(
        HttpSession s,
        @PathVariable long id,
        @PathVariable String action,
        @RequestBody(required = false) Map<String, Object> input
    ) {
        return sesionService.sessionAction(
            student(s),
            id,
            action,
            input == null ? Map.of() : input
        );
    }

    @PostMapping("/sesiones/{id}/calificacion")
    public Object rate(
        HttpSession s,
        @PathVariable long id,
        @RequestBody Map<String, Object> input
    ) {
        sesionService.rate(student(s), id, input);
        return Map.of(
            "mensaje",
            "Calificación guardada. El comentario queda pendiente de moderación."
        );
    }
}
