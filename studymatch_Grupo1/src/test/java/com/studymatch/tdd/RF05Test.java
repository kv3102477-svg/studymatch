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

class RF05Test extends BaseTdd {

    @Test
    @DisplayName("RF05_CP01 E1 RF-005-EDICIÓN DE PERFIL")
    void RF05_CP01() throws Exception {
        // ARRANGE
        var entrada = fila(
            "nombre",
            "Ana Actualizada",
            "carrera",
            "Ingeniería de Sistemas",
            "ciclo",
            "Ciclo 3",
            "bio",
            "Mentora",
            "push_activo",
            true
        );
        // ACT
        estudianteService.updateProfile(101L, entrada);
        // ASSERT
        verify(estudiantes).actualizarPerfil(
            "Ana Actualizada",
            "Ingeniería de Sistemas",
            "Ciclo 3",
            "Mentora",
            true,
            101L
        );
    }

    @Test
    @DisplayName("RF05_CP02 E2 RF-005-Formato de campo inválido")
    void RF05_CP02() throws Exception {
        // ARRANGE
        var entrada = fila(
            "nombre",
            "",
            "carrera",
            "Ingeniería de Sistemas",
            "ciclo",
            "Ciclo 3"
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            estudianteService.updateProfile(101L, entrada)
        );
        // ASSERT
        assertEquals("Revisa el campo nombre.", error.getReason());
        verify(estudiantes, never()).actualizarPerfil(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyBoolean(),
            anyLong()
        );
    }

    @Test
    @DisplayName(
        "RF05_CP04 E4 RF-005-Error del sistema al guardar los cambios"
    )
    void RF05_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("guardar perfil")
        )
            .when(estudiantes)
            .actualizarPerfil(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyLong()
            );
        var entrada = fila(
            "nombre",
            "Ana Torres",
            "carrera",
            "Ingeniería de Sistemas",
            "ciclo",
            "Ciclo 3"
        );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> estudianteService.updateProfile(101L, entrada)
        );
        // ASSERT
        assertEquals("guardar perfil", error.getMessage());
    }
}
