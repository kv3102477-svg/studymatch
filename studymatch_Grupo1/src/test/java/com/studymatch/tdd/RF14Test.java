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

class RF14Test extends BaseTdd {

    @Test
    @DisplayName("RF14_CP01 E1 RF-014-SUGERENCIA DE COMPAÑEROS")
    void RF14_CP01() throws Exception {
        // ARRANGE
        when(
            estudiantes.buscarCompaneros(
                anyLong(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()
            )
        ).thenReturn(
            new ArrayList<>(
                List.of(
                    fila(
                        "id",
                        202L,
                        "nombre",
                        "Luis Rojas",
                        "dias_disponibles",
                        "2",
                        "minutos_en_comun",
                        60
                    )
                )
            )
        );
        when(materias.listarParaComparacion(any())).thenReturn(
            List.of(
                fila(
                    "estudiante_id",
                    101L,
                    "materia_id",
                    10L,
                    "tipo_rol",
                    "APRENDER",
                    "nombre",
                    "Programación Web"
                ),
                fila(
                    "estudiante_id",
                    202L,
                    "materia_id",
                    10L,
                    "tipo_rol",
                    "ENSENAR",
                    "nombre",
                    "Programación Web"
                )
            )
        );
        // ACT
        var salida = busquedaService.search(101L, "", true);
        // ASSERT
        assertEquals(1, salida.size());
        assertEquals(100L, salida.get(0).get("coincidencia_materias"));
        assertEquals(
            List.of("Programación Web"),
            salida.get(0).get("materias_ensenar")
        );
    }

    @Test
    @DisplayName(
        "RF14_CP02 E2 RF-014-No existen coincidencias suficientes"
    )
    void RF14_CP02() throws Exception {
        // ARRANGE
        when(
            estudiantes.buscarCompaneros(
                anyLong(),
                anyBoolean(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()
            )
        ).thenReturn(List.of());
        // ACT
        var salida = busquedaService.search(101L, "", true);
        // ASSERT
        assertTrue(salida.isEmpty());
    }

    @Test
    @DisplayName(
        "RF14_CP03 E3 RF-014-Perfil del estudiante con información insuficiente"
    )
    void RF14_CP03() throws Exception {
        // ARRANGE
        when(materias.listarPropias(101L)).thenReturn(List.of());
        // ACT
        var error = assertThrows(ResponseStatusException.class, () ->
            busquedaService.search(101L, "", true)
        );
        // ASSERT
        assertEquals(
            "Completa tus materias y disponibilidad para ver sugerencias",
            error.getReason()
        );
    }

    @Test
    @DisplayName(
        "RF14_CP04 E4 RF-014-Error del sistema al generar las sugerencias"
    )
    void RF14_CP04() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "generar sugerencias"
            )
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
            () -> busquedaService.search(101L, "", true)
        );
        // ASSERT
        assertEquals("generar sugerencias", error.getMessage());
    }
}
