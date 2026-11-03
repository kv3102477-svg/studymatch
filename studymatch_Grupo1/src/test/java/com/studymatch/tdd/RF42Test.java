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

class RF42Test extends BaseTdd {

    @Test
    @DisplayName("RF42_CP01 E1 RF-0042-Reporte de emparejamientos")
    void RF42_CP01() throws Exception {
        // ARRANGE
        when(
            reportes.emparejamientos(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
            )
        ).thenReturn(
            List.of(
                fila("estado", "ACEPTADO"),
                fila("estado", "ACEPTADO"),
                fila("estado", "RECHAZADO")
            )
        );
        when(
            reportes.emparejamientos(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
            )
        ).thenReturn(List.of(fila("estado", "ACEPTADO")));
        // ACT
        var salida = reportesService.emparejamientos(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(3, salida.get("total"));
        assertEquals(2L, salida.get("aceptados"));
        assertEquals(100.0, salida.get("crecimiento"));
    }

    @Test
    @DisplayName(
        "RF42_CP02 E2 RF-0042-Reporte de emparejamientos - Sin emparejamientos en periodo"
    )
    void RF42_CP02() throws Exception {
        // ARRANGE
        when(reportes.emparejamientos(any(), any())).thenReturn(
            List.of()
        );
        // ACT
        var salida = reportesService.emparejamientos(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(0, salida.get("total"));
        assertNull(salida.get("crecimiento"));
    }

    @Test
    @DisplayName(
        "RF42_CP03 E3 RF-0042-Reporte de emparejamientos - Periodo inválido"
    )
    void RF42_CP03() throws Exception {
        // ARRANGE
        var inicio = LocalDate.of(2026, 9, 30);
        var fin = LocalDate.of(2026, 9, 1);
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            reportesService.emparejamientos(true, inicio, fin)
        );
        // ASSERT
        assertEquals(
            "Revisa el periodo seleccionado.",
            error.getReason()
        );
        verifyNoInteractions(reportes);
    }

    @Test
    @DisplayName(
        "RF42_CP04 E4 RF-0042-Reporte de emparejamientos - Sin histórico del periodo anterior"
    )
    void RF42_CP04() throws Exception {
        // ARRANGE
        when(
            reportes.emparejamientos(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
            )
        ).thenReturn(List.of(fila("estado", "ACEPTADO")));
        // ACT
        var salida = reportesService.emparejamientos(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(true, salida.get("sin_historico"));
        assertNull(salida.get("crecimiento"));
    }
}
