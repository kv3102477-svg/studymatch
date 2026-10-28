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

class RF31Test extends BaseTdd {

    @Test
    @DisplayName("RF31_CP01 E1 RF-0031-Calificación de compañero")
    void RF31_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        sesionService.rate(
            202L,
            501L,
            fila("estrellas", 5, "texto", "Excelente explicación")
        );
        // ASSERT
        verify(calificaciones).guardar(501L, 202L, 5);
        verify(calificaciones).guardarComentario(
            1L,
            "Excelente explicación"
        );
    }

    @Test
    @DisplayName(
        "RF31_CP02 E2 RF-0031-Vencimiento del plazo para calificar"
    )
    void RF31_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        sesionActual.put(
            "fin_real",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 1, 10, 0)
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.rate(202L, 501L, fila("estrellas", 5))
        );
        // ASSERT
        assertEquals(
            "El plazo para calificar ha terminado.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF31_CP03 E3 RF-0031-Actualización de calificación (Intento de duplicidad)"
    )
    void RF31_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        sesionService.rate(202L, 501L, fila("estrellas", 4));
        // ASSERT
        verify(calificaciones).guardar(501L, 202L, 4);
        verify(calificaciones).eliminarComentario(1L);
    }

    @Test
    @DisplayName(
        "RF31_CP04 E4 RF-0031-Intento de calificación en sesión cancelada"
    )
    void RF31_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CANCELADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.rate(202L, 501L, fila("estrellas", 5))
        );
        // ASSERT
        assertEquals(
            "La sesión debe estar completada.",
            error.getReason()
        );
    }
}
