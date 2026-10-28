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

class RF16Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF16_CP04 E4 RF-016-Error del sistema al aplicar los filtros"
    )
    void RF16_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("filtrar compañeros")
        )
            .when(estudiantes)
            .buscarCompaneros(
                anyLong(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()
            );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () ->
                busquedaService.search(
                    101L,
                    "",
                    false,
                    "Web",
                    2,
                    "Ingeniería de Sistemas",
                    "APRENDER"
                )
        );
        // ASSERT
        assertEquals("filtrar compañeros", error.getMessage());
    }
}
