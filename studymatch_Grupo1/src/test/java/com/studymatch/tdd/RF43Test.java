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

class RF43Test extends BaseTdd {

    @Test
    @DisplayName("RF43_CP01 E1 RF-0043-Reporte de satisfacción")
    void RF43_CP01() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of(fila("id", 1L))
        );
        when(reportes.calificaciones(any(), any())).thenReturn(
            List.of(
                fila("estrellas", 5),
                fila("estrellas", 5),
                fila("estrellas", 4),
                fila("estrellas", 3),
                fila("estrellas", 1)
            )
        );
        // ACT
        var salida = reportesService.satisfaccion(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(3.6, salida.get("promedio"));
        assertEquals(0.0, salida.get("nps"));
        assertEquals(5, salida.get("resenas"));
    }

    @Test
    @DisplayName(
        "RF43_CP02 E2 RF-0043-Reporte de satisfacción - Calificaciones insuficientes"
    )
    void RF43_CP02() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of(fila("id", 1L))
        );
        when(reportes.calificaciones(any(), any())).thenReturn(
            List.of(fila("estrellas", 5))
        );
        // ACT
        var salida = reportesService.satisfaccion(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertNull(salida.get("nps"));
        assertEquals(
            "Datos aún insuficientes para un NPS representativo",
            salida.get("mensaje")
        );
    }

    @Test
    @DisplayName(
        "RF43_CP03 E3 RF-0043-Reporte de satisfacción  - Error en cálculo / Caché"
    )
    void RF43_CP03() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of(fila("id", 1L))
        );
        doThrow(
            new DataAccessResourceFailureException(
                "calcular satisfacción"
            )
        )
            .when(reportes)
            .calificaciones(any(), any());
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () ->
                reportesService.satisfaccion(
                    true,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30)
                )
        );
        // ASSERT
        assertEquals("calcular satisfacción", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF43_CP04 E4 RF-0043-Reporte de satisfacción - Periodo sin sesiones completadas"
    )
    void RF43_CP04() throws Exception {
        // ARRANGE
        when(reportes.sesiones(any(), any(), any())).thenReturn(
            List.of()
        );
        // ACT
        var salida = reportesService.satisfaccion(
            true,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(
            "No hay sesiones completadas en el periodo seleccionado",
            salida.get("mensaje")
        );
        verify(reportes, never()).calificaciones(any(), any());
    }
}
