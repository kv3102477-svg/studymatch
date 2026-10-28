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

    public NotificacionService(NotificacionDAO notificacionDAO) {
        this.notificacionDAO = notificacionDAO;
    }

    public Object notifications(long student) {
        return notificacionDAO.listar(student);
    }

    @Transactional
    public Object read(long student, long id) {
        notificacionDAO.marcarLeida(id, student);
        return Map.of("mensaje", "Notificación leída.");
    }
}
