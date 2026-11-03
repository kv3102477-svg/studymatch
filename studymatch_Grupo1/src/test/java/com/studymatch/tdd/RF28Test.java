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

class RF28Test extends BaseTdd {

    @Test
    @DisplayName("RF28_CP01 E1 RF-0028-Finalización de sesión")
    void RF28_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("marca_receptor", true);
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "finalizar",
            Map.of()
        );
        // ASSERT
        assertEquals("COMPLETADA", salida.get("estado"));
        verify(insignias).otorgarAlcanzadas(101L);
    }

    @Test
    @DisplayName(
        "RF28_CP02 E2 RF-0028-Finalización automática por caducidad de tiempo"
    )
    void RF28_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("duracion_programada_min", 60);
        // ACT
        var salida = sesionService.sessionAction(
            101L,
            501L,
            "finalizar_por_caducidad",
            Map.of()
        );
        // ASSERT
        assertEquals("COMPLETADA", salida.get("estado"));
    }

    @Test
    @DisplayName(
        "RF28_CP03 E3 RF-0028-Intento de marcación prematura (Sesión no iniciada)"
    )
    void RF28_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(
                101L,
                501L,
                "finalizar",
                Map.of()
            )
        );
        // ASSERT
        assertEquals("La sesión no está en curso.", error.getReason());
    }

    @Test
    @DisplayName(
        "RF28_CP04 E4 RF-0028-Intento de marcación en sesión cancelada"
    )
    void RF28_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CANCELADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(
                101L,
                501L,
                "finalizar",
                Map.of()
            )
        );
        // ASSERT
        assertEquals("La sesión no está en curso.", error.getReason());
    }
}
