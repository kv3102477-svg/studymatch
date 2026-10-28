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

class RF30Test extends BaseTdd {

    @Test
    @DisplayName("RF30_CP01 E1 RF-0030-Otorgamiento de Insignias")
    void RF30_CP01() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                20.0,
                "horas_aprendidas",
                0.0,
                "sesiones_completadas",
                20L
            )
        );
        // ACT
        progresoService.award(101L);
        // ASSERT
        verify(insignias).otorgarAlcanzadas(101L);
    }

    @Test
    @DisplayName(
        "RF30_CP02 E2 RF-0030-Insignia ya otorgada (Evitar duplicidad)"
    )
    void RF30_CP02() throws Exception {
        // ARRANGE
        var ejecutor = mock(com.studymatch.dao.jdbc.SqlExecutor.class);
        var dao = new com.studymatch.dao.jdbc.JdbcInsigniaDAO(ejecutor);
        // ACT
        dao.otorgarAlcanzadas(101L);
        // ASSERT
        verify(ejecutor).update(
            argThat(
                sql ->
                    sql.contains("ON CONFLICT") &&
                    sql.contains("DO NOTHING")
            ),
            eq(101L),
            eq(101L)
        );
    }

    @Test
    @DisplayName(
        "RF30_CP03 E3 RF-0030-Error en el cálculo del acumulado"
    )
    void RF30_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("calcular acumulado")
        )
            .when(insignias)
            .otorgarAlcanzadas(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> progresoService.award(101L)
        );
        // ASSERT
        assertEquals("calcular acumulado", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF30_CP04 E4 RF-0030-Acumulado inconsistente o bloqueado"
    )
    void RF30_CP04() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                -1.0,
                "horas_aprendidas",
                0.0,
                "sesiones_completadas",
                0L
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            progresoService.award(101L)
        );
        // ASSERT
        assertEquals(
            "Las horas están en revisión. No se pueden otorgar insignias.",
            error.getReason()
        );
        verify(insignias, never()).otorgarAlcanzadas(anyLong());
    }
}
