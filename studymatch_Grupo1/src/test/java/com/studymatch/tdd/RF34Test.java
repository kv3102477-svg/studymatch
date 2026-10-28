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

class RF34Test extends BaseTdd {

    @Test
    @DisplayName("RF34_CP01 E1 RF-0034-Periodo de prueba gratuito")
    void RF34_CP01() throws Exception {
        // ARRANGE
        when(suscripciones.buscarPruebaAnterior(101L)).thenReturn(
            List.of()
        );
        when(suscripciones.buscarVigente(101L)).thenReturn(List.of());
        // ACT
        suscripcionService.trial(101L, fila("plan_id", 1L));
        // ASSERT
        verify(suscripciones).crearPrueba(101L, 1L);
    }

    @Test
    @DisplayName(
        "RF34_CP02 E2 RF-0034-Periodo de prueba gratuito - Ya tuvo cuenta previa"
    )
    void RF34_CP02() throws Exception {
        // ARRANGE
        when(suscripciones.buscarPruebaAnterior(101L)).thenReturn(
            List.of(fila("id", 1L))
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            suscripcionService.trial(101L, fila("plan_id", 1L))
        );
        // ASSERT
        assertEquals(
            "Ya utilizaste el periodo de prueba.",
            error.getReason()
        );
        verify(suscripciones, never()).crearPrueba(
            anyLong(),
            anyLong()
        );
    }

    @Test
    @DisplayName(
        "RF34_CP03 E3 RF-0034-Periodo de prueba gratuito - Error de conexión al asignar"
    )
    void RF34_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("asignar prueba")
        )
            .when(suscripciones)
            .crearPrueba(anyLong(), anyLong());
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> suscripcionService.trial(101L, fila("plan_id", 1L))
        );
        // ASSERT
        assertEquals("asignar prueba", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF34_CP04 E4 RF-0034-Periodo de prueba gratuito - Error al actualizar permisos"
    )
    void RF34_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "actualizar acceso al plan"
            )
        )
            .when(suscripciones)
            .crearPrueba(anyLong(), anyLong());
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> suscripcionService.trial(101L, fila("plan_id", 1L))
        );
        // ASSERT
        assertEquals("actualizar acceso al plan", error.getMessage());
    }
}
