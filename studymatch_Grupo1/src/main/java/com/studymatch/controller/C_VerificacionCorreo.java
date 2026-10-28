package com.studymatch.controller;

import com.studymatch.service.AuthSessionService;
import com.studymatch.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class C_VerificacionCorreo {

    private final EmailVerificationService verification;
    private final AuthSessionService sessions;

    public C_VerificacionCorreo(
        EmailVerificationService verification,
        AuthSessionService sessions
    ) {
        this.verification = verification;
        this.sessions = sessions;
    }

    @GetMapping("/verificacion")
    public Object verificationStatus(HttpSession session) {
        return verification.status(session);
    }

    @PostMapping("/verificar")
    public Object verify(@RequestBody Map<String, String> input, HttpServletRequest request) {
        return sessions.establish(
            verification.verify(request.getSession(), input.get("codigo")),
            request
        );
    }

    @PostMapping("/reenviar-codigo")
    public Object resend(HttpSession session) {
        verification.send(session);
        return verification.status(session);
    }
}
