package com.studymatch.controller;
import com.studymatch.service.CertificadoService;
import org.springframework.web.bind.annotation.*;
/** El código aleatorio compartido permite verificar la firma sin exponer el archivo privado. */
@RestController
public class C_VerificarCertificado {
    private final CertificadoService servicio;
    public C_VerificarCertificado(CertificadoService servicio){this.servicio=servicio;}
    @GetMapping("/certificados/verificar/{codigo}")public Object verificar(@PathVariable String codigo){return servicio.verificarCodigo(java.util.UUID.fromString(codigo).toString());}
}
