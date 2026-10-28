package com.studymatch.dao.jdbc;
import com.studymatch.dao.ReportesDAO;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Repository;
/** Recupera los registros de los periodos solicitados con parámetros JDBC. */
@Repository
public class JdbcReportesDAO implements ReportesDAO {
    private final SqlExecutor sql;
    public JdbcReportesDAO(SqlExecutor sql){this.sql=sql;}
    public List<Map<String,Object>> sesiones(LocalDate desde,LocalDate hasta,Long materiaId){
        return sql.list("SELECT s.*,m.nombre AS materia,p.nombre AS proponente,r.nombre AS receptor "
            + "FROM sesion s JOIN materia m ON m.id=s.materia_id JOIN estudiante p ON p.id=s.proponente_id "
            + "JOIN estudiante r ON r.id=s.receptor_id WHERE s.estado='COMPLETADA' AND s.fecha_hora>=? "
            + "AND s.fecha_hora<? AND (CAST(? AS bigint) IS NULL OR s.materia_id=?) ORDER BY s.fecha_hora,s.id",
            desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay(),materiaId,materiaId);
    }
    public List<Map<String,Object>> emparejamientos(LocalDate desde,LocalDate hasta){
        return sql.list("SELECT * FROM emparejamiento WHERE creado_en>=? AND creado_en<? ORDER BY creado_en,id",
            desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
    }
    public List<Map<String,Object>> calificaciones(LocalDate desde,LocalDate hasta){
        return sql.list("SELECT c.estrellas,s.fecha_hora FROM calificacion c JOIN sesion s ON s.id=c.sesion_id "
            + "WHERE s.estado='COMPLETADA' AND s.fecha_hora>=? AND s.fecha_hora<?",desde.atStartOfDay(),hasta.plusDays(1).atStartOfDay());
    }
    public Set<Long> estudiantesActivos(LocalDate desde,LocalDate hasta){
        Set<Long> ids=new HashSet<>();
        for(var fila:sesiones(desde,hasta,null)){
            ids.add(((Number)fila.get("proponente_id")).longValue());ids.add(((Number)fila.get("receptor_id")).longValue());
        }return ids;
    }
}
