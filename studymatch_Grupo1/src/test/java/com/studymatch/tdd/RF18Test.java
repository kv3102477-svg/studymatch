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

class RF18Test extends BaseTdd {

    @Test
    @DisplayName("RF18_CP01 E1 RF-0018-Aceptación de emparejamiento")
    void RF18_CP01() throws Exception {
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
        assertEquals(1, avisosGuardados.size());
    }

    @Test
    @DisplayName("RF18_CP02 E2 RF-0018-Rechazo de la Propuesta")
    void RF18_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "PROPUESTA");
        // ACT
        var salida = sesionService.sessionAction(
            202L,
            501L,
            "rechazar",
            Map.of()
        );
        // ASSERT
        assertEquals("CANCELADA", salida.get("estado"));
        assertEquals(1, avisosGuardados.size());
    }

    @Test
    @DisplayName("RF18_CP03 E3 RF-0018-Propuesta Expirada (> 72 horas)")
    void RF18_CP03() throws Exception {
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
        verify(sesiones, never()).confirmar(anyLong());
    }

    @Test
    @DisplayName("RF18_CP04 E4 RF-0018-Emisor Eliminó su Cuenta")
    void RF18_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Cuenta no disponible."
            )
        )
            .when(estudiantes)
            .perfil(101L);
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(202L, 501L, "aceptar", Map.of())
        );
        // ASSERT
        assertEquals("Cuenta no disponible.", error.getReason());
        verify(sesiones, never()).confirmar(anyLong());
    }
}
