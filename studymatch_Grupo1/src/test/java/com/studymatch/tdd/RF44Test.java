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

class RF44Test extends BaseTdd {

    @Test
    @DisplayName("RF44_CP01 E1 RF-0044-Reporte de retención")
    void RF44_CP01() throws Exception {
        // ARRANGE
        when(
            reportes.estudiantesActivos(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
            )
        ).thenReturn(Set.of(101L, 202L, 303L, 404L));
        when(
            reportes.estudiantesActivos(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
            )
        ).thenReturn(Set.of(101L, 202L));
        // ACT
        var salida = reportesService.retencion(
            true,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(50.0, salida.get("tasa"));
        assertEquals(4, salida.get("iniciales"));
        assertEquals(2, salida.get("finales"));
    }

    @Test
    @DisplayName(
        "RF44_CP02 E2 RF-0044-Reporte de retención - Segundo periodo no completado"
    )
    void RF44_CP02() throws Exception {
        // ARRANGE
        var fin = LocalDate.of(2099, 12, 31);
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            reportesService.retencion(
                true,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
                fin
            )
        );
        // ASSERT
        assertEquals(
            "Disponible al concluir el segundo periodo seleccionado",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF44_CP03 E3 RF-0044-Reporte de retención - No existen estudiantes activos"
    )
    void RF44_CP03() throws Exception {
        // ARRANGE
        when(reportes.estudiantesActivos(any(), any())).thenReturn(
            Set.of()
        );
        // ACT
        var salida = reportesService.retencion(
            true,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertNull(salida.get("tasa"));
        assertEquals(0, salida.get("iniciales"));
    }

    @Test
    @DisplayName(
        "RF44_CP04 E4 RF-0044-Reporte de retención - Retención mayor a 100%"
    )
    void RF44_CP04() throws Exception {
        // ARRANGE
        when(
            reportes.estudiantesActivos(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
            )
        ).thenReturn(Set.of(101L));
        when(
            reportes.estudiantesActivos(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
            )
        ).thenReturn(Set.of(101L, 202L));
        // ACT
        var salida = reportesService.retencion(
            true,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)
        );
        // ASSERT
        assertEquals(100.0, salida.get("tasa"));
        assertEquals(
            "Se sumaron estudiantes nuevos en el segundo periodo",
            salida.get("mensaje")
        );
    }
}
