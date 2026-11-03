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

class RF25Test extends BaseTdd {

    @Test
    @DisplayName("RF25_CP01 E1 RF-0025-Cancelación de sesión")
    void RF25_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "cancelar",
            fila("motivo", "Examen")
        );
        // ASSERT
        assertEquals("CANCELADA", salida.get("estado"));
        assertTrue(
            avisosGuardados
                .get(0)
                .get("mensaje")
                .toString()
                .contains("Examen")
        );
    }

    @Test
    @DisplayName("RF25_CP02 E2 RF-0025-Motivo no ingresado")
    void RF25_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(
                101L,
                501L,
                "cancelar",
                fila("motivo", " ")
            )
        );
        // ASSERT
        assertEquals("Revisa el campo motivo.", error.getReason());
        assertEquals("CONFIRMADA", sesionActual.get("estado"));
    }

    @Test
    @DisplayName(
        "RF25_CP03 E3 RF-0025-Intento de cancelación de sesión completada"
    )
    void RF25_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(
                101L,
                501L,
                "cancelar",
                fila("motivo", "Examen")
            )
        );
        // ASSERT
        assertEquals(
            "Esta sesión ya no se puede cancelar.",
            error.getReason()
        );
        verify(sesiones, never()).cancelar(anyLong());
    }

    @Test
    @DisplayName(
        "RF25_CP04 E4 RF-0025-Fallo en notificación y cola de reintentos"
    )
    void RF25_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        doThrow(
            new org.springframework.mail.MailSendException(
                "timeout del proveedor"
            )
        )
            .when(avisos)
            .crear(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
            );
        // ACT
        var salida = assertDoesNotThrow(() ->
            sesionService.sessionAction(
                101L,
                501L,
                "cancelar",
                fila("motivo", "Examen")
            )
        );
        // ASSERT
        assertEquals("CANCELADA", salida.get("estado"));
    }
}
