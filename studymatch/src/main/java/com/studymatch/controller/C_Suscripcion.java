package com.studymatch.controller;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.studymatch.service.SuscripcionService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Recibe peticiones HTTP y delega el trabajo a los servicios. */
@RestController
@RequestMapping("/api")
public class C_Suscripcion {

    private final SuscripcionService suscripcionService;

    public C_Suscripcion(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    private long student(HttpSession session) {
        return (Long) session.getAttribute("studentId");
    }

    @GetMapping("/planes")
    public Object plans() {
        return suscripcionService.plans();
    }

    @GetMapping("/suscripciones")
    public Object subscriptions(HttpSession s) {
        return suscripcionService.subscriptions(student(s));
    }

    @PostMapping("/suscripciones/prueba")
    public Object trial(HttpSession s, @RequestBody Map<String, Object> input) {
        suscripcionService.trial(student(s), input);
        return Map.of("mensaje", "Prueba gratuita activada.");
    }

    @PostMapping("/suscripciones/{id}/cancelar")
    public Object cancel(HttpSession s, @PathVariable long id) {
        return suscripcionService.cancel(student(s), id);
    }

    @GetMapping("/pagos")
    public Object payments(HttpSession s) {
        return suscripcionService.payments(student(s));
    }

    @PostMapping("/pagos")
    public Object pay() {
        throw new ResponseStatusException(
            SERVICE_UNAVAILABLE,
            "Los pagos todavía no están habilitados. No se realizó ningún cobro."
        );
    }

    @PostMapping("/pagos/prueba")
    public Object paymentDemo(HttpSession s, @RequestBody Map<String, Object> input) {
        return suscripcionService.paymentDemo(student(s), input);
    }

    @GetMapping("/suscripciones/acceso")
    public Object acceso(HttpSession s) { return suscripcionService.acceso(student(s)); }

    @GetMapping("/pagos/{id}/comprobante")
    public Object receipt(HttpSession s, @PathVariable long id) {
        return suscripcionService.receipt(student(s), id);
    }

    @GetMapping("/pagos/{id}/comprobante/pdf")
    public org.springframework.http.ResponseEntity<byte[]> comprobantePdf(HttpSession s,@PathVariable long id){
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .header("Content-Disposition","attachment; filename=Comprobante_StudyMatch.pdf").body(suscripcionService.comprobantePdf(student(s),id));
    }
}
