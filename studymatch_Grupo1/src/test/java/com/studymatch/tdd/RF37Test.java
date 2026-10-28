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

class RF37Test extends BaseTdd {

    @Test
    @DisplayName("RF37_CP03 E3 RF-0037- Error de conexión a BD")
    void RF37_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "consultar suscripción"
            )
        )
            .when(suscripciones)
            .listarDelEstudiante(101L);
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> suscripcionService.subscriptions(101L)
        );
        // ASSERT
        assertEquals("consultar suscripción", error.getMessage());
    }
}
