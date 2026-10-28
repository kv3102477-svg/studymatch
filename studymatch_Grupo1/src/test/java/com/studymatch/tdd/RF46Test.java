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

class RF46Test extends BaseTdd {

    @Test
    @DisplayName("RF46_CP01 E1 RF-0046-Notificación de mensajes")
    void RF46_CP01() throws Exception {
        // ARRANGE
        var entrada = fila("texto", "Mensaje de prueba");
        // ACT
        conversacionService.message(101L, 1L, entrada);
        // ASSERT
        assertEquals(1, avisosGuardados.size());
        assertEquals(
            "Nuevo mensaje",
            avisosGuardados.get(0).get("titulo")
        );
    }

    @Test
    @DisplayName(
        "RF46_CP02 E2 RF-0046-Notificación de mensajes - Push desactivadas"
    )
    void RF46_CP02() throws Exception {
        // ARRANGE
        var receptor = perfil(202L);
        receptor.put("push_activo", false);
        doReturn(receptor).when(estudiantes).perfil(202L);
        // ACT
        conversacionService.message(
            101L,
            1L,
            fila("texto", "Mensaje de prueba")
        );
        // ASSERT
        assertEquals(1, avisosGuardados.size());
        assertEquals(202L, avisosGuardados.get(0).get("receptor_id"));
    }

    @Test
    @DisplayName(
        "RF46_CP03 E3 RF-0046-Notificación de mensajes - Usuario ya está visualizando el chat"
    )
    void RF46_CP03() throws Exception {
        // ARRANGE
        conversacionService.messages(202L, 1L);
        var entrada = fila("texto", "Mensaje con chat abierto");
        // ACT
        conversacionService.message(101L, 1L, entrada);
        // ASSERT
        verify(conversaciones).crearMensaje(
            1L,
            101L,
            "Mensaje con chat abierto"
        );
        assertTrue(avisosGuardados.isEmpty());
    }

    @Test
    @DisplayName(
        "RF46_CP04 E4 RF-0046-Notificación de mensajes - Destinatario fuera de línea"
    )
    void RF46_CP04() throws Exception {
        // ARRANGE
        var receptor = perfil(202L);
        receptor.put("en_linea", false);
        doReturn(receptor).when(estudiantes).perfil(202L);
        // ACT
        conversacionService.message(
            101L,
            1L,
            fila("texto", "Lo leerás después")
        );
        // ASSERT
        assertEquals(1, avisosGuardados.size());
        verify(conversaciones).crearMensaje(
            1L,
            101L,
            "Lo leerás después"
        );
    }
}
