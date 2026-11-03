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

class RF32Test extends BaseTdd {

    @Test
    @DisplayName("RF32_CP01 E1 RF-0032-Comentario de sesión")
    void RF32_CP01() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        sesionService.rate(
            202L,
            501L,
            fila("estrellas", 5, "texto", "Muy clara la clase")
        );
        // ASSERT
        verify(calificaciones).guardarComentario(
            1L,
            "Muy clara la clase"
        );
    }

    @Test
    @DisplayName(
        "RF32_CP02 E2 RF-0032-Calificación sin comentario (Campo de texto vacío)"
    )
    void RF32_CP02() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        sesionService.rate(
            202L,
            501L,
            fila("estrellas", 5, "texto", "")
        );
        // ASSERT
        verify(calificaciones).guardar(501L, 202L, 5);
        verify(calificaciones, never()).guardarComentario(
            anyLong(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF32_CP03 E3 RF-0032-Exceso del límite de caracteres (> 500 caracteres)"
    )
    void RF32_CP03() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        var entrada = fila("estrellas", 5, "texto", "a".repeat(501));
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.rate(202L, 501L, entrada)
        );
        // ASSERT
        assertEquals(
            "El comentario admite hasta 500 caracteres.",
            error.getReason()
        );
        verify(calificaciones, never()).guardar(
            anyLong(),
            anyLong(),
            anyInt()
        );
    }

    @Test
    @DisplayName(
        "RF32_CP04 E4 RF-0032-Detección de lenguaje inapropiado"
    )
    void RF32_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "COMPLETADA");
        // ACT
        var salida = sesionService.rate(
            202L,
            501L,
            fila("estrellas", 5, "texto", "Eres un idiota")
        );
        // ASSERT
        assertEquals(true, salida.get("requiere_moderacion"));
        assertEquals(
            "Gracias por tu opinión. Tu comentario está en revisión",
            salida.get("mensaje")
        );
        verify(calificaciones).guardarComentario(1L, "Eres un idiota");
    }
}
