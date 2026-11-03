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

class RF38Test extends BaseTdd {

    @Test
    @DisplayName("RF38_CP01 E1 RF-0038-Aviso de vencimiento")
    void RF38_CP01() throws Exception {
        // ARRANGE
        when(avisos.bloquearRecordatorios()).thenReturn(
            fila("acquired", true)
        );
        // ACT
        recordatorioService.processReminders();
        // ASSERT
        verify(avisos).recordarVencimientos();
    }

    @Test
    @DisplayName(
        "RF38_CP02 E2 RF-0038-Aviso de vencimiento- El usuario ya renovó"
    )
    void RF38_CP02() throws Exception {
        // ARRANGE
        var ejecutor = mock(com.studymatch.dao.jdbc.SqlExecutor.class);
        var dao = new com.studymatch.dao.jdbc.JdbcNotificacionDAO(
            ejecutor
        );
        // ACT
        dao.recordarVencimientos();
        // ASSERT
        verify(ejecutor).update(
            argThat(
                sql ->
                    sql.contains("NOT EXISTS") && sql.contains("newer")
            )
        );
    }

    @Test
    @DisplayName(
        "RF38_CP03 E3 RF-0038-Aviso de vencimiento- Falla en el servidor de correos"
    )
    void RF38_CP03() throws Exception {
        // ARRANGE
        when(avisos.pendientes()).thenReturn(
            List.of(
                fila(
                    "id",
                    1L,
                    "receptor_id",
                    101L,
                    "titulo",
                    "Vencimiento de suscripción",
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
        "RF38_CP04 E4 RF-0038-Aviso de vencimiento - Suscripción ya vencida"
    )
    void RF38_CP04() throws Exception {
        // ARRANGE
        var ejecutor = mock(com.studymatch.dao.jdbc.SqlExecutor.class);
        var dao = new com.studymatch.dao.jdbc.JdbcSuscripcionDAO(
            ejecutor
        );
        // ACT
        dao.expirarVencidas();
        // ASSERT
        verify(ejecutor).update(
            argThat(
                sql ->
                    sql.contains("EXPIRADO") &&
                    sql.contains("fecha_vencimiento<CURRENT_DATE")
            )
        );
    }
}
