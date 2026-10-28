package com.studymatch;

import static org.junit.jupiter.api.Assertions.*;
import com.studymatch.dao.SuscripcionDAO;
import com.studymatch.dao.jdbc.SqlExecutor;
import com.studymatch.service.SuscripcionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba escrituras reales en Supabase. Spring revierte cada prueba al terminar. */
@SpringBootTest(properties="studymatch.jobs.enabled=false")
@Transactional
@EnabledIfEnvironmentVariable(named="STUDYMATCH_DB_TESTS",matches="true")
class PlanesDatabaseTest {
    @Autowired SuscripcionService servicio;
    @Autowired SuscripcionDAO datos;
    @Autowired SqlExecutor sql;

    private long estudiante() {
        return ((Number)sql.one("SELECT id FROM estudiante WHERE lower(correo)='u27912045@utp.edu.pe'")
            .get("id")).longValue();
    }

    private long plan() {
        return ((Number)sql.one("SELECT id FROM plan WHERE precio>0 AND periodo='18 semanas' AND activo=true ORDER BY id LIMIT 1")
            .get("id")).longValue();
    }

    private Map<String,Object> pago(String estado) {
        return new HashMap<>(Map.of("plan_id",plan(),"escenario",estado,"referencia",UUID.randomUUID().toString(),
            "tarjeta_prueba","4111111111111111","renovar",true));
    }

    @Test void aprobadoPersistePagoActivaProYNoDuplicaAlReintentar() {
        // ARRANGE
        long estudiante=estudiante();var entrada=pago("APROBADO");
        // ACT
        var salida=(Map<?,?>)servicio.paymentDemo(estudiante,entrada);
        servicio.paymentDemo(estudiante,entrada);
        // ASSERT: consulta real, no un mock.
        assertEquals("APROBADO",salida.get("estado"));
        assertTrue(datos.tienePro(estudiante));
        var pago=sql.one("SELECT estado,marca_tarjeta,monto FROM pago WHERE estudiante_id=? AND numero_comprobante=?",
            estudiante,"DEMO-"+entrada.get("referencia").toString().replace("-", "").substring(0,15));
        assertEquals("COMPLETADO",pago.get("estado"));
        assertEquals("SIMULADO",pago.get("marca_tarjeta"));
        assertEquals(1,datos.pagoDemoAnterior(estudiante,"DEMO-"+entrada.get("referencia").toString().replace("-", "").substring(0,15)).size());
        assertEquals(1,datos.buscarVigente(estudiante).size());
    }

    @Test void rechazoYDemoraConservanElPlanAnterior() {
        // ARRANGE
        long estudiante=estudiante();var antes=datos.listarDelEstudiante(estudiante);
        // ACT
        servicio.paymentDemo(estudiante,pago("RECHAZADO"));
        servicio.paymentDemo(estudiante,pago("TIMEOUT"));
        // ASSERT
        assertEquals(antes,datos.listarDelEstudiante(estudiante));
    }

    @Test void cancelarProVuelveAGratuitoYConservaHistorial() {
        // ARRANGE
        long estudiante=estudiante();servicio.paymentDemo(estudiante,pago("APROBADO"));
        long vigente=((Number)datos.buscarVigente(estudiante).get(0).get("id")).longValue();
        // ACT
        servicio.cancel(estudiante,vigente);
        // ASSERT
        assertFalse(datos.tienePro(estudiante));
        assertEquals("GRATUITO",datos.listarDelEstudiante(estudiante).get(0).get("estado"));
        assertTrue(datos.listarDelEstudiante(estudiante).stream().anyMatch(s->"EXPIRADO".equals(s.get("estado"))));
    }

    @Test void cambiarDeMensualACicloMantieneUnaSolaSuscripcionActiva() {
        // ARRANGE
        long estudiante=estudiante();var mensual=pago("APROBADO");
        long planMensual=((Number)sql.one("SELECT id FROM plan WHERE precio>0 AND periodo='1 mes' ORDER BY id LIMIT 1").get("id")).longValue();
        mensual.put("plan_id",planMensual);
        servicio.paymentDemo(estudiante,mensual);
        // ACT
        servicio.paymentDemo(estudiante,pago("APROBADO"));
        // ASSERT
        var activas=datos.buscarVigente(estudiante);
        assertEquals(1,activas.size());
        assertEquals(plan(),((Number)activas.get(0).get("plan_id")).longValue());
    }

    @Test void planVencidoNoConservaPermisosPro() {
        // ARRANGE
        long estudiante=estudiante();servicio.paymentDemo(estudiante,pago("APROBADO"));
        // ACT
        sql.update("UPDATE suscripcion SET fecha_inicio=CURRENT_DATE-30,fecha_vencimiento=CURRENT_DATE-1 WHERE estudiante_id=? AND estado='ACTIVO'",estudiante);
        // ASSERT
        assertFalse(datos.tienePro(estudiante));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->servicio.exigirPro(estudiante));
    }
}
