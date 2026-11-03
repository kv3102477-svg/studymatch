package com.studymatch.tdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.studymatch.service.DocumentService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;

class RF03Test extends BaseTdd {

    @Test
    @DisplayName("RF03_CP01 E1 RF-003-RECUPERACIÓN DE CONTRASEÑA")
    void RF03_CP01() throws Exception {
        // ARRANGE
        var service = recuperacion();
        when(estudiantes.buscarPorCorreo(anyString())).thenReturn(
            List.of(fila("id", 101L))
        );
        try (
            var transporte = mockConstruction(
                org.springframework.mail.javamail.JavaMailSenderImpl.class,
                (sender, context) -> {
                    when(sender.getJavaMailProperties()).thenReturn(
                        new Properties()
                    );
                    when(sender.createMimeMessage()).thenReturn(
                        new jakarta.mail.internet.MimeMessage(
                            jakarta.mail.Session.getInstance(
                                new Properties()
                            )
                        )
                    );
                }
            )
        ) {
            // ACT
            service.request("u12345678@utp.edu.pe");
            // ASSERT
            verify(tokens).invalidarRecuperacion(101L);
            verify(tokens).crearRecuperacion(
                eq(101L),
                matches("[a-f0-9]{64}")
            );
            verify(transporte.constructed().get(0)).send(
                any(jakarta.mail.internet.MimeMessage.class)
            );
        }
    }

    @Test
    @DisplayName("RF03_CP02 E2 RF-003-Correo no registrado")
    void RF03_CP02() throws Exception {
        // ARRANGE
        var service = recuperacion();
        when(estudiantes.buscarPorCorreo(anyString())).thenReturn(
            List.of()
        );
        // ACT
        assertDoesNotThrow(() ->
            service.request("u87654321@utp.edu.pe")
        );
        // ASSERT
        verify(tokens, never()).crearRecuperacion(
            anyLong(),
            anyString()
        );
    }

    @Test
    @DisplayName("RF03_CP03 E3 RF-003-Enlace de recuperación expirado")
    void RF03_CP03() throws Exception {
        // ARRANGE
        var service = recuperacion();
        doThrow(
            new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "El enlace venció."
            )
        )
            .when(tokens)
            .buscarRecuperacionVigente(anyString());
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            service.reset("a".repeat(64), "NuevaClave2026!")
        );
        // ASSERT
        assertEquals("El enlace venció.", error.getReason());
        verify(estudiantes, never()).cambiarClave(
            anyString(),
            anyLong()
        );
    }

    @Test
    @DisplayName(
        "RF03_CP04 E4 RF-003-Nueva contraseña no cumple los requisitos"
    )
    void RF03_CP04() throws Exception {
        // ARRANGE
        var service = recuperacion();
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            service.reset("a".repeat(64), "123")
        );
        // ASSERT
        assertEquals(
            "Revisa el enlace y la contraseña (mínimo 8 caracteres).",
            error.getReason()
        );
        verify(estudiantes, never()).cambiarClave(
            anyString(),
            anyLong()
        );
    }
}
