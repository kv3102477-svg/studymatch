package com.studymatch.dao.jdbc;
import com.studymatch.dao.CertificadoDAO;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcCertificadoDAO implements CertificadoDAO {
    private final SqlExecutor sql;public JdbcCertificadoDAO(SqlExecutor sql){this.sql=sql;}
    public List<Map<String,Object>> buscar(long estudiante,long ciclo){return sql.list("SELECT id,codigo,horas,emitido_en FROM certificado WHERE estudiante_id=? AND ciclo_id=?",estudiante,ciclo);}
    public List<Map<String,Object>> listar(long estudiante){return sql.list("SELECT id,codigo,horas,emitido_en,ciclo_id FROM certificado WHERE estudiante_id=? ORDER BY emitido_en DESC",estudiante);}
    public Map<String,Object> buscarCodigo(String codigo){return sql.one("SELECT * FROM certificado WHERE codigo=?",codigo);}
    public Map<String,Object> ciclo(long ciclo){return sql.one("SELECT id,nombre,fecha_fin FROM ciclo_academico WHERE id=? FOR UPDATE",ciclo);}
    public List<Map<String,Object>> candidatos(long ciclo){return sql.list("SELECT e.id AS estudiante_id,e.nombre,e.carrera,SUM(s.duracion_real_min)/60.0 AS horas FROM estudiante e "
        + "JOIN sesion s ON (s.mentor_id=e.id OR s.aprendiz_id=e.id) WHERE s.ciclo_id=? AND s.estado='COMPLETADA' "
        + "AND s.duracion_real_min>=15 AND s.revision_horas=false AND e.cuenta_activa=true GROUP BY e.id",ciclo);}
    public long guardar(long estudiante,long ciclo,String codigo,double horas,byte[] contenido,String huella){
        return sql.insert("INSERT INTO certificado(estudiante_id,ciclo_id,codigo,horas,contenido,huella) VALUES (?,?,?,?,?,?) "
            + "ON CONFLICT(estudiante_id,ciclo_id) DO UPDATE SET estudiante_id=EXCLUDED.estudiante_id",estudiante,ciclo,codigo,horas,contenido,huella);
    }
    @Override public void registrarFirma(long id,byte[] firma,byte[] clavePublica){sql.update("UPDATE certificado SET firma=?,clave_publica=? WHERE id=?",firma,clavePublica,id);}

    @Override public java.util.List<java.util.Map<String,Object>> ciclosCerradosActivos(){return sql.list("SELECT id FROM ciclo_academico WHERE activo=true AND fecha_fin<CURRENT_DATE ORDER BY id");}
    @Override public boolean avisoEmitido(long estudiante,long ciclo,String mensaje){return Boolean.TRUE.equals(sql.one("SELECT EXISTS(SELECT 1 FROM notificacion WHERE receptor_id=? AND referencia_id=? AND mensaje=?) AS avisado",estudiante,ciclo,mensaje).get("avisado"));}
}
