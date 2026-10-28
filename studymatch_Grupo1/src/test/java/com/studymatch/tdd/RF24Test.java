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

class RF24Test extends BaseTdd {

    @Test
    @DisplayName("RF24_CP01 E1 RF-0024-Recordatorio de sesión")
    void RF24_CP01() throws Exception {
        // ARRANGE
        when(avisos.bloquearRecordatorios()).thenReturn(
            fila("acquired", true)
        );
        // ACT
        recordatorioService.processReminders();
        // ASSERT
        verify(avisos).recordarSesiones();
        verify(avisos).recordarVencimientos();
    }

    @Test
    @DisplayName(
        "RF24_CP02 E2 RF-0024-Notificaciones Push Desactivadas"
    )
    void RF24_CP02() throws Exception {
        // ARRANGE
        when(avisos.bloquearRecordatorios()).thenReturn(
            fila("acquired", false)
        );
        // ACT
        recordatorioService.processReminders();
        // ASSERT
        verify(avisos, never()).recordarSesiones();
    }

    @Test
    @DisplayName(
        "RF24_CP03 E3 RF-0024-Sesión Cancelada Omite Envío de Recordatorio"
    )
    void RF24_CP03() throws Exception {
        // ARRANGE
        var ejecutor = mock(com.studymatch.dao.jdbc.SqlExecutor.class);
        var dao = new com.studymatch.dao.jdbc.JdbcNotificacionDAO(
            ejecutor
        );
        // ACT
        dao.recordarSesiones();
        // ASSERT
        verify(ejecutor).update(
            argThat(
                sql ->
                    sql.contains("CONFIRMADA") &&
                    sql.contains("NOT EXISTS")
            )
        );
    }

    @Test
    @DisplayName(
        "RF24_CP04 E4 RF-0024-Fallo de Envío, Reintento y Bitácora de Incidentes"
    )
    void RF24_CP04() throws Exception {
        // ARRANGE
        when(avisos.pendientes()).thenReturn(
            List.of(
                fila(
                    "id",
                    1L,
                    "receptor_id",
                    101L,
                    "titulo",
                    "Recordatorio de sesión",
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
}
