package com.studymatch.service;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.studymatch.dao.EstudianteDAO;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Gestiona el código temporal, su vencimiento y los intentos de verificación. */
@Service
public class EmailVerificationService {

    private final EstudianteDAO estudianteDAO;
    private final RecoveryService mail;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EstudianteDAO estudianteDAO, RecoveryService mail) {
        this.estudianteDAO = estudianteDAO;
        this.mail = mail;
    }

    public void begin(HttpSession session, Map<String, Object> profile) {
        long id = ((Number) profile.get("id")).longValue();
        session.setAttribute("pendingStudentId", id);
        session.setAttribute("pendingAuth", estudianteDAO.claveGuardada(id).get("contrasena_hash"));
    }

    private long identity(HttpSession session) {
        if (
            !(session.getAttribute("pendingStudentId") instanceof Long id)
        ) throw new ResponseStatusException(
            UNAUTHORIZED,
            "Inicia sesión o regístrate para verificar tu correo."
        );
        var rows = estudianteDAO.validarIdentidad(id, (String) session.getAttribute("pendingAuth"));
        if (rows.isEmpty()) throw new ResponseStatusException(
            UNAUTHORIZED,
            "La solicitud venció. Vuelve a iniciar sesión."
        );
        return id;
    }

    public Map<String, Object> status(HttpSession session) {
        if (session.getAttribute("pendingStudentId") == null) return Map.of(
            "requiere_verificacion",
            false
        );
        var profile = estudianteDAO.perfil(identity(session));
        long retryAt = session.getAttribute("otpResendAt") instanceof Long value ? value : 0L;
        boolean sent =
            Boolean.TRUE.equals(session.getAttribute("otpSent")) &&
            session.getAttribute("otpExpires") instanceof Long expires &&
            expires > System.currentTimeMillis();
        return Map.of(
            "requiere_verificacion",
            true,
            "correo",
            profile.get("correo"),
            "codigo_enviado",
            sent,
            "reenviar_en_segundos",
            Math.max(0L, (retryAt - System.currentTimeMillis() + 999) / 1000),
            "csrf",
            session.getAttribute("csrf")
        );
    }

    private String hash(String code) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Genera y envía el código respetando el intervalo entre intentos. */
    public void send(HttpSession session) {
        synchronized (session) {
            long id = identity(session),
                now = System.currentTimeMillis();
            if (
                session.getAttribute("otpResendAt") instanceof Long next && now < next
            ) throw new ResponseStatusException(
                TOO_MANY_REQUESTS,
                "Espera un minuto antes de volver a solicitar el código."
            );
            session.setAttribute("otpResendAt", now + 60000L);
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
            // Un reenvío fallido conserva el código anterior si todavía era válido.
            try {
                mail.sendVerificationCode(estudianteDAO.perfil(id).get("correo").toString(), code);
            } catch (RuntimeException ex) {
                session.setAttribute("otpResendAt", System.currentTimeMillis() + 5000L);
                throw ex;
            }
            session.setAttribute("otpHash", hash(code));
            session.setAttribute("otpExpires", now + 600000L);
            session.setAttribute("otpFailures", 0);
            session.setAttribute("otpSent", true);
        }
    }

    /** Comprueba el código, su vigencia y el límite de intentos. */
    public Map<String, Object> verify(HttpSession session, String code) {
        synchronized (session) {
            long id = identity(session);
            if (
                !(session.getAttribute("otpExpires") instanceof Long expires) ||
                System.currentTimeMillis() > expires ||
                session.getAttribute("otpHash") == null
            ) throw new ResponseStatusException(
                BAD_REQUEST,
                "El código no está disponible o venció. Solicita uno nuevo."
            );
            int failures = (Integer) session.getAttribute("otpFailures");
            if (failures >= 5) throw new ResponseStatusException(
                TOO_MANY_REQUESTS,
                "Alcanzaste el límite de intentos. Solicita un código nuevo."
            );
            session.setAttribute("otpFailures", failures + 1);
            if (
                code == null ||
                !code.matches("[0-9]{6}") ||
                !MessageDigest.isEqual(
                    hash(code).getBytes(StandardCharsets.UTF_8),
                    session.getAttribute("otpHash").toString().getBytes(StandardCharsets.UTF_8)
                )
            ) throw new ResponseStatusException(
                BAD_REQUEST,
                "El código no es correcto. Revisa los seis dígitos."
            );
            if (
                estudianteDAO.verificarCorreo(id, (String) session.getAttribute("pendingAuth")) != 1
            ) throw new ResponseStatusException(
                UNAUTHORIZED,
                "La solicitud venció. Vuelve a iniciar sesión."
            );
            session.removeAttribute("otpHash");
            return estudianteDAO.perfil(id);
        }
    }
}
