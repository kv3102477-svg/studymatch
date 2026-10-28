package com.studymatch.service;
import com.studymatch.dao.CertificadoDAO;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
/** Revisa el cierre del ciclo activo; el fallo de un estudiante no impide procesar a los demás. */
@Service
public class CierreCicloService {
    private final CertificadoDAO datos;private final CertificadoService certificados;
    @Value("${studymatch.jobs.enabled:true}")private boolean habilitado=true;
    public CierreCicloService(CertificadoDAO datos,CertificadoService certificados){this.datos=datos;this.certificados=certificados;}
    @Scheduled(fixedDelay=3600000,initialDelay=60000)
    public void revisar(){
        if(!habilitado)return;
        for(var ciclo:datos.ciclosCerradosActivos()){
            long id=((Number)ciclo.get("id")).longValue();
            for(var estudiante:datos.candidatos(id)){
                try{certificados.emitir(((Number)estudiante.get("estudiante_id")).longValue(),id);}
                catch(org.springframework.web.server.ResponseStatusException invalidos){
                    org.slf4j.LoggerFactory.getLogger(CierreCicloService.class).info("Certificación omitida: {}",invalidos.getReason());
                }
            }
        }
    }
}
