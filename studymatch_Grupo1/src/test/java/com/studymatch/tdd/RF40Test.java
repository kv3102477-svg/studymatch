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

class RF40Test extends BaseTdd {

    @Test
    @DisplayName("RF40_CP01 E1 RF-0040-Constancia de horas de mentoría")
    void RF40_CP01() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                2.0,
                "horas_aprendidas",
                1.0,
                "sesiones_completadas",
                3L
            )
        );
        // ACT
        byte[] pdf = progresoService.constanciaPdf(101L);
        // ASSERT
        assertEquals(
            "%PDF-",
            new String(
                pdf,
                0,
                5,
                java.nio.charset.StandardCharsets.US_ASCII
            )
        );
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertTrue(
                new org.apache.pdfbox.text.PDFTextStripper()
                    .getText(doc)
                    .contains("Ana Torres")
            );
        }
    }

    @Test
    @DisplayName(
        "RF40_CP02 E2 RF-0040-Constancia de horas de mentoría - Sin horas acumuladas"
    )
    void RF40_CP02() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                0.0,
                "horas_aprendidas",
                0.0,
                "sesiones_completadas",
                0L
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            progresoService.constanciaPdf(101L)
        );
        // ASSERT
        assertEquals(
            "Aún no tienes horas acumuladas para emitir una constancia.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF40_CP03 E3 RF-0040-Constancia de horas de mentoría - Error en compilación del PDF"
    )
    void RF40_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("consultar horas")
        )
            .when(sesiones)
            .horasAcumuladas(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> progresoService.constanciaPdf(101L)
        );
        // ASSERT
        assertEquals("consultar horas", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF40_CP04 E4 RF-0040-Constancia de horas de mentoría - Horas con inconsistencia"
    )
    void RF40_CP04() throws Exception {
        // ARRANGE
        when(sesiones.horasAcumuladas(101L)).thenReturn(
            fila(
                "horas_ensenadas",
                -1.0,
                "horas_aprendidas",
                0.0,
                "sesiones_completadas",
                0L,
                "en_revision",
                true
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            progresoService.constanciaPdf(101L)
        );
        // ASSERT
        assertEquals(
            "Tus horas están en revisión. Podrás descargar tu constancia cuando se validen",
            error.getReason()
        );
    }
}
