package com.studymatch.controller;

import com.studymatch.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Notificacion {

    private final NotificacionService notificacionService;

    public C_Notificacion(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/notificaciones")
    public Object notifications(HttpSession s) {
        return notificacionService.notifications(student(s));
    }

    @PostMapping("/notificaciones/{id}/leer")
    public Object read(HttpSession s, @PathVariable long id) {
        return notificacionService.read(student(s), id);
    }
}
