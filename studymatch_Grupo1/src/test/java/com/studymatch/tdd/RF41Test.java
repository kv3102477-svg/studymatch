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

class RF41Test extends BaseTdd {

    @Test
    @DisplayName("RF41_CP01 E1 RF-0041-Reporte de asesorías por curso")
    void RF41_CP01() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of(
                fila(
                    "proponente_id",
                    101L,
                    "receptor_id",
                    202L,
                    "duracion_real_min",
                    60
                ),
                fila(
                    "proponente_id",
                    101L,
                    "receptor_id",
                    303L,
                    "duracion_real_min",
                    30
                )
            )
        );
        // ACT
        var salida = reportesService.asesorias(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            10L
        );
        // ASSERT
        assertEquals(2, salida.get("asesorias"));
        assertEquals(1.5, salida.get("horas"));
        assertEquals(3, salida.get("participantes"));
    }

    @Test
    @DisplayName(
        "RF41_CP02 E2 RF-0041-Reporte de asesorías por curso - Sin rol autorizado"
    )
    void RF41_CP02() throws Exception {
        // ARRANGE
        var inicio = LocalDate.of(2026, 9, 1);
        var fin = LocalDate.of(2026, 9, 30);
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            reportesService.asesorias(false, inicio, fin, 10L)
        );
        // ASSERT
        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(reportes);
    }

    @Test
    @DisplayName(
        "RF41_CP03 E3 RF-0041-Reporte de asesorías por curso - Sin sesiones registradas"
    )
    void RF41_CP03() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of()
        );
        // ACT
        var salida = reportesService.asesorias(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            10L
        );
        // ASSERT
        assertEquals(0, salida.get("asesorias"));
        assertEquals(0.0, salida.get("horas"));
        assertEquals(0, salida.get("participantes"));
    }

    @Test
    @DisplayName(
        "RF41_CP04 E4 RF-0041-Reporte de asesorías por curso - Sesiones sin duración registrada"
    )
    void RF41_CP04() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of(
                fila(
                    "proponente_id",
                    101L,
                    "receptor_id",
                    202L,
                    "duracion_real_min",
                    null
                )
            )
        );
        // ACT
        var salida = reportesService.asesorias(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            10L
        );
        // ASSERT
        assertEquals(0.0, salida.get("horas"));
        assertEquals(1, salida.get("sin_duracion"));
    }
}
