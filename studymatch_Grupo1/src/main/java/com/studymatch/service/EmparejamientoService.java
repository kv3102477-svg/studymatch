package com.studymatch.service;
import com.studymatch.dao.EmparejamientoDAO;
import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Service;
@Service
public class EmparejamientoService {
    private final BusquedaService busqueda; private final MateriaService materias; private final EmparejamientoDAO datos; private final LongSupplier tiempo;
    @org.springframework.beans.factory.annotation.Autowired
    public EmparejamientoService(BusquedaService busqueda,MateriaService materias,EmparejamientoDAO datos){this(busqueda,materias,datos,System::nanoTime);}
    public EmparejamientoService(BusquedaService busqueda,MateriaService materias,EmparejamientoDAO datos,LongSupplier tiempo){this.busqueda=busqueda;this.materias=materias;this.datos=datos;this.tiempo=tiempo;}
    @org.springframework.transaction.annotation.Transactional
    public Map<String,Object> buscar(long estudiante){
        long inicio=tiempo.getAsLong();
        var candidatos=busqueda.search(estudiante,"",true);
        long milisegundos=(tiempo.getAsLong()-inicio)/1_000_000;
        if(milisegundos>=900_000)throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "La búsqueda superó el límite de 15 minutos. Inténtalo nuevamente.");
        if(candidatos.isEmpty())return Map.of("encontrado",false,"tiempo_ms",milisegundos);
        var candidato=candidatos.get(0);
        long receptor=((Number)candidato.get("id")).longValue();
        var coincidencias=materias.matchingSubjects(estudiante,receptor);
        if(coincidencias.isEmpty())return Map.of("encontrado",false,"tiempo_ms",milisegundos);
        long materia=((Number)coincidencias.get(0).get("id")).longValue();
        int porcentaje=((Number)candidato.get("coincidencia_materias")).intValue();
        long id=datos.crear(estudiante,receptor,materia,Math.max(0,Math.min(100,porcentaje)));
        return Map.of("id",id,"encontrado",true,"tiempo_ms",milisegundos,"companero",candidato);
    }
}
