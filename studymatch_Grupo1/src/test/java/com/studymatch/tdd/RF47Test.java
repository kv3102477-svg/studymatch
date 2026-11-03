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

class RF47Test extends BaseTdd {

    @Test
    @DisplayName(
        "RF47_CP03 E3 RF-0047-Sección de ayuda - Formulario incompleto a soporte"
    )
    void RF47_CP03() throws Exception {
        // ARRANGE
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Cuenta y Perfil",
            "",
            "",
            null
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            supportService.create(101L, entrada, false)
        );
        // ASSERT
        assertEquals("Revisa el campo mensaje.", error.getReason());
        verify(soporte, never()).crear(
            anyLong(),
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName(
        "RF47_CP04 E4 RF-0047-Sección de ayuda - Error al registrar el ticket"
    )
    void RF47_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException("registrar ticket")
        )
            .when(soporte)
            .crear(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            );
        var entrada = new com.studymatch.model.SolicitudSoporte(
            "Cuenta y Perfil",
            "Ayuda",
            "No puedo editar mi perfil",
            null
        );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> supportService.create(101L, entrada, false)
        );
        // ASSERT
        assertEquals("registrar ticket", error.getMessage());
    }
}
