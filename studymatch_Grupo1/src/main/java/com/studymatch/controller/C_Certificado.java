package com.studymatch.controller;

import com.studymatch.service.CertificadoService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/certificados")
public class C_Certificado {
    private final CertificadoService certificados;
    public C_Certificado(CertificadoService certificados){this.certificados=certificados;}
    @GetMapping public Object listar(HttpSession s){return certificados.listar((Long)s.getAttribute("studentId"));}
    @PostMapping("/{ciclo}") public Object emitir(HttpSession s,@PathVariable long ciclo){return certificados.emitir((Long)s.getAttribute("studentId"),ciclo);}
    @GetMapping("/{codigo}/pdf") public ResponseEntity<byte[]> descargar(HttpSession s,@PathVariable String codigo){
        byte[] archivo=certificados.descargar((Long)s.getAttribute("studentId"),codigo);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=Certificado_StudyMatch_"+UUID.fromString(codigo)+".pdf").body(archivo);
    }
}
