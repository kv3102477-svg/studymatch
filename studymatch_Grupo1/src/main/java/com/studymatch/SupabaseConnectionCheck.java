package com.studymatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Comprueba la conexión sin depender de tablas ni modificar datos. */
@Component
public class SupabaseConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SupabaseConnectionCheck.class);
    private final JdbcTemplate jdbc;

    public SupabaseConnectionCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        if (!Integer.valueOf(1).equals(result)) {
            throw new IllegalStateException("No se pudo verificar la conexión a PostgreSQL.");
        }
        log.info(
            "Conexión a PostgreSQL verificada (SELECT 1). Los datos se consultan en Supabase."
        );
    }
}
