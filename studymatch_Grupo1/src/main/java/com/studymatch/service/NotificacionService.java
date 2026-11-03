package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;

import com.studymatch.dao.NotificacionDAO;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta avisos del estudiante y permite marcarlos como leídos. */
@Service
public class NotificacionService {

    private final NotificacionDAO notificacionDAO;
    private final com.studymatch.dao.EstudianteDAO estudiantes;
    private final RecoveryService correo;

    public NotificacionService(NotificacionDAO notificacionDAO,com.studymatch.dao.EstudianteDAO estudiantes,RecoveryService correo) {
        this.notificacionDAO = notificacionDAO;
        this.estudiantes=estudiantes;this.correo=correo;
    }

    public Object notifications(long student) {
        return notificacionDAO.listar(student);
    }

    @Transactional
    public Object read(long student, long id) {
        notificacionDAO.marcarLeida(id, student);
        return Map.of("mensaje", "Notificación leída.");
    }

    @org.springframework.beans.factory.annotation.Value("${studymatch.jobs.enabled:true}")
    private boolean jobsEnabled=true;

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=30000,initialDelay=60000)
    @Transactional
    public void procesarCola(){if(jobsEnabled)reintentar();}

    /** Procesa avisos ya guardados; un fallo de correo no deshace la operación original. */
    @Transactional
    public void reintentar(){
        for(var aviso:notificacionDAO.pendientes()){
            long id=((Number)aviso.get("id")).longValue();
            try{
                var receptor=estudiantes.perfil(((Number)aviso.get("receptor_id")).longValue());
                correo.sendAviso(receptor.get("correo").toString(),aviso.get("titulo").toString(),aviso.get("mensaje").toString());
                notificacionDAO.marcarEnviado(id);
            }catch(org.springframework.mail.MailException | org.springframework.web.server.ResponseStatusException error){
                int intentos=((Number)aviso.getOrDefault("intentos",0)).intValue();
                long espera=intentos==0?1:intentos==1?5:15;
                notificacionDAO.registrarFallo(id,error.getClass().getSimpleName(),
                    java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")).plusMinutes(espera));
            }
        }
    }
}
