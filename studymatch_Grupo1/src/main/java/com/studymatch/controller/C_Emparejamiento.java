package com.studymatch.controller;
import com.studymatch.service.EmparejamientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/emparejamientos")
public class C_Emparejamiento {
    private final EmparejamientoService servicio;
    public C_Emparejamiento(EmparejamientoService servicio){this.servicio=servicio;}
    @PostMapping("/buscar") public Object buscar(HttpSession sesion){return servicio.buscar((Long)sesion.getAttribute("studentId"));}
}
