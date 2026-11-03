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

class RF10Test extends BaseTdd {

    @Test
    @DisplayName("RF10_CP01 E1 RF-010-REGISTRO DE DISPONIBILIDAD")
    void RF10_CP01() throws Exception {
        // ARRANGE
        var entrada = List.of(bloque(2, "10:00", "11:00"));
        // ACT
        disponibilidadService.replaceAvailability(101L, entrada);
        // ASSERT
        verify(horarios).eliminarDelEstudiante(101L);
        verify(horarios).guardarBloques(
            argThat(
                rows -> rows.size() == 1 && rows.get(0)[0].equals(101L)
            )
        );
    }

    @Test
    @DisplayName("RF10_CP02 E2 RF-010-Franja horaria inválida")
    void RF10_CP02() throws Exception {
        // ARRANGE
        var entrada = List.of(bloque(2, "11:00", "10:00"));
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            disponibilidadService.replaceAvailability(101L, entrada)
        );
        // ASSERT
        assertEquals(
            "Revisa el día y el intervalo horario.",
            error.getReason()
        );
        verify(horarios, never()).eliminarDelEstudiante(anyLong());
    }

    @Test
    @DisplayName("RF10_CP03 E3 RF-010-No se registra ningún horario")
    void RF10_CP03() throws Exception {
        // ARRANGE
        List<Map<String, Object>> entrada = List.of();
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            disponibilidadService.replaceAvailability(101L, entrada)
        );
        // ASSERT
        assertEquals(
            "Selecciona al menos una franja horaria",
            error.getReason()
        );
        verify(horarios, never()).eliminarDelEstudiante(anyLong());
    }

    @Test
    @DisplayName("RF10_CP04 E4 RF-010-Error del sistema al guardar")
    void RF10_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("guardar horario")
        )
            .when(horarios)
            .guardarBloques(anyList());
        var entrada = List.of(bloque(2, "10:00", "11:00"));
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () ->
                disponibilidadService.replaceAvailability(101L, entrada)
        );
        // ASSERT
        assertEquals("guardar horario", error.getMessage());
    }
}
