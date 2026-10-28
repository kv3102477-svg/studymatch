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

class RF20Test extends BaseTdd {

    @Test
    @DisplayName("RF20_CP01 E1 RF-0020-Tiempo de emparejamiento")
    void RF20_CP01() throws Exception {
        // ARRANGE
        var buscador = mock(
            com.studymatch.service.BusquedaService.class
        );
        when(buscador.search(101L, "", true)).thenReturn(
            List.of(fila("id", 202L, "coincidencia_materias", 100L))
        );
        var ticks = new java.util.concurrent.atomic.AtomicLong();
        var service = new com.studymatch.service.EmparejamientoService(
            buscador,
            materiaService,
            matches,
            () -> ticks.getAndAdd(1000000L)
        );
        // ACT
        var salida = service.buscar(101L);
        // ASSERT
        assertEquals(1L, salida.get("id"));
        assertEquals(1L, salida.get("tiempo_ms"));
        verify(matches).crear(101L, 202L, 10L, 100);
    }

    @Test
    @DisplayName("RF20_CP02 E2 RF-0020-Tiempo de Búsqueda Excedido")
    void RF20_CP02() throws Exception {
        // ARRANGE
        var buscador = mock(
            com.studymatch.service.BusquedaService.class
        );
        when(buscador.search(101L, "", true)).thenReturn(
            List.of(fila("id", 202L, "coincidencia_materias", 100L))
        );
        var ticks = new java.util.concurrent.atomic.AtomicLong();
        var service = new com.studymatch.service.EmparejamientoService(
            buscador,
            materiaService,
            matches,
            () -> ticks.getAndAdd(901000000000L)
        );
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            service.buscar(101L)
        );
        // ASSERT
        assertEquals(
            "La búsqueda superó el límite de 15 minutos. Inténtalo nuevamente.",
            error.getReason()
        );
        verify(matches, never()).crear(
            anyLong(),
            anyLong(),
            anyLong(),
            anyInt()
        );
    }

    @Test
    @DisplayName(
        "RF20_CP03 E3 RF-0020-No Existen Compañeros Disponibles"
    )
    void RF20_CP03() throws Exception {
        // ARRANGE
        var buscador = mock(
            com.studymatch.service.BusquedaService.class
        );
        when(buscador.search(101L, "", true)).thenReturn(List.of());
        var service = new com.studymatch.service.EmparejamientoService(
            buscador,
            materiaService,
            matches,
            () -> 0L
        );
        // ACT
        var salida = service.buscar(101L);
        // ASSERT
        assertEquals(false, salida.get("encontrado"));
        verify(matches, never()).crear(
            anyLong(),
            anyLong(),
            anyLong(),
            anyInt()
        );
    }

    @Test
    @DisplayName(
        "RF20_CP04 E4 RF-0020-Sobrecarga del Servidor / Error de Procesamiento"
    )
    void RF20_CP04() throws Exception {
        // ARRANGE
        var buscador = mock(
            com.studymatch.service.BusquedaService.class
        );
        doThrow(
            new DataAccessResourceFailureException(
                "buscar emparejamiento"
            )
        )
            .when(buscador)
            .search(101L, "", true);
        var service = new com.studymatch.service.EmparejamientoService(
            buscador,
            materiaService,
            matches,
            () -> 0L
        );
        // ACT
        var error = assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.buscar(101L)
        );
        // ASSERT
        assertEquals("buscar emparejamiento", error.getMessage());
        verify(matches, never()).crear(
            anyLong(),
            anyLong(),
            anyLong(),
            anyInt()
        );
    }
}
