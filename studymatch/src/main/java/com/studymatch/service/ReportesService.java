package com.studymatch.service;
import com.studymatch.dao.ReportesDAO;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
/** Calcula reportes a partir de datos reales. No almacena cifras de demostración. */
@Service
public class ReportesService {
    private final ReportesDAO datos;
    public ReportesService(ReportesDAO datos){this.datos=datos;}
    private void validar(boolean autorizado,LocalDate desde,LocalDate hasta){
        if(!autorizado)throw new ResponseStatusException(FORBIDDEN,"No tienes permiso para consultar este reporte.");
        if(desde==null||hasta==null||desde.isAfter(hasta))throw new ResponseStatusException(BAD_REQUEST,"Revisa el periodo seleccionado.");
    }
    public Map<String,Object> asesorias(boolean autorizado,LocalDate desde,LocalDate hasta,Long materia){
        validar(autorizado,desde,hasta);
        var filas=datos.sesiones(desde,hasta,materia);
        Set<Long> participantes=new HashSet<>();
        long minutos=0;int incompletas=0;
        for(var fila:filas){
            participantes.add(((Number)fila.get("proponente_id")).longValue());
            participantes.add(((Number)fila.get("receptor_id")).longValue());
            if(fila.get("duracion_real_min") instanceof Number duracion && duracion.longValue()>=0)minutos+=duracion.longValue();
            else incompletas++;
        }
        return Map.of("asesorias",filas.size(),"horas",minutos/60.0,"participantes",participantes.size(),"sin_duracion",incompletas,"detalle",filas);
    }
    public Map<String,Object> emparejamientos(boolean autorizado,LocalDate desde,LocalDate hasta){
        validar(autorizado,desde,hasta);
        var actual=datos.emparejamientos(desde,hasta);
        LocalDate inicioAnterior=desde.minusMonths(1).withDayOfMonth(1);
        LocalDate finAnterior=desde.withDayOfMonth(1).minusDays(1);
        var anterior=datos.emparejamientos(inicioAnterior,finAnterior);
        long aceptados=actual.stream().filter(f->"ACEPTADO".equals(f.get("estado"))).count();
        long previos=anterior.stream().filter(f->"ACEPTADO".equals(f.get("estado"))).count();
        Map<String,Object> salida=new LinkedHashMap<>();
        salida.put("total",actual.size());salida.put("aceptados",aceptados);
        salida.put("sin_historico",previos==0);
        salida.put("crecimiento",previos==0?null:(aceptados-previos)*100.0/previos);
        salida.put("detalle",actual);
        return salida;
    }
    public Map<String,Object> satisfaccion(boolean autorizado,LocalDate desde,LocalDate hasta){
        validar(autorizado,desde,hasta);
        var sesiones=datos.sesiones(desde,hasta,null);
        Map<String,Object> salida=new LinkedHashMap<>();
        salida.put("nps",null);salida.put("promedio",null);salida.put("resenas",0);
        if(sesiones.isEmpty()){
            salida.put("mensaje","No hay sesiones completadas en el periodo seleccionado");return salida;
        }
        var filas=datos.calificaciones(desde,hasta);
        salida.put("resenas",filas.size());
        if(filas.size()<5){salida.put("mensaje","Datos aún insuficientes para un NPS representativo");return salida;}
        int promotores=0,detractores=0,total=0;
        for(var fila:filas){
            int estrellas=((Number)fila.get("estrellas")).intValue();
            if(estrellas<1||estrellas>5)throw new ResponseStatusException(CONFLICT,"Hay calificaciones fuera del rango permitido.");
            total+=estrellas;if(estrellas==5)promotores++;else if(estrellas<=3)detractores++;
        }
        salida.put("promedio",total/(double)filas.size());
        salida.put("nps",(promotores-detractores)*100.0/filas.size());
        salida.put("promotores",promotores);salida.put("detractores",detractores);
        salida.put("pasivos",filas.size()-promotores-detractores);
        salida.put("escala","Adaptación a estrellas: 5 promotor, 4 pasivo, 1 a 3 detractor. Mínimo 5 reseñas.");
        salida.put("mensaje","");return salida;
    }
    public Map<String,Object> retencion(boolean autorizado,LocalDate inicio1,LocalDate fin1,LocalDate inicio2,LocalDate fin2){
        validar(autorizado,inicio1,fin1);validar(autorizado,inicio2,fin2);
        if(!fin1.isBefore(inicio2))throw new ResponseStatusException(BAD_REQUEST,"Los periodos no deben superponerse.");
        if(!fin2.isBefore(LocalDate.now(ZoneId.of("America/Bogota"))))throw new ResponseStatusException(CONFLICT,"Disponible al concluir el segundo periodo seleccionado");
        var iniciales=datos.estudiantesActivos(inicio1,fin1);
        var finales=datos.estudiantesActivos(inicio2,fin2);
        Map<String,Object> salida=new LinkedHashMap<>();
        salida.put("iniciales",iniciales.size());salida.put("finales",finales.size());
        salida.put("tasa",iniciales.isEmpty()?null:Math.min(100.0,finales.size()*100.0/iniciales.size()));
        salida.put("mensaje",finales.size()>iniciales.size()?"Se sumaron estudiantes nuevos en el segundo periodo":iniciales.isEmpty()?"No existen estudiantes activos en el periodo inicial":"");
        return salida;
    }
}
