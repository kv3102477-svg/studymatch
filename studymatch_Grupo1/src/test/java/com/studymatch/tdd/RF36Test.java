package com.studymatch.tdd;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessResourceFailureException;
/** No se realizan cobros reales. Se verifican los efectos del pago de demostración. */
class RF36Test extends BaseTdd {

    private Map<String,Object> entrada(String estado) {
        return fila("plan_id",1L,"escenario",estado,"referencia",UUID.randomUUID().toString(),
            "tarjeta_prueba","4111111111111111");
    }

    @Test void RF36_CP01() {
        // ARRANGE
        var datos=entrada("APROBADO");
        // ACT
        var salida=(Map<?,?>)suscripcionService.paymentDemo(101L,datos);
        // ASSERT
        assertEquals("APROBADO",salida.get("estado"));
        assertTrue(salida.get("mensaje").toString().contains("No se cobró"));
        verify(suscripciones).activarPlan(101L,1L,126);
    }
    @Test void RF36_CP02() {
        // ARRANGE
        var datos=entrada("RECHAZADO");
        // ACT
        var salida=(Map<?,?>)suscripcionService.paymentDemo(101L,datos);
        // ASSERT
        assertEquals("RECHAZADO",salida.get("estado"));
        verify(suscripciones).registrarPagoDemo(eq(101L),eq(1L),anyString(),any(),eq("PEN"),eq("RECHAZADO"),anyString());
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
    @Test void RF36_CP03() {
        // ARRANGE
        var datos=entrada("TIMEOUT");
        // ACT
        var salida=(Map<?,?>)suscripcionService.paymentDemo(101L,datos);
        // ASSERT
        assertEquals("TIMEOUT",salida.get("estado"));
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
    @Test void RF36_CP04() {
        // ARRANGE: número diferente a la tarjeta permitida en esta demostración.
        var datos=entrada("APROBADO");datos.put("tarjeta_prueba","123");
        // ACT
        var error=assertThrows(ResponseStatusException.class,()->suscripcionService.paymentDemo(101L,datos));
        // ASSERT
        assertEquals("Usa únicamente la tarjeta de prueba 4111 1111 1111 1111.",error.getReason());
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
    @Test void pagoRepetidoNoDuplicaSuscripcion() {
        // ARRANGE
        var datos=entrada("APROBADO");
        when(suscripciones.pagoDemoAnterior(eq(101L),anyString()))
            .thenReturn(List.of(fila("id",9L,"plan_id",1L,"estado","COMPLETADO")));
        // ACT
        var salida=(Map<?,?>)suscripcionService.paymentDemo(101L,datos);
        // ASSERT
        assertEquals("APROBADO",salida.get("estado"));
        verify(suscripciones,never()).registrarPagoDemo(anyLong(),anyLong(),anyString(),any(),anyString(),anyString(),anyString());
        verify(suscripciones,never()).activarPlan(anyLong(),anyLong(),anyInt());
    }
}
