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

class RF48Test extends BaseTdd {

    @Test
    @DisplayName("RF48_CP01 E1 RF-0048-Reporte de usuario")
    void RF48_CP01() throws Exception {
        // ARRANGE
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Otro motivo",
            "Reporte",
            "Me envía mensajes repetidos",
            202L
        );
        // ACT
        var salida = supportService.create(101L, entrada, true);
        // ASSERT
        assertEquals("PENDIENTE", salida.get("estado"));
        assertEquals(false, salida.get("existente"));
        verify(soporte).crear(
            101L,
            202L,
            "REPORTE",
            "Otro motivo",
            "Reporte de compañero",
            "Me envía mensajes repetidos"
        );
    }

    @Test
    @DisplayName(
        "RF48_CP02 E2 RF-0048-Reporte de usuario - Motivo no válido"
    )
    void RF48_CP02() throws Exception {
        // ARRANGE
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Motivo inexistente",
            "Reporte",
            "Mensaje de ejemplo",
            202L
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            supportService.create(101L, entrada, true)
        );
        // ASSERT
        assertEquals("Selecciona un motivo válido.", error.getReason());
    }

    @Test
    @DisplayName(
        "RF48_CP03 E3 RF-0048-Reporte de usuario - Reporte duplicado"
    )
    void RF48_CP03() throws Exception {
        // ARRANGE
        when(soporte.reportePendiente(101L, 202L)).thenReturn(
            List.of(fila("id", 12L))
        );
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Otro motivo",
            "Reporte",
            "Mensaje de ejemplo",
            202L
        );
        // ACT
        var salida = supportService.create(101L, entrada, true);
        // ASSERT
        assertEquals(12L, salida.get("id"));
        assertEquals(true, salida.get("existente"));
        verify(soporte, never()).crear(
            anyLong(),
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF48_CP04 E4 RF-0048-Reporte de usuario - Usuario con reportes acumulados"
    )
    void RF48_CP04() throws Exception {
        // ARRANGE
        when(soporte.contarReportes(202L)).thenReturn(4);
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Otro motivo",
            "Reporte",
            "Usuario con quejas anteriores",
            202L
        );
        // ACT
        var salida = supportService.create(101L, entrada, true);
        // ASSERT
        assertEquals("PENDIENTE", salida.get("estado"));
        assertFalse(salida.containsKey("prioridad"));
        verify(soporte).priorizar(1L);
    }
}
