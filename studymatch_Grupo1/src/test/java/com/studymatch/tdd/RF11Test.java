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

class RF11Test extends BaseTdd {

    @Test
    @DisplayName("RF11_CP01 E1 RF-011-ACTUALIZACIÓN DE DISPONIBILIDAD")
    void RF11_CP01() throws Exception {
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
    @DisplayName("RF11_CP02 E2 RF-011-Franja horaria inválida")
    void RF11_CP02() throws Exception {
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
    @DisplayName(
        "RF11_CP04 E4 RF-011-Error del sistema al guardar los cambios"
    )
    void RF11_CP04() throws Exception {
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
