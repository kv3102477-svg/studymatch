package com.studymatch.controller;

import com.studymatch.model.SolicitudSoporte;
import com.studymatch.service.SupportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class C_Soporte {

    private final SupportService service;

    public C_Soporte(SupportService service) {
        this.service = service;
    }

    @PostMapping("/soporte")
    public Object support(HttpSession session, @RequestBody SolicitudSoporte input) {
        return service.create((Long) session.getAttribute("studentId"), input, false);
    }

    @PostMapping("/reportes-usuario")
    public Object report(HttpSession session, @RequestBody SolicitudSoporte input) {
        return service.create((Long) session.getAttribute("studentId"), input, true);
    }
}
