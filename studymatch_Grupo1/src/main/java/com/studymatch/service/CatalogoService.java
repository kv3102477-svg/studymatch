package com.studymatch.service;

import com.studymatch.dao.CatalogoDAO;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Entrega las carreras y ciclos disponibles para los formularios. */
@Service
public class CatalogoService {

    private final CatalogoDAO catalogoDAO;

    public CatalogoService(CatalogoDAO catalogoDAO) {
        this.catalogoDAO = catalogoDAO;
    }

    public Object cycle() {
        return catalogoDAO.cicloAcademicoActivo();
    }

    /** Entrega las opciones disponibles para el registro de estudiantes. */
    public Map<String, Object> catalogs() {
        return Map.of(
            "carreras",
            catalogoDAO.carrerasActivas(),
            "ciclos",
            catalogoDAO.ciclosEstudio()
        );
    }
}
