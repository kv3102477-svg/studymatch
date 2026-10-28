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

class RF27Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF27_CP03 E3 RF-0027-Error al cargar el historial y reintento"
    )
    void RF27_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("cargar historial")
        )
            .when(sesiones)
            .listarDelEstudiante(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> sesionService.sessions(101L)
        );
        // ASSERT
        assertEquals("cargar historial", error.getMessage());
    }
}
