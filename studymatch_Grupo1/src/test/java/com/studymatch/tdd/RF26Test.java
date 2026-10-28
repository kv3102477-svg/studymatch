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

class RF26Test extends BaseTdd {

    @Test
    @DisplayName("RF26_CP01 E1 RF-0026-Registro de Duración")
    void RF26_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("marca_receptor", true);
        // ACT
        sesionService.sessionAction(101L, 501L, "finalizar", Map.of());
        // ASSERT
        assertEquals(60L, sesionActual.get("duracion_real_min"));
        assertEquals("COMPLETADA", sesionActual.get("estado"));
    }

    @Test
    @DisplayName(
        "RF26_CP02 E2 RF-0026-Omitió marcar fin / Cálculo con duración estimada"
    )
    void RF26_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("fin_real", null);
        sesionActual.put("duracion_programada_min", 60);
        // ACT
        sesionService.sessionAction(
            101L,
            501L,
            "finalizar_por_caducidad",
            Map.of()
        );
        // ASSERT
        assertEquals(60L, sesionActual.get("duracion_real_min"));
        assertEquals("ESTIMADO", sesionActual.get("origen_duracion"));
    }

    @Test
    @DisplayName(
        "RF26_CP03 E3 RF-0026-Duración inusualmente corta (< 5min) con confirmación"
    )
    void RF26_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("marca_receptor", true);
        sesionActual.put(
            "inicio_real",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 27, 9, 58)
            )
        );
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
        assertEquals(
            "La duración es menor de cinco minutos. Confirma para finalizar.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF26_CP04 E4 RF-0026-Fallo en el cálculo de duración y registro sin tiempo"
    )
    void RF26_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "EN_CURSO");
        sesionActual.put("marca_receptor", true);
        sesionActual.put("inicio_real", null);
        // ACT
        var salida = assertDoesNotThrow(() ->
            sesionService.sessionAction(
                101L,
                501L,
                "finalizar",
                Map.of()
            )
        );
        // ASSERT
        assertEquals("COMPLETADA", salida.get("estado"));
        assertNull(salida.get("duracion_real_min"));
    }
}
