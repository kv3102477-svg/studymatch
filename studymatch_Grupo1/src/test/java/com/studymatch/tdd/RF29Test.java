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

class RF29Test extends BaseTdd {

    @Test
    @DisplayName("RF29_CP01 E1 RF-0029-Registro de Horas de Mentoría")
    void RF29_CP01() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                2.0,
                "horas_aprendidas",
                0.0,
                "sesiones_completadas",
                2L
            )
        );
        // ACT
        var salida = progresoService.hours(101L);
        // ASSERT
        assertEquals(2.0, salida.get("horas_ensenadas"));
        assertEquals(0.0, salida.get("horas_aprendidas"));
    }

    @Test
    @DisplayName(
        "RF29_CP02 E2 RF-0029-Duración no registrada o insuficiente (< 15 min)"
    )
    void RF29_CP02() throws Exception {
        // ARRANGE
        var ejecutor = mock(com.studymatch.dao.jdbc.SqlExecutor.class);
        var dao = new com.studymatch.dao.jdbc.JdbcSesionDAO(ejecutor);
        // ACT
        dao.horasAcumuladas(101L);
        // ASSERT
        verify(ejecutor).one(
            argThat(
                sql ->
                    sql.contains("duracion_real_min>=15") &&
                    sql.contains("en_revision")
            ),
            eq(101L),
            eq(101L),
            eq(101L),
            eq(101L),
            eq(101L),
            eq(101L)
        );
    }

    @Test
    @DisplayName("RF29_CP03 E3 RF-0029-Registro de rol dual")
    void RF29_CP03() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                2.0,
                "horas_aprendidas",
                1.5,
                "sesiones_completadas",
                3L
            )
        );
        // ACT
        var salida = progresoService.hours(101L);
        // ASSERT
        assertEquals(2.0, salida.get("horas_ensenadas"));
        assertEquals(1.5, salida.get("horas_aprendidas"));
    }

    @Test
    @DisplayName("RF29_CP04 E4 RF-0029-Inconsistencia en el cálculo")
    void RF29_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("cálculo de horas")
        )
            .when(sesiones)
            .horasAcumuladas(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> progresoService.hours(101L)
        );
        // ASSERT
        assertEquals("cálculo de horas", error.getMessage());
    }
}
