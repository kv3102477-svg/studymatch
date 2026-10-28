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

class RF12Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF12_CP03 E3 RF-012-Error del sistema al consultar el perfil"
    )
    void RF12_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("consultar perfil")
        )
            .when(estudiantes)
            .perfilPublico(202L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> estudianteService.other(202L)
        );
        // ASSERT
        assertEquals("consultar perfil", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF12_CP04 E4 RF-012-Falla al cargar el detalle de una pestaña"
    )
    void RF12_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("cargar pestaña")
        )
            .when(materias)
            .listarPublicas(202L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> estudianteService.other(202L)
        );
        // ASSERT
        assertEquals("cargar pestaña", error.getMessage());
    }
}
