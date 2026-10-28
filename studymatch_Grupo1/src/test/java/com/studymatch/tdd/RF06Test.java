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

class RF06Test extends BaseTdd {

    @Test
    @DisplayName("RF06_CP01 E1 RF-006-ELIMINACIÓN DE CUENTA")
    void RF06_CP01() throws Exception {
        // ARRANGE
        when(sesiones.listarDelEstudiante(101L)).thenReturn(List.of());
        // ACT
        var salida = (Map<?, ?>) estudianteService.delete(101L);
        // ASSERT
        verify(estudiantes).desactivar(101L);
        assertEquals("Cuenta desactivada.", salida.get("mensaje"));
    }

    @Test
    @DisplayName(
        "RF06_CP03 E3 RF-006-Error del sistema durante la eliminación"
    )
    void RF06_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("eliminar cuenta")
        )
            .when(estudiantes)
            .desactivar(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> estudianteService.delete(101L)
        );
        // ASSERT
        assertEquals("eliminar cuenta", error.getMessage());
    }

    @Test
    @DisplayName("RF06_CP04 E4 RF-006-Sesiones confirmadas pendientes")
    void RF06_CP04() throws Exception {
        // ARRANGE
        when(sesiones.listarDelEstudiante(101L)).thenReturn(
            List.of(
                fila(
                    "estado",
                    "CONFIRMADA",
                    "fecha_hora",
                    java.sql.Timestamp.valueOf(
                        LocalDateTime.of(2099, 1, 1, 10, 0)
                    )
                )
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            estudianteService.delete(101L)
        );
        // ASSERT
        assertEquals(
            "Tienes sesiones confirmadas pendientes. Cancélalas antes de eliminar tu cuenta",
            error.getReason()
        );
        verify(estudiantes, never()).desactivar(101L);
    }
}
