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

class RF08Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF08_CP01 E1 RF-008-REGISTRO DE MATERIAS PARA APRENDER"
    )
    void RF08_CP01() throws Exception {
        // ARRANGE
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "APRENDER",
            "nivel",
            "BASICO"
        );
        // ACT
        materiaService.subject(101L, entrada);
        // ASSERT
        verify(materias).guardarPropia(101L, 10L, "APRENDER", "BASICO");
    }

    @Test
    @DisplayName("RF08_CP02 E2 RF-008-Materia ya registrada")
    void RF08_CP02() throws Exception {
        // ARRANGE
        when(materias.listarPropias(101L)).thenReturn(
            List.of(
                fila(
                    "materia_id",
                    10L,
                    "tipo_rol",
                    "APRENDER",
                    "nivel",
                    "BASICO"
                )
            )
        );
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "APRENDER",
            "nivel",
            "BASICO"
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            materiaService.subject(101L, entrada)
        );
        // ASSERT
        assertEquals(
            "Esta materia ya está en tu lista",
            error.getReason()
        );
        verify(materias, never()).guardarPropia(
            anyLong(),
            anyLong(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("RF08_CP03 E3 RF-008-No se selecciona ninguna materia")
    void RF08_CP03() throws Exception {
        // ARRANGE
        var entrada = fila("tipo_rol", "APRENDER", "nivel", "BASICO");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            materiaService.subject(101L, entrada)
        );
        // ASSERT
        assertEquals("Revisa el campo materia_id.", error.getReason());
        verify(materias, never()).guardarPropia(
            anyLong(),
            anyLong(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF08_CP04 E4 RF-008-Error del sistema al registrar la materia"
    )
    void RF08_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("guardar materia")
        )
            .when(materias)
            .guardarPropia(
                anyLong(),
                anyLong(),
                anyString(),
                anyString()
            );
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "APRENDER",
            "nivel",
            "BASICO"
        );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> materiaService.subject(101L, entrada)
        );
        // ASSERT
        assertEquals("guardar materia", error.getMessage());
    }
}
