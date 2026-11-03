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

class RF09Test extends BaseTdd {

    @Test
    @DisplayName("RF09_CP01 E1 RF-009-NIVEL DE DOMINIO ACADÉMICO")
    void RF09_CP01() throws Exception {
        // ARRANGE
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "ENSENAR",
            "nivel",
            "AVANZADO"
        );
        // ACT
        materiaService.subject(101L, entrada);
        // ASSERT
        verify(materias).guardarPropia(
            101L,
            10L,
            "ENSENAR",
            "AVANZADO"
        );
    }

    @Test
    @DisplayName("RF09_CP02 E2 RF-009-No se selecciona un nivel")
    void RF09_CP02() throws Exception {
        // ARRANGE
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "ENSENAR",
            "nivel",
            ""
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            materiaService.subject(101L, entrada)
        );
        // ASSERT
        assertEquals("Revisa el campo nivel.", error.getReason());
    }

    @Test
    @DisplayName(
        "RF09_CP03 E3 RF-009-Error del sistema al guardar el nivel"
    )
    void RF09_CP03() throws Exception {
        // ARRANGE
        doThrow(new DataAccessResourceFailureException("guardar nivel"))
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
            "ENSENAR",
            "nivel",
            "AVANZADO"
        );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> materiaService.subject(101L, entrada)
        );
        // ASSERT
        assertEquals("guardar nivel", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF09_CP04 E4 RF-009-Cambio de nivel de una materia registrada"
    )
    void RF09_CP04() throws Exception {
        // ARRANGE
        when(materias.listarPropias(101L)).thenReturn(
            List.of(
                fila(
                    "materia_id",
                    10L,
                    "tipo_rol",
                    "ENSENAR",
                    "nivel",
                    "BASICO"
                )
            )
        );
        var entrada = fila(
            "materia_id",
            10L,
            "tipo_rol",
            "ENSENAR",
            "nivel",
            "AVANZADO"
        );
        // ACT
        materiaService.subject(101L, entrada);
        // ASSERT
        verify(materias).guardarPropia(
            101L,
            10L,
            "ENSENAR",
            "AVANZADO"
        );
    }
}
