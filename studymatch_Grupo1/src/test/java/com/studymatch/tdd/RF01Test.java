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

class RF01Test extends BaseTdd {

    @Test
    @DisplayName("RF01_CP01 E1 RF-001-REGISTRO DE ESTUDIANTE")
    void RF01_CP01() throws Exception {
        // ARRANGE
        var entrada = registro("Ana Torres", "u12345678@utp.edu.pe");
        // ACT
        var salida = estudianteService.register(entrada);
        // ASSERT
        assertEquals(303L, salida.get("id"));
        assertEquals("Ana Torres", salida.get("nombre"));
        assertEquals(false, salida.get("correo_verificado"));
        assertFalse(salida.containsKey("contrasena_hash"));
        assertEquals(
            "GRATUITO",
            suscripcionesGuardadas.get(0).get("estado")
        );
    }

    @Test
    @DisplayName("RF01_CP02 E2 RF-001-Campos obligatorios vacíos")
    void RF01_CP02() throws Exception {
        // ARRANGE
        var entrada = registro("", "u12345678@utp.edu.pe");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            estudianteService.register(entrada)
        );
        // ASSERT
        assertEquals("Revisa el campo nombre.", error.getReason());
        assertTrue(cuentas.isEmpty());
    }

    @Test
    @DisplayName("RF01_CP03 E3 RF-001-Formato de correo inválido")
    void RF01_CP03() throws Exception {
        // ARRANGE
        var entrada = registro("Ana Torres", "ana@gmail.com");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            estudianteService.register(entrada)
        );
        // ASSERT
        assertEquals(
            "Usa tu correo institucional terminado en @utp.edu.pe.",
            error.getReason()
        );
        assertTrue(cuentas.isEmpty());
    }

    @Test
    @DisplayName("RF01_CP04 E4 RF-001-Correo ya registrado")
    void RF01_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new org.springframework.dao.DuplicateKeyException(
                "correo duplicado"
            )
        )
            .when(estudiantes)
            .crear(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            );
        var entrada = registro("Ana Torres", "u12345678@utp.edu.pe");
        // ACT
        var error = assertThrows(
            org.springframework.dao.DuplicateKeyException.class,
            () -> estudianteService.register(entrada)
        );
        // ASSERT
        assertEquals("correo duplicado", error.getMessage());
        assertTrue(suscripcionesGuardadas.isEmpty());
    }

    @Test
    @DisplayName(
        "RF01_CP05 E5 RF-001-Código de verificación incorrecto o expirado"
    )
    void RF01_CP05() throws Exception {
        // ARRANGE
        var session = pendienteVerificacion();
        verificacionService.send(session);
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            verificacionService.verify(session, "abcdef")
        );
        // ASSERT
        assertEquals(
            "El código no es correcto. Revisa los seis dígitos.",
            error.getReason()
        );
        assertEquals(1, session.getAttribute("otpFailures"));
        verify(estudiantes, never()).verificarCorreo(
            anyLong(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF01_CP06 E6 RF-001-Error del sistema al guardar o enviar el código"
    )
    void RF01_CP06() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("guardar registro")
        )
            .when(estudiantes)
            .crear(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () ->
                estudianteService.register(
                    registro("Ana Torres", "u12345678@utp.edu.pe")
                )
        );
        // ASSERT
        assertEquals("guardar registro", error.getMessage());
        assertTrue(suscripcionesGuardadas.isEmpty());
    }
}
