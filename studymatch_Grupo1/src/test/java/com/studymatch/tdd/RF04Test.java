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

class RF04Test extends BaseTdd {

    @Test
    @DisplayName("RF04_CP01 E1 RF-004-CIERRE DE SESIÓN")
    void RF04_CP01() throws Exception {
        // ARRANGE
        var control = new com.studymatch.controller.C_Autenticacion(
            estudianteService,
            correoService,
            autenticacionService
        );
        var session =
            new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("studentId", 101L);
        // ACT
        var salida = control.logout(session);
        // ASSERT
        assertEquals("Sesión cerrada.", salida.get("mensaje"));
        assertTrue(session.isInvalid());
    }

    @Test
    @DisplayName(
        "RF04_CP02 E2 RF-004-Error del sistema al cerrar sesión"
    )
    void RF04_CP02() throws Exception {
        // ARRANGE
        var control = new com.studymatch.controller.C_Autenticacion(
            estudianteService,
            correoService,
            autenticacionService
        );
        var session = mock(jakarta.servlet.http.HttpSession.class);
        doThrow(new RuntimeException("invalidación fallida"))
            .when(session)
            .invalidate();
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            control.logout(session)
        );
        // ASSERT
        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    @DisplayName(
        "RF04_CP04 E4 RF-004-Sesión ya expirada al cerrar sesión"
    )
    void RF04_CP04() throws Exception {
        // ARRANGE
        var control = new com.studymatch.controller.C_Autenticacion(
            estudianteService,
            correoService,
            autenticacionService
        );
        var session =
            new org.springframework.mock.web.MockHttpSession();
        session.invalidate();
        // ACT
        var salida = assertDoesNotThrow(() -> control.logout(session));
        // ASSERT
        assertEquals("Sesión cerrada.", salida.get("mensaje"));
    }
}
