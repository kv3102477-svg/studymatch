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

class RF13Test extends BaseTdd {

    @Test
    @DisplayName("RF13_CP01 E1 RF-013-CARGA DE DOCUMENTOS")
    void RF13_CP01() throws Exception {
        // ARRANGE
        var archivo =
            new org.springframework.mock.web.MockMultipartFile(
                "archivo",
                "material.pdf",
                "application/pdf",
                "%PDF-1.4\nContenido de prueba".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8
                )
            );
        // ACT
        long id = documentService.upload(101L, archivo);
        // ASSERT
        assertEquals(1L, id);
        verify(documentos).crear(
            eq(101L),
            eq("material.pdf"),
            eq("PDF"),
            any(),
            eq("pglo:1")
        );
    }

    @Test
    @DisplayName(
        "RF13_CP02 E2 RF-013-Formato o tamaño de archivo no permitido"
    )
    void RF13_CP02() throws Exception {
        // ARRANGE
        var archivo =
            new org.springframework.mock.web.MockMultipartFile(
                "archivo",
                "virus.exe",
                "application/octet-stream",
                new byte[] { 1, 2, 3 }
            );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            documentService.upload(101L, archivo)
        );
        // ASSERT
        assertEquals(
            "Solo se admiten PDF, DOCX y PPTX.",
            error.getReason()
        );
        verify(documentos, never()).guardarContenido(any());
    }

    @Test
    @DisplayName(
        "RF13_CP04 E4 RF-013-Error del sistema durante la carga"
    )
    void RF13_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("guardar archivo")
        )
            .when(documentos)
            .guardarContenido(any());
        var archivo =
            new org.springframework.mock.web.MockMultipartFile(
                "archivo",
                "material.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
            );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> documentService.upload(101L, archivo)
        );
        // ASSERT
        assertEquals("guardar archivo", error.getMessage());
        verify(documentos, never()).crear(
            anyLong(),
            anyString(),
            anyString(),
            any(),
            anyString()
        );
    }
}
