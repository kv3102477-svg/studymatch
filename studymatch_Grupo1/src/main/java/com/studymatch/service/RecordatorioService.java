package com.studymatch.service;

import com.studymatch.dao.CalificacionDAO;
import com.studymatch.dao.NotificacionDAO;
import com.studymatch.dao.SuscripcionDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Genera recordatorios y actualiza vencimientos de forma periódica. */
@Service
public class RecordatorioService {

    private final CalificacionDAO calificacionDAO;
    private final NotificacionDAO notificacionDAO;
    private final SuscripcionDAO suscripcionDAO;

    @org.springframework.beans.factory.annotation.Value("${studymatch.jobs.enabled:true}")
    private boolean jobsEnabled;

    public RecordatorioService(
        CalificacionDAO calificacionDAO,
        NotificacionDAO notificacionDAO,
        SuscripcionDAO suscripcionDAO
    ) {
        this.calificacionDAO = calificacionDAO;
        this.notificacionDAO = notificacionDAO;
        this.suscripcionDAO = suscripcionDAO;
    }

    /** Ejecuta periódicamente la actualización de avisos si está habilitada. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void reminders() {
        if (!jobsEnabled) return;
        processReminders();
    }

    /** Evita ejecuciones simultáneas y genera los avisos pendientes. */
    @Transactional
    public void processReminders() {
        if (!Boolean.TRUE.equals(notificacionDAO.bloquearRecordatorios().get("acquired"))) return;
        suscripcionDAO.expirarVencidas();
        calificacionDAO.expirarPlazo();
        notificacionDAO.recordarSesiones();
        notificacionDAO.recordarVencimientos();
    }
}
