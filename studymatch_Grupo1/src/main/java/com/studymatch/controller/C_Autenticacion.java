package com.studymatch.controller;

import com.studymatch.service.AuthSessionService;
import com.studymatch.service.EstudianteService;
import com.studymatch.service.RecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class C_Autenticacion {

    private final EstudianteService service;

    private final RecoveryService recovery;
    private final AuthSessionService sessions;

    public C_Autenticacion(
        EstudianteService service,
        RecoveryService recovery,
        AuthSessionService sessions
    ) {
        this.service = service;
        this.recovery = recovery;
        this.sessions = sessions;
    }

    @PostMapping("/recuperar")
    public Object recover(@RequestBody Map<String, String> input) {
        recovery.request(input.get("correo"));
        return Map.of(
            "mensaje",
            "Si existe una cuenta con ese correo, recibirás un enlace de recuperación."
        );
    }

    @PostMapping("/restablecer")
    public Object reset(@RequestBody Map<String, String> input) {
        recovery.reset(input.get("token"), input.get("contrasena"));
        return Map.of("mensaje", "Contraseña actualizada. Inicia sesión.");
    }

    @GetMapping("/csrf")
    public Map<String, Object> csrf(HttpSession session) {
        if (session.getAttribute("csrf") == null) session.setAttribute(
            "csrf",
            UUID.randomUUID().toString()
        );
        return Map.of("token", session.getAttribute("csrf"));
    }

    @PostMapping("/login")
    public Map<String, Object> login(
        @RequestBody Map<String, String> input,
        HttpServletRequest request
    ) {
        return sessions.authenticate(service.login(input), request);
    }

    @GetMapping("/yo")
    public Map<String, Object> me(HttpSession session) {
        return service.perfil((Long) session.getAttribute("studentId"));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("mensaje", "Sesión cerrada.");
    }
}
