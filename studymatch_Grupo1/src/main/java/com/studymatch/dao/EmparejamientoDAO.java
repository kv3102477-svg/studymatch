package com.studymatch.dao;
/** Registra coincidencias reales sin duplicar la propuesta pendiente de la misma pareja y materia. */
public interface EmparejamientoDAO { long crear(long proponente,long receptor,long materia,int porcentaje);
    int responder(long estudiante,long companero,long materia,boolean aceptar);
}
