package com.studymatch.controller;

import com.studymatch.service.BusquedaService;
import com.studymatch.service.CatalogoService;
import com.studymatch.service.EstudianteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Estudiante {

    private final BusquedaService busquedaService;
    private final CatalogoService catalogoService;
    private final EstudianteService estudianteService;

    public C_Estudiante(
        BusquedaService busquedaService,
        CatalogoService catalogoService,
        EstudianteService estudianteService
    ) {
        this.busquedaService = busquedaService;
        this.catalogoService = catalogoService;
        this.estudianteService = estudianteService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/ciclo")
    public Object cycle() {
        return catalogoService.cycle();
    }

    @GetMapping("/estudiantes")
    public Object search(
        HttpSession s,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "false") boolean recomendados,
        @RequestParam(defaultValue = "") String curso,
        @RequestParam(required = false) Integer dia,
        @RequestParam(defaultValue = "") String carrera,
        @RequestParam(defaultValue = "") String rol
    ) {
        return busquedaService.search(student(s), q, recomendados, curso.trim(), dia, carrera, rol);
    }

    @GetMapping("/estudiantes/{id}")
    public Object other(@PathVariable long id) {
        return estudianteService.other(id);
    }
}
