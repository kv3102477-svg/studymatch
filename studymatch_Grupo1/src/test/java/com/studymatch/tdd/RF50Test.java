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

class RF50Test extends BaseTdd {

    @Test
    @DisplayName("RF50_CP01 E1 RF-0050-Descarga de certificado PDF")
    void RF50_CP01() throws Exception {
        // ARRANGE
        var emitido = certificadoService.emitir(101L, 1L);
        String codigo = emitido.get("codigo").toString();
        // ACT
        byte[] contenido = certificadoService.descargar(101L, codigo);
        // ASSERT
        assertTrue(contenido.length > 100);
        assertEquals(
            "%PDF-",
            new String(
                contenido,
                0,
                5,
                java.nio.charset.StandardCharsets.US_ASCII
            )
        );
        try (var doc = org.apache.pdfbox.Loader.loadPDF(contenido)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName(
        "RF50_CP02 E2 RF-0050-Descarga de certificado PDF - No generado aún"
    )
    void RF50_CP02() throws Exception {
        // ARRANGE
        doThrow(
            new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Aún no se ha generado tu certificado."
            )
        )
            .when(certificados)
            .buscarCodigo("no-existe");
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            certificadoService.descargar(101L, "no-existe")
        );
        // ASSERT
        assertEquals(
            "Aún no se ha generado tu certificado.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF50_CP03 E3 RF-0050-Descarga de certificado PDF - Error en PDF o Conexión"
    )
    void RF50_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "descargar certificado"
            )
        )
            .when(certificados)
            .buscarCodigo("abc");
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> certificadoService.descargar(101L, "abc")
        );
        // ASSERT
        assertEquals("descargar certificado", error.getMessage());
    }
}
