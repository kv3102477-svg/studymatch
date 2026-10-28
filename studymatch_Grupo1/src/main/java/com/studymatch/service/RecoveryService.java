package com.studymatch.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.TokenDAO;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Gestiona la recuperación de contraseña y el envío de correos. */
@Service
public class RecoveryService {

    private final EstudianteDAO estudianteDAO;
    private final TokenDAO tokenDAO;

    @Value("${MAIL_HOST:}")
    private String host;

    @Value("${MAIL_PORT:587}")
    private int port;

    @Value("${MAIL_USER:}")
    private String user;

    @Value("${MAIL_PASSWORD:}")
    private String password;

    @Value("${MAIL_FROM:}")
    private String from;

    @Value("${APP_URL:http://localhost:8080}")
    private String appUrl;

    private final TemplateEngine templates;

    public RecoveryService(
        EstudianteDAO estudianteDAO,
        TokenDAO tokenDAO,
        TemplateEngine templates
    ) {
        this.estudianteDAO = estudianteDAO;
        this.tokenDAO = tokenDAO;
        this.templates = templates;
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Crea un enlace temporal para recuperar la contraseña de una cuenta existente. */
    @Transactional
    public void request(String email) {
        if (host.isBlank() || from.isBlank()) throw new ResponseStatusException(
            SERVICE_UNAVAILABLE,
            "La recuperación de contraseña todavía no está habilitada."
        );
        email = com.studymatch.service.Validaciones.required(email, "correo", 120).toLowerCase(
            Locale.ROOT
        );
        var rows = estudianteDAO.buscarPorCorreo(email);
        if (rows.isEmpty()) return;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        Object student = rows.get(0).get("id");
        tokenDAO.invalidarRecuperacion(((Number) student).longValue());
        tokenDAO.crearRecuperacion(((Number) student).longValue(), hash(token));
        sendMail(
            email,
            "Restablecer tu contraseña de StudyMatch",
            "Para crear una nueva contraseña, abre este enlace (válido durante 30 minutos):\n" +
                appUrl +
                "/?reset=" +
                token,
            renderMail(
                "Restablece tu contraseña",
                "Recibimos una solicitud para cambiar la contraseña de tu cuenta. Usa el siguiente botón para crear una nueva.",
                null,
                appUrl + "/?reset=" + token,
                30
            )
        );
    }

    public void sendVerificationCode(String email, String code) {
        sendMail(
            email,
            "Verifica tu correo de StudyMatch",
            "Tu código de verificación es: " +
                code +
                "\nVence en 10 minutos. Si no solicitaste este registro, ignora este mensaje.",
            renderMail(
                "Verifica tu correo",
                "Confirma tu correo institucional para empezar a aprender y compartir. Ingresa este código en StudyMatch:",
                code,
                null,
                10
            )
        );
    }

    private String renderMail(
        String title,
        String description,
        String code,
        String actionUrl,
        int minutes
    ) {
        var context = new Context(new Locale("es"));
        context.setVariable("title", title);
        context.setVariable("description", description);
        context.setVariable("code", code);
        context.setVariable("actionUrl", actionUrl);
        context.setVariable("minutes", minutes);
        return templates.process("email", context);
    }

    private void sendMail(String email, String subject, String body, String html) {
        if (host.isBlank() || from.isBlank()) throw new ResponseStatusException(
            SERVICE_UNAVAILABLE,
            "Falta configurar el envío de correo. Intenta de nuevo cuando el administrador lo habilite."
        );
        var sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(password);
        var settings = sender.getJavaMailProperties();
        settings.setProperty("mail.smtp.auth", String.valueOf(!user.isBlank()));
        settings.setProperty("mail.smtp.starttls.enable", "true");
        settings.setProperty("mail.smtp.starttls.required", "true");
        settings.setProperty("mail.smtp.connectiontimeout", "10000");
        settings.setProperty("mail.smtp.timeout", "10000");
        settings.setProperty("mail.smtp.writetimeout", "10000");
        try {
            var message = sender.createMimeMessage();
            var mail = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            mail.setFrom(from, "StudyMatch");
            mail.setTo(email);
            mail.setSubject(subject);
            mail.setText(body, html);
            mail.addInline(
                "studymatch-logo",
                new ClassPathResource("static/images/studymatch-logo.png"),
                "image/png"
            );
            sender.send(message);
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            throw new MailPreparationException("No se pudo preparar el correo de StudyMatch.", ex);
        }
    }

    /** Comprueba el token de recuperación antes de cambiar la contraseña. */
    @Transactional
    public void reset(String token, String raw) {
        if (
            token == null ||
            !token.matches("[a-f0-9]{64}") ||
            raw == null ||
            raw.length() < 8 ||
            raw.getBytes(StandardCharsets.UTF_8).length > 72
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "Revisa el enlace y la contraseña (mínimo 8 caracteres)."
        );
        var row = tokenDAO.buscarRecuperacionVigente(hash(token));
        estudianteDAO.cambiarClave(
            new BCryptPasswordEncoder(12).encode(raw),
            ((Number) row.get("estudiante_id")).longValue()
        );
        tokenDAO.invalidarRecuperacion(((Number) row.get("estudiante_id")).longValue());
    }
}
