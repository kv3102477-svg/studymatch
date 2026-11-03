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

class RF45Test extends BaseTdd {

    @Test
    @DisplayName("RF45_CP01 E1 RF-0045-Chat entre estudiantes")
    void RF45_CP01() throws Exception {
        // ARRANGE
        when(
            conversaciones.crearMensaje(
                1L,
                101L,
                "Hola, estudiamos programación"
            )
        ).thenReturn(77L);
        // ACT
        long id = conversacionService.message(
            101L,
            1L,
            fila("texto", "Hola, estudiamos programación")
        );
        // ASSERT
        assertEquals(77L, id);
        assertEquals(1, avisosGuardados.size());
        assertEquals(202L, avisosGuardados.get(0).get("receptor_id"));
    }

    @Test
    @DisplayName(
        "RF45_CP02 E2 RF-0045-Chat entre estudiantes - Emparejamiento rechazado o finalizado"
    )
    void RF45_CP02() throws Exception {
        // ARRANGE
        when(
            conversaciones.buscarParticipante(anyLong(), anyLong())
        ).thenReturn(
            fila(
                "id",
                1L,
                "estudiante_a",
                101L,
                "estudiante_b",
                202L,
                "estado",
                "CERRADA"
            )
        );
        // ACT
        long id = conversacionService.message(
            101L,
            1L,
            fila("texto", "Continuemos en otra materia")
        );
        // ASSERT
        assertEquals(1L, id);
        verify(conversaciones).crearMensaje(
            1L,
            101L,
            "Continuemos en otra materia"
        );
    }

    @Test
    @DisplayName(
        "RF45_CP03 E3 RF-0045-Chat entre estudiantes - Contenido inapropiado"
    )
    void RF45_CP03() throws Exception {
        // ARRANGE
        var entrada = fila("texto", "Eres un idiota");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            conversacionService.message(101L, 1L, entrada)
        );
        // ASSERT
        assertEquals(
            "Mensaje bloqueado y marcado para revisión",
            error.getReason()
        );
        verify(conversaciones).crearMensaje(
            eq(1L),
            eq(101L),
            startsWith("[STUDYMATCH_MODERACION]")
        );
        assertTrue(avisosGuardados.isEmpty());
    }

    @Test
    @DisplayName(
        "RF45_CP04 E4 RF-0045-Chat entre estudiantes - Pérdida de conexión al enviar"
    )
    void RF45_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("conexión al enviar")
        )
            .when(conversaciones)
            .crearMensaje(anyLong(), anyLong(), anyString());
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () ->
                conversacionService.message(
                    101L,
                    1L,
                    fila("texto", "Hola")
                )
        );
        // ASSERT
        assertEquals("conexión al enviar", error.getMessage());
        assertTrue(avisosGuardados.isEmpty());
    }
}
