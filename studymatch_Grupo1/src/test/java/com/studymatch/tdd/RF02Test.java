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

class RF02Test extends BaseTdd {

    @Test
    @DisplayName("RF02_CP01 E1 RF-002-INICIO DE SESIÓN")
    void RF02_CP01() throws Exception {
        // ARRANGE
        var entrada = Map.of(
            "correo",
            "u12345678@utp.edu.pe",
            "contrasena",
            "Clave-Prueba2026!"
        );
        // ACT
        var salida = estudianteService.login(entrada);
        // ASSERT
        assertEquals(101L, salida.get("id"));
        assertFalse(salida.containsKey("contrasena_hash"));
    }

    @Test
    @DisplayName("RF02_CP02 E2 RF-002-Credenciales incorrectas")
    void RF02_CP02() throws Exception {
        // ARRANGE
        var entrada = Map.of(
            "correo",
            "u12345678@utp.edu.pe",
            "contrasena",
            "Incorrecta"
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            estudianteService.login(entrada)
        );
        // ASSERT
        assertEquals(
            "Correo o contraseña incorrectos.",
            error.getReason()
        );
    }

    @Test
    @DisplayName("RF02_CP03 E3 RF-002-Cuenta no verificada")
    void RF02_CP03() throws Exception {
        // ARRANGE
        var perfil = perfil(101L);
        perfil.put("correo_verificado", false);
        var request =
            new org.springframework.mock.web.MockHttpServletRequest();
        request.getSession();
        // ACT
        var salida = autenticacionService.authenticate(perfil, request);
        // ASSERT
        assertEquals(true, salida.get("requiere_verificacion"));
        assertNull(request.getSession().getAttribute("studentId"));
        assertEquals(
            101L,
            request.getSession().getAttribute("pendingStudentId")
        );
    }

    @Test
    @DisplayName(
        "RF02_CP04 E4 RF-002-Error del sistema al crear la sesión"
    )
    void RF02_CP04() throws Exception {
        // ARRANGE
        doThrow(new DataAccessResourceFailureException("crear sesión"))
            .when(estudiantes)
            .claveGuardada(anyLong());
        var request =
            new org.springframework.mock.web.MockHttpServletRequest();
        request.getSession();
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> autenticacionService.establish(perfil(101L), request)
        );
        // ASSERT
        assertEquals("crear sesión", error.getMessage());
        assertNull(request.getSession().getAttribute("studentId"));
    }
}
