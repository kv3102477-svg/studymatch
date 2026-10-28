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

class RF15Test extends BaseTdd {

    @Test
    @DisplayName("RF15_CP01 E1 RF-015-CÁLCULO DE COMPATIBILIDAD")
    void RF15_CP01() throws Exception {
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
        var salida = busquedaService.search(101L, "", false);
        // ASSERT
        assertEquals(100L, salida.get(0).get("coincidencia_materias"));
    }

    @Test
    @DisplayName(
        "RF15_CP02 E2 RF-015-Información insuficiente para el cálculo"
    )
    void RF15_CP02() throws Exception {
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
        when(materias.listarParaComparacion(any())).thenReturn(
            List.of()
        );
        // ACT
        var salida = busquedaService.search(101L, "", false);
        // ASSERT
        assertNull(salida.get(0).get("coincidencia_materias"));
    }

    @Test
    @DisplayName(
        "RF15_CP03 E3 RF-015-Error del sistema durante el cálculo"
    )
    void RF15_CP03() throws Exception {
        // ARRANGE
        doThrow(
            new DataAccessResourceFailureException(
                "calcular compatibilidad"
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
            () -> busquedaService.search(101L, "", false)
        );
        // ASSERT
        assertEquals("calcular compatibilidad", error.getMessage());
    }

    @Test
    @DisplayName(
        "RF15_CP04 E4 RF-015-Cálculo sin disponibilidad horaria"
    )
    void RF15_CP04() throws Exception {
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
        when(horarios.listar(101L)).thenReturn(List.of());
        // ACT
        var salida = busquedaService.search(101L, "", false);
        // ASSERT
        assertEquals(100L, salida.get(0).get("coincidencia_materias"));
    }
}
