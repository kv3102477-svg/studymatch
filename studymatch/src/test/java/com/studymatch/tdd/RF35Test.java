package com.studymatch.tdd;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessResourceFailureException;
/** Contratación simulada: se comprueba el guardado y la activación, no solo el mensaje. */
class RF35Test extends BaseTdd {

    private Map<String,Object> entrada(String estado) {
        return fila("plan_id",1L,"escenario",estado,"referencia",UUID.randomUUID().toString(),
            "tarjeta_prueba","4111111111111111");
    }

    @Test void RF35_CP01() {
        // ARRANGE: cuenta gratuita y un plan con precio definido por el servidor.
        var datos=entrada("APROBADO");
        // ACT
        var salida=(Map<?,?>)suscripcionService.paymentDemo(101L,datos);
        // ASSERT
        assertEquals("APROBADO",salida.get("estado"));
        verify(suscripciones).registrarPagoDemo(eq(101L),eq(1L),eq("SEMESTRAL"),
            eq(new java.math.BigDecimal("20")),eq("PEN"),eq("COMPLETADO"),startsWith("DEMO-"));
        verify(suscripciones).activarPlan(101L,1L,126);
    }
    @Test void RF35_CP02() {
        // ARRANGE
        when(suscripciones.buscarVigente(101L)).thenReturn(List.of(fila("id",5L,"plan_id",1L)));
        // ACT
        var error=assertThrows(ResponseStatusException.class,()->suscripcionService.paymentDemo(101L,entrada("APROBADO")));
        // ASSERT
        assertEquals("Ya tienes este plan vigente. Confirma su renovación para continuar.",error.getReason());
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
    @Test void RF35_CP04() {
        // ARRANGE: falla la escritura de la contratación.
        when(suscripciones.registrarPagoDemo(anyLong(),anyLong(),anyString(),any(),anyString(),anyString(),anyString()))
            .thenThrow(new DataAccessResourceFailureException("guardar contratación"));
        // ACT
        assertThrows(DataAccessResourceFailureException.class,()->suscripcionService.paymentDemo(101L,entrada("APROBADO")));
        // ASSERT
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
}
