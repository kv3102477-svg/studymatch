package com.studymatch.tdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("RF21 Programar sesión")
class RF21Test extends BaseTdd {

    @Test
    @DisplayName("RF21_CP01 E1 Propuesta válida")
    void RF21_CP01_creaPropuestaYNotificacion() {
        // ARRANGE
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        long id = sesionService.propose(101L, entrada);
        // ASSERT
        assertAll(
            () -> assertEquals(1L, id),
            () -> assertEquals(1, sesionesGuardadas.size()),
            () ->
                assertEquals(
                    "PROPUESTA",
                    sesionesGuardadas.get(0).get("estado")
                ),
            () -> assertEquals(1, avisosGuardados.size()),
            () ->
                assertEquals(
                    202L,
                    avisosGuardados.get(0).get("receptor_id")
                )
        );
    }

    @Test
    @DisplayName(
        "RF21_CP02 E2 Fuera de la disponibilidad del compañero"
    )
    void RF21_CP02_rechazaHorarioFueraDeDisponibilidad() {
        // ARRANGE
        when(horarios.bloquesDelDia(eq(202L), anyInt())).thenReturn(
            List.of(
                fila(
                    "hora_inicio",
                    Time.valueOf("09:00:00"),
                    "hora_fin",
                    Time.valueOf("12:00:00")
                )
            )
        );
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 18, 0));
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.propose(101L, entrada)
        );
        // ASSERT
        assertEquals(
            "La sesión completa debe estar dentro de la disponibilidad de ambos estudiantes.",
            error.getReason()
        );
        assertTrue(sesionesGuardadas.isEmpty());
        assertTrue(avisosGuardados.isEmpty());
    }

    @Test
    @DisplayName("RF21_CP03 E3 Fecha menor de 24 horas")
    void RF21_CP03_rechazaMenosDe24Horas() {
        // ARRANGE: reloj fijo a las 10:00; la propuesta es diez horas después.
        var entrada = propuesta(LocalDateTime.of(2026, 10, 27, 20, 0));
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.propose(101L, entrada)
        );
        // ASSERT
        assertEquals(
            "Programa la sesión con más de 24 horas de anticipación.",
            error.getReason()
        );
        assertTrue(sesionesGuardadas.isEmpty());
        assertTrue(avisosGuardados.isEmpty());
    }

    @Test
    @DisplayName("RF21_CP04 E4 Falla el DAO al guardar")
    void RF21_CP04_noNotificaUnaPropuestaQueNoSeGuardo() {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("BD no disponible")
        )
            .when(sesiones)
            .crearPropuesta(
                anyLong(),
                anyLong(),
                anyLong(),
                any(),
                anyInt(),
                anyLong(),
                anyLong()
            );
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> sesionService.propose(101L, entrada)
        );
        // ASSERT
        assertEquals("BD no disponible", error.getMessage());
        assertTrue(sesionesGuardadas.isEmpty());
        assertTrue(avisosGuardados.isEmpty());
    }
}
