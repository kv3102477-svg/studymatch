package com.studymatch.dao.jdbc;

import com.studymatch.dao.CatalogoDAO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Implementa CatalogoDAO con consultas a PostgreSQL. */
@Repository
public class JdbcCatalogoDAO implements CatalogoDAO {

    private final SqlExecutor sql;

    public JdbcCatalogoDAO(SqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> cicloAcademicoActivo() {
        return sql.list(
            "SELECT id,nombre FROM ciclo_academico WHERE activo=true ORDER BY id DESC LIMIT 1"
        );
    }

    @Override
    public List<Map<String, Object>> carrerasActivas() {
        return sql.list("SELECT id,nombre FROM carrera WHERE activo=true ORDER BY nombre");
    }

    @Override
    public List<Map<String, Object>> ciclosEstudio() {
        return sql.list(
            "SELECT id,nombre FROM ciclo_estudio WHERE activo=true AND nombre IN ('Ciclo " +
                "1','Ciclo 2','Ciclo 3','Ciclo 4','Ciclo 5','Ciclo 6','Ciclo 7','Ciclo 8','Ciclo " +
                "9','Ciclo 10') ORDER BY orden,nombre"
        );
    }

    @Override
    public List<Map<String, Object>> buscarOpcion(String catalogo, String nombre) {
        return sql.list(
            switch (catalogo) {
                case "carrera" -> "SELECT activo FROM carrera WHERE nombre=? FOR KEY SHARE";
                case "ciclo" -> "SELECT activo FROM ciclo_estudio WHERE nombre=? FOR KEY SHARE";
                default -> throw new IllegalArgumentException("Catálogo no válido.");
            },
            nombre
        );
    }
}
