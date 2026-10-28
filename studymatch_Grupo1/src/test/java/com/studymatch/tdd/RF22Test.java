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

class RF22Test extends BaseTdd {

    @Test
    @DisplayName("RF22_CP01 E1 RF-0022-Confirmación de sesión")
    void RF22_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "PROPUESTA");
        // ACT
        var salida = sesionService.sessionAction(
            202L,
            501L,
            "aceptar",
            Map.of()
        );
        // ASSERT
        assertEquals("CONFIRMADA", salida.get("estado"));
    }

    @Test
    @DisplayName("RF22_CP02 E2 RF-0022-Reprogramación con Conflicto de")
    void RF22_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "PROPUESTA");
        when(sesiones.buscarCruces(anyLong(), any(), any())).thenReturn(
            List.of(fila("id", 900L))
        );
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(
                202L,
                501L,
                "reprogramar",
                entrada
            )
        );
        // ASSERT
        assertEquals(
            "El horario se cruza con otra sesión de uno de los estudiantes.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF22_CP03 E3 RF-0022-Cancelación por Desacuerdo tras Varios Intentos"
    )
    void RF22_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "PROPUESTA");
        sesionActual.put("reprogramaciones", 3);
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        var salida = sesionService.sessionAction(
            202L,
            501L,
            "reprogramar",
            entrada
        );
        // ASSERT
        assertEquals("CANCELADA", salida.get("estado"));
    }

    @Test
    @DisplayName(
        "RF22_CP04 E4 RF-0022-Propuesta Expirada por Plazo sin Respuesta"
    )
    void RF22_CP04() throws Exception {
        // ARRANGE
        sesionActual.put(
            "creada_en",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 23, 10, 0)
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(202L, 501L, "aceptar", Map.of())
        );
        // ASSERT
        assertEquals(
            "La propuesta venció después de 72 horas.",
            error.getReason()
        );
    }
}
