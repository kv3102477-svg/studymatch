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

class RF23Test extends BaseTdd {

    @Test
    @DisplayName("RF23_CP01 E1 RF-0023-Generación de Videollamada")
    void RF23_CP01() throws Exception {
        // ARRANGE
        var entrada = Map.<String, Object>of(
            "enlace",
            "https://meet.google.com/abc-defg-hij"
        );
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "enlace",
            entrada
        );
        // ASSERT
        assertEquals(
            "https://meet.google.com/abc-defg-hij",
            salida.get("enlace_videollamada")
        );
    }

    @Test
    @DisplayName(
        "RF23_CP02 E2 RF-0023-Fallo de Generación Automática e Ingreso Manual"
    )
    void RF23_CP02() throws Exception {
        // ARRANGE
        var entrada = Map.<String, Object>of(
            "enlace",
            "https://zoom.us/j/123456789"
        );
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "enlace",
            entrada
        );
        // ASSERT
        assertEquals(
            "https://zoom.us/j/123456789",
            salida.get("enlace_videollamada")
        );
    }

    @Test
    @DisplayName(
        "RF23_CP04 E4 RF-0023-Invalidación y Retiro de Enlace por Cancelación de Sesión"
    )
    void RF23_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "cancelar",
            fila("motivo", "Cambio de horario")
        );
        // ASSERT
        assertEquals("CANCELADA", salida.get("estado"));
        assertNull(salida.get("enlace_videollamada"));
    }
}
