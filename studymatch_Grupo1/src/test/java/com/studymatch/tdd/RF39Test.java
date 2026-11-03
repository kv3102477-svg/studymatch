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

class RF39Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF39_CP01 E1 RF-0039-Generación de comprobante de pago"
    )
    void RF39_CP01() throws Exception {
        // ARRANGE
        var generador = new com.studymatch.service.PdfService();
        // ACT
        byte[] pdf = suscripcionService.comprobantePdf(101L, 1L);
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
                    .contains("SM-0001")
            );
        }
    }

    @Test
    @DisplayName(
        "RF39_CP02 E2 RF-0039-Generación de comprobante de pago- Falla al generar el PDF"
    )
    void RF39_CP02() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "leer datos del comprobante"
            )
        )
            .when(suscripciones)
            .comprobante(1L, 101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> suscripcionService.comprobantePdf(101L, 1L)
        );
        // ASSERT
        assertEquals("leer datos del comprobante", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF39_CP03 E3 RF-0039-Generación de comprobante de pago - Abortado por pago previo rechazado"
    )
    void RF39_CP03() throws Exception {
        // ARRANGE
        when(suscripciones.comprobante(1L, 101L)).thenReturn(
            fila("id", 1L, "estado", "RECHAZADO")
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            suscripcionService.comprobantePdf(101L, 1L)
        );
        // ASSERT
        assertEquals(
            "Solo se emiten comprobantes de pagos completados.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF39_CP04 E4 RF-0039-Generación de comprobante de pago - Comprobante ya emitido"
    )
    void RF39_CP04() throws Exception {
        // ARRANGE
        var primer = (Map<?, ?>) suscripcionService.receipt(101L, 1L);
        // ACT
        var segundo = (Map<?, ?>) suscripcionService.receipt(101L, 1L);
        // ASSERT
        assertEquals(
            primer.get("numero_comprobante"),
            segundo.get("numero_comprobante")
        );
        verify(suscripciones, never()).crearPrueba(
            anyLong(),
            anyLong()
        );
    }
}
