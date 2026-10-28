package com.studymatch.dao.jdbc;

import com.studymatch.dao.DocumentoDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa DocumentoDAO con consultas a PostgreSQL. */
@Repository
public class JdbcDocumentoDAO implements DocumentoDAO {

    private final SqlExecutor sql;

    public JdbcDocumentoDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> listarDelEstudiante(long estudianteId) {
        return sql.list(
            "SELECT id,nombre_archivo,formato,tamano_mb,subido_en FROM documento_apoyo WHERE " +
                "estudiante_id=? ORDER BY subido_en DESC",
            estudianteId
        );
    }

    @Override
    public Map<String, Object> guardarContenido(byte[] contenido) {
        return sql.one("SELECT lo_from_bytea(0,?) AS oid", contenido);
    }

    @Override
    public Map<String, Object> eliminarContenido(long archivoOid) {
        return sql.one("SELECT lo_unlink(CAST(? AS oid))", archivoOid);
    }

    @Override
    public long crear(
        long estudianteId,
        String nombre,
        String formato,
        java.math.BigDecimal tamanoMb,
        String ubicacion
    ) {
        return sql.insert(
            "INSERT INTO " +
                "documento_apoyo(estudiante_id,nombre_archivo,formato,tamano_mb,url_archivo) VALUES " +
                "(?,?,?,?,?)",
            estudianteId,
            nombre,
            formato,
            tamanoMb,
            ubicacion
        );
    }

    @Override
    public Map<String, Object> buscarPropio(long documentoId, long estudianteId) {
        return sql.one(
            "SELECT * FROM documento_apoyo WHERE id=? AND estudiante_id=?",
            documentoId,
            estudianteId
        );
    }

    @Override
    public Map<String, Object> leerContenido(long archivoOid) {
        return sql.one("SELECT lo_get(CAST(? AS oid)) AS contenido", archivoOid);
    }

    @Override
    public Map<String, Object> bloquearPropio(long documentoId, long estudianteId) {
        return sql.one(
            "SELECT url_archivo FROM documento_apoyo WHERE id=? AND estudiante_id=? FOR UPDATE",
            documentoId,
            estudianteId
        );
    }

    @Override
    public int eliminar(long documentoId) {
        return sql.update("DELETE FROM documento_apoyo WHERE id=?", documentoId);
    }
}
