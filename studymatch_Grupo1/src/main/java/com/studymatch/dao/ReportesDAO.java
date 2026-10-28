package com.studymatch.dao;
import java.time.LocalDate;
import java.util.*;
/** Consultas de datos sin cálculos ni decisiones de negocio. */
public interface ReportesDAO {
    List<Map<String,Object>> sesiones(LocalDate desde,LocalDate hasta,Long materiaId);
    List<Map<String,Object>> emparejamientos(LocalDate desde,LocalDate hasta);
    List<Map<String,Object>> calificaciones(LocalDate desde,LocalDate hasta);
    Set<Long> estudiantesActivos(LocalDate desde,LocalDate hasta);
}
