package com.studymatch.service;
import com.studymatch.dao.*;
import java.time.*;
import java.util.*;
import java.security.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
/** Emite un documento de participación del sistema, no una certificación oficial de la universidad. */
@Service
public class CertificadoService {
    private final CertificadoDAO datos;private final PdfService pdf;private final NotificacionDAO avisos;private final Clock clock;private final FirmaService firma;
    @org.springframework.beans.factory.annotation.Value("${APP_URL:http://localhost:8080}") private String urlBase="http://localhost:8080";
    @Autowired
    public CertificadoService(CertificadoDAO datos,PdfService pdf,NotificacionDAO avisos,FirmaService firma){this(datos,pdf,avisos,Clock.system(ZoneId.of("America/Bogota")),firma);}
    public CertificadoService(CertificadoDAO datos,PdfService pdf,NotificacionDAO avisos,Clock clock,FirmaService firma){this.datos=datos;this.pdf=pdf;this.avisos=avisos;this.clock=clock;this.firma=firma;}
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Map<String,Object> emitir(long estudiante,long ciclo){
        var periodo=datos.ciclo(ciclo);
        var existente=datos.buscar(estudiante,ciclo);if(!existente.isEmpty())return existente.get(0);
        LocalDate cierre=((java.sql.Date)periodo.get("fecha_fin")).toLocalDate();
        if(!cierre.isBefore(LocalDate.now(clock)))throw new ResponseStatusException(CONFLICT,"El ciclo todavía no ha terminado.");
        var candidato=datos.candidatos(ciclo).stream().filter(f->((Number)f.get("estudiante_id")).longValue()==estudiante).findFirst().orElse(null);
        if(candidato==null||((Number)candidato.get("horas")).doubleValue()<5){
            String mensaje="No alcanzaste las horas mínimas para el certificado de este ciclo";
            if(!datos.avisoEmitido(estudiante,ciclo,mensaje))avisos.crear(estudiante,"Certificado",mensaje,"SISTEMA",ciclo);throw new ResponseStatusException(CONFLICT,mensaje);
        }
        String nombre=Objects.toString(candidato.get("nombre"),"").trim();
        if(nombre.isEmpty())throw new ResponseStatusException(CONFLICT,"Completa los datos del estudiante antes de emitir el certificado.");
        double horas=((Number)candidato.get("horas")).doubleValue();
        if(!Double.isFinite(horas)||horas<0)throw new ResponseStatusException(CONFLICT,"Las horas están en revisión.");
        String codigo=UUID.randomUUID().toString();
        byte[] contenido=pdf.certificado("Constancia de participación StudyMatch",List.of("Estudiante: "+nombre,"Carrera: "+candidato.get("carrera"),"Ciclo: "+periodo.get("nombre"),"Horas registradas: "+horas,"Código de verificación: "+codigo,"Emitido: "+LocalDate.now(clock),"Documento del sistema de mentorías entre estudiantes."),urlBase+"/certificados/verificar/"+codigo);
        long id=datos.guardar(estudiante,ciclo,codigo,horas,contenido,huella(contenido));
        datos.registrarFirma(id,firma.firmar(contenido),firma.clavePublica());
        avisos.crear(estudiante,"Certificado disponible","Tu constancia de participación está disponible.","SISTEMA",id);
        return Map.of("id",id,"codigo",codigo,"horas",horas);
    }
    public byte[] descargar(long estudiante,String codigo){
        var documento=datos.buscarCodigo(codigo);
        if(((Number)documento.get("estudiante_id")).longValue()!=estudiante)throw new ResponseStatusException(FORBIDDEN,"El certificado no pertenece a tu cuenta.");
        byte[] contenido=(byte[])documento.get("contenido");
        if(contenido==null||!huella(contenido).equals(documento.get("huella"))||!firma.verificar(contenido,(byte[])documento.get("firma"),(byte[])documento.get("clave_publica")))throw new ResponseStatusException(CONFLICT,"No se pudo validar la integridad del certificado.");
        return contenido;
    }
    private String huella(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
    public Object listar(long estudiante){return datos.listar(estudiante);}

    public Map<String,Object> verificarCodigo(String codigo){
        var documento=datos.buscarCodigo(codigo);
        byte[] contenido=(byte[])documento.get("contenido");
        boolean valido=contenido!=null&&huella(contenido).equals(documento.get("huella"))
            &&firma.verificar(contenido,(byte[])documento.get("firma"),(byte[])documento.get("clave_publica"));
        return Map.of("codigo",codigo,"valido",valido,"horas",documento.get("horas"),"emisor","StudyMatch");
    }
}
