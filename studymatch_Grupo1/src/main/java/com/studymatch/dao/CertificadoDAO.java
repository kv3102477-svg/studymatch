package com.studymatch.dao;
import java.util.*;
/** Guarda los certificados emitidos; las descargas reutilizan el archivo original. */
public interface CertificadoDAO {
    List<Map<String,Object>> buscar(long estudianteId,long cicloId);
    List<Map<String,Object>> listar(long estudianteId);
    Map<String,Object> buscarCodigo(String codigo);
    Map<String,Object> ciclo(long cicloId);
    List<Map<String,Object>> candidatos(long cicloId);
    long guardar(long estudianteId,long cicloId,String codigo,double horas,byte[] contenido,String huella);
    void registrarFirma(long certificadoId,byte[] firma,byte[] clavePublica);
    java.util.List<java.util.Map<String,Object>> ciclosCerradosActivos();
    boolean avisoEmitido(long estudiante,long ciclo,String mensaje);
}
