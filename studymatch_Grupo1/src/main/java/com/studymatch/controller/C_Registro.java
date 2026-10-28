package com.studymatch.controller;

import com.studymatch.model.Registro;
import com.studymatch.service.AuthSessionService;
import com.studymatch.service.CatalogoService;
import com.studymatch.service.EstudianteService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class C_Registro {

    private final EstudianteService service;
    private final CatalogoService catalogoService;

    private final AuthSessionService sessions;

    public C_Registro(
        EstudianteService service,
        AuthSessionService sessions,
        CatalogoService catalogoService
    ) {
        this.service = service;
        this.catalogoService = catalogoService;
        this.sessions = sessions;
    }

    @GetMapping("/catalogos")
    public Map<String, Object> catalogs() {
        return catalogoService.catalogs();
    }

    @PostMapping("/registro")
    public Map<String, Object> register(@RequestBody Registro input, HttpServletRequest request) {
        return sessions.authenticate(service.register(input), request);
    }
}
