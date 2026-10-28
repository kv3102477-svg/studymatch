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

class RF33Test extends BaseTdd {

    @Test
    @DisplayName("RF33_CP01 E1 RF-0033-Ranking de mentoría")
    void RF33_CP01() throws Exception {
        // ARRANGE
        when(insignias.ranking()).thenReturn(
            List.of(
                fila("id", 101L, "horas_ensenadas", 20.0),
                fila("id", 202L, "horas_ensenadas", 10.0)
            )
        );
        // ACT
        var salida = progresoService.ranking();
        // ASSERT
        assertEquals(101L, salida.get(0).get("id"));
        assertEquals(20.0, salida.get(0).get("horas_ensenadas"));
    }

    @Test
    @DisplayName(
        "RF33_CP02 E2 RF-0033-Ranking de mentoría - Ningún estudiante tiene horas"
    )
    void RF33_CP02() throws Exception {
        // ARRANGE
        when(insignias.ranking()).thenReturn(List.of());
        // ACT
        var salida = progresoService.ranking();
        // ASSERT
        assertTrue(salida.isEmpty());
    }

    @Test
    @DisplayName(
        "RF33_CP03 E3 RF-0033-Ranking de mentoría - Empate de horas"
    )
    void RF33_CP03() throws Exception {
        // ARRANGE
        when(insignias.ranking()).thenReturn(
            List.of(
                fila("id", 101L, "horas_ensenadas", 20.0),
                fila("id", 202L, "horas_ensenadas", 20.0)
            )
        );
        // ACT
        var salida = progresoService.ranking();
        // ASSERT
        assertEquals(1, salida.get(0).get("posicion"));
        assertEquals(1, salida.get(1).get("posicion"));
    }

    @Test
    @DisplayName(
        "RF33_CP04 E4 RF-0033-Ranking de mentoría - Estudiante sin horas propias"
    )
    void RF33_CP04() throws Exception {
        // ARRANGE
        when(insignias.ranking()).thenReturn(
            List.of(fila("id", 202L, "horas_ensenadas", 10.0))
        );
        // ACT
        var salida = progresoService.ranking();
        // ASSERT
        assertEquals(1, salida.size());
        assertEquals(202L, salida.get(0).get("id"));
    }
}
