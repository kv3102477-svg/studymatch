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

class RF19Test extends BaseTdd {

    @Test
    @DisplayName("RF19_CP01 E1 RF-0019-Notificación de emparejamiento")
    void RF19_CP01() throws Exception {
        // ARRANGE
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        long id = sesionService.propose(101L, entrada);
        // ASSERT
        assertEquals(1, avisosGuardados.size());
        assertEquals(
            "PROPUESTA_SESION",
            avisosGuardados.get(0).get("evento")
        );
        assertEquals(id, avisosGuardados.get(0).get("referencia_id"));
    }

    @Test
    @DisplayName(
        "RF19_CP02 E2 RF-0019-Push Desactivado  EVENTO ALTERNATIVO"
    )
    void RF19_CP02() throws Exception {
        // ARRANGE
        var receptor = perfil(202L);
        receptor.put("push_activo", false);
        doReturn(receptor).when(estudiantes).perfil(202L);
        var entrada = propuesta(LocalDateTime.of(2026, 10, 29, 10, 0));
        // ACT
        sesionService.propose(101L, entrada);
        // ASSERT
        assertEquals(1, avisosGuardados.size());
        assertEquals(202L, avisosGuardados.get(0).get("receptor_id"));
    }

    @Test
    @DisplayName(
        "RF19_CP03 E3 RF-0019-Fallo de Red/ Dispositivo y Reintento"
    )
    void RF19_CP03() throws Exception {
        // ARRANGE
        when(avisos.pendientes()).thenReturn(
            List.of(
                fila(
                    "id",
                    1L,
                    "receptor_id",
                    101L,
                    "titulo",
                    "Nueva propuesta",
                    "mensaje",
                    "Aviso de prueba",
                    "intentos",
                    0
                )
            )
        );
        doThrow(
            new org.springframework.mail.MailSendException(
                "Timeout simulado"
            )
        )
            .when(correoService)
            .sendAviso(anyString(), anyString(), anyString());
        // ACT
        assertDoesNotThrow(() -> notificacionService.reintentar());
        // ASSERT
        verify(avisos).registrarFallo(
            eq(1L),
            eq("MailSendException"),
            any()
        );
        verify(avisos, never()).marcarEnviado(anyLong());
    }

    @Test
    @DisplayName(
        "RF19_CP04 E4 RF-0019-Propuesta Ya Revisada en Otra Sesión"
    )
    void RF19_CP04() throws Exception {
        // ARRANGE
        sesionActual.put("estado", "CONFIRMADA");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            sesionService.sessionAction(202L, 501L, "aceptar", Map.of())
        );
        // ASSERT
        assertEquals(
            "La propuesta no se puede aceptar.",
            error.getReason()
        );
        assertTrue(avisosGuardados.isEmpty());
    }
}
