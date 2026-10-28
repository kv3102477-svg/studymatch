package com.studymatch.service;

import com.studymatch.dao.EstudianteDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Mantiene un único flujo de verificación y renovación de sesión para los controladores. */
@Service
public class AuthSessionService {

    private final EstudianteDAO estudianteDAO;
    private final EmailVerificationService verification;

    public AuthSessionService(EstudianteDAO estudianteDAO, EmailVerificationService verification) {
        this.estudianteDAO = estudianteDAO;
        this.verification = verification;
    }

    public Map<String, Object> authenticate(
        Map<String, Object> profile,
        HttpServletRequest request
    ) {
        if (Boolean.TRUE.equals(profile.get("correo_verificado"))) return establish(
            profile,
            request
        );
        request.getSession().invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(1800);
        session.setAttribute("csrf", UUID.randomUUID().toString());
        verification.begin(session, profile);
        return verification.status(session);
    }

    public Map<String, Object> establish(Map<String, Object> profile, HttpServletRequest request) {
        Object claveHash = estudianteDAO.claveGuardada(((Number) profile.get("id")).longValue()).get("contrasena_hash");
        request.getSession().invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(1800);
        session.setAttribute("studentId", ((Number) profile.get("id")).longValue());
        session.setAttribute("authVersion", claveHash);
        String csrf = UUID.randomUUID().toString();
        session.setAttribute("csrf", csrf);
        return Map.of("estudiante", profile, "csrf", csrf);
    }
}
