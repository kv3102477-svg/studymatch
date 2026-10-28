package com.studymatch;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.studymatch.dao.EstudianteDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EstudianteDAO estudianteDAO;

    public WebConfig(EstudianteDAO estudianteDAO) {
        this.estudianteDAO = estudianteDAO;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(
                new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Object handler
                    ) {
                        response.setHeader("Cache-Control", "no-store");
                        return true;
                    }
                }
            )
            .addPathPatterns("/", "/app.js");
        registry
            .addInterceptor(
                new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Object handler
                    ) {
                        response.setHeader("Cache-Control", "no-store");
                        response.setHeader("X-Content-Type-Options", "nosniff");
                        var session = request.getSession(false);
                        if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
                            String expected =
                                session == null ? null : (String) session.getAttribute("csrf");
                            String actual = request.getHeader("X-CSRF-Token");
                            if (
                                expected == null ||
                                actual == null ||
                                !MessageDigest.isEqual(
                                    expected.getBytes(StandardCharsets.UTF_8),
                                    actual.getBytes(StandardCharsets.UTF_8)
                                )
                            ) throw new ResponseStatusException(
                                FORBIDDEN,
                                "La sesión del formulario venció. Recarga la página."
                            );
                        }
                        if (
                            Set.of(
                                "/api/auth/csrf",
                                "/api/auth/catalogos",
                                "/api/auth/verificacion",
                                "/api/auth/verificar",
                                "/api/auth/reenviar-codigo",
                                "/api/auth/login",
                                "/api/auth/registro",
                                "/api/auth/recuperar",
                                "/api/auth/restablecer"
                            ).contains(request.getRequestURI())
                        ) return true;
                        if (
                            session == null || !(session.getAttribute("studentId") instanceof Long)
                        ) throw new ResponseStatusException(
                            UNAUTHORIZED,
                            "Inicia sesión para continuar."
                        );
                        var credentials = estudianteDAO.validarSesion(
                            ((Number) session.getAttribute("studentId")).longValue()
                        );
                        if (
                            credentials.isEmpty() ||
                            !java.util.Objects.equals(
                                session.getAttribute("authVersion"),
                                credentials.get(0).get("contrasena_hash")
                            )
                        ) {
                            session.invalidate();
                            throw new ResponseStatusException(
                                UNAUTHORIZED,
                                "Tu sesión venció. Inicia sesión de nuevo."
                            );
                        }
                        if (
                            !Boolean.TRUE.equals(credentials.get(0).get("correo_verificado"))
                        ) throw new ResponseStatusException(
                            FORBIDDEN,
                            "Verifica tu correo antes de entrar. Vuelve a iniciar sesión."
                        );
                        return true;
                    }
                }
            )
            .addPathPatterns("/api/**");
    }
}
