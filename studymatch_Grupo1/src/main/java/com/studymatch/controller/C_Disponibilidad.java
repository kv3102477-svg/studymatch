package com.studymatch.controller;

import com.studymatch.service.DisponibilidadService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Disponibilidad {

    private final DisponibilidadService disponibilidadService;

    public C_Disponibilidad(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/perfil/horario")
    public Object availability(HttpSession s) {
        return disponibilidadService.availability(student(s));
    }

    @PutMapping("/perfil/horario")
    public Object saveAvailability(HttpSession s, @RequestBody List<Map<String, Object>> input) {
        disponibilidadService.replaceAvailability(student(s), input);
        return Map.of("mensaje", "Disponibilidad guardada.");
    }
}
