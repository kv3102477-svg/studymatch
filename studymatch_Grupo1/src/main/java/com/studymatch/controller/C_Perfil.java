package com.studymatch.controller;

import com.studymatch.service.EstudianteService;
import com.studymatch.service.ProgresoService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Perfil {

    private final EstudianteService estudianteService;
    private final ProgresoService progresoService;

    public C_Perfil(EstudianteService estudianteService, ProgresoService progresoService) {
        this.estudianteService = estudianteService;
        this.progresoService = progresoService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @PutMapping("/perfil")
    public Object profile(HttpSession s, @RequestBody Map<String, Object> input) {
        return estudianteService.updateProfile(student(s), input);
    }

    @PutMapping("/cuenta/contrasena")
    public Object password(HttpSession s, @RequestBody Map<String, String> input) {
        estudianteService.changePassword(student(s), input);
        return Map.of("mensaje", "Contraseña actualizada.");
    }

    @DeleteMapping("/cuenta")
    public Object delete(HttpSession s) {
        Object result = estudianteService.delete(student(s));
        s.invalidate();
        return result;
    }

    @GetMapping("/perfil/constancia")
    public Object certificate(HttpSession s) {
        return progresoService.certificate(student(s));
    }

    @GetMapping("/perfil/horas")
    public Object hours(HttpSession s) {
        return progresoService.hours(student(s));
    }

    @GetMapping("/perfil/resumen")
    public Object summary(HttpSession s) {
        return progresoService.summary(student(s));
    }

    @GetMapping("/insignias")
    public Object badges(HttpSession s) {
        return progresoService.badges(student(s));
    }

    @GetMapping("/ranking")
    public Object ranking() {
        return progresoService.ranking();
    }
}
