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

class RF49Test extends BaseTdd {

    @Test
    @DisplayName("RF49_CP01 E1 RF-0049-Generación de certificado")
    void RF49_CP01() throws Exception {
        // ARRANGE
        when(certificados.buscar(101L, 1L)).thenReturn(List.of());
        // ACT
        var salida = certificadoService.emitir(101L, 1L);
        // ASSERT
        assertNotNull(salida.get("codigo"));
        assertEquals(1, certificadosGuardados.size());
        assertEquals(1, avisosGuardados.size());
        verify(certificados).registrarFirma(
            eq(1L),
            argThat(firma -> firma.length > 0),
            argThat(clave -> clave.length > 0)
        );
        try (
            var documento = org.apache.pdfbox.Loader.loadPDF(
                (byte[]) certificadosGuardados
                    .values()
                    .iterator()
                    .next()
                    .get("contenido")
            )
        ) {
            assertTrue(
                documento
                    .getPage(0)
                    .getResources()
                    .getXObjectNames()
                    .iterator()
                    .hasNext()
            );
        }
    }

    @Test
    @DisplayName(
        "RF49_CP02 E2 RF-0049-Generación de certificado - Sin horas acumuladas global"
    )
    void RF49_CP02() throws Exception {
        // ARRANGE
        when(certificados.candidatos(1L)).thenReturn(List.of());
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            certificadoService.emitir(101L, 1L)
        );
        // ASSERT
        assertEquals(
            "No alcanzaste las horas mínimas para el certificado de este ciclo",
            error.getReason()
        );
        verify(certificados, never()).guardar(
            anyLong(),
            anyLong(),
            anyString(),
            anyDouble(),
            any(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF49_CP03 E3 RF-0049-Generación de certificado - - Inconsistencia en un alumno"
    )
    void RF49_CP03() throws Exception {
        // ARRANGE
        when(certificados.candidatos(1L)).thenReturn(
            List.of(
                fila(
                    "estudiante_id",
                    101L,
                    "nombre",
                    "",
                    "carrera",
                    "Ingeniería de Sistemas",
                    "horas",
                    10.0
                )
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            certificadoService.emitir(101L, 1L)
        );
        // ASSERT
        assertEquals(
            "Completa los datos del estudiante antes de emitir el certificado.",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF49_CP04 E4 RF-0049-Generación de certificado - Horas por debajo del mínimo"
    )
    void RF49_CP04() throws Exception {
        // ARRANGE
        when(certificados.candidatos(1L)).thenReturn(
            List.of(
                fila(
                    "estudiante_id",
                    101L,
                    "nombre",
                    "Ana Torres",
                    "carrera",
                    "Ingeniería de Sistemas",
                    "horas",
                    4.0
                )
            )
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            certificadoService.emitir(101L, 1L)
        );
        // ASSERT
        assertEquals(
            "No alcanzaste las horas mínimas para el certificado de este ciclo",
            error.getReason()
        );
        assertEquals(1, avisosGuardados.size());
    }
}
