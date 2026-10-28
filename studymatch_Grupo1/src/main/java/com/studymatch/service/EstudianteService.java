package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static com.studymatch.service.Validaciones.required;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.studymatch.dao.CatalogoDAO;
import com.studymatch.dao.DisponibilidadDAO;
import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.MateriaDAO;
import com.studymatch.dao.SuscripcionDAO;
import com.studymatch.model.Registro;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Gestiona el registro, el inicio de sesión y los cambios del perfil. */
@Service
public class EstudianteService {

    private final CatalogoDAO catalogoDAO;
    private final DisponibilidadDAO disponibilidadDAO;
    private final EstudianteDAO estudianteDAO;
    private final MateriaDAO materiaDAO;
    private final SuscripcionDAO suscripcionDAO;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);

    public EstudianteService(
        CatalogoDAO catalogoDAO,
        DisponibilidadDAO disponibilidadDAO,
        EstudianteDAO estudianteDAO,
        MateriaDAO materiaDAO,
        SuscripcionDAO suscripcionDAO
    ) {
        this.catalogoDAO = catalogoDAO;
        this.disponibilidadDAO = disponibilidadDAO;
        this.estudianteDAO = estudianteDAO;
        this.materiaDAO = materiaDAO;
        this.suscripcionDAO = suscripcionDAO;
    }

    /** Comprueba los límites de longitud admitidos para la contraseña. */
    private void password(String value) {
        if (
            value == null ||
            value.length() < 8 ||
            value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "La contraseña debe tener al menos 8 caracteres y no superar 72 bytes."
        );
    }

    /** Comprueba que la opción exista y esté disponible en el catálogo. */
    private String catalogChoice(String catalog, String value, String previous) {
        int max = catalog.equals("carrera") ? 100 : 20;
        String name = required(value, catalog, max);
        if (
            catalog.equals("ciclo") && !name.matches("Ciclo ([1-9]|10)")
        ) throw new ResponseStatusException(BAD_REQUEST, "Selecciona un ciclo del 1 al 10.");
        // El nombre funciona como clave de catálogo; la FK propaga sus cambios al perfil.
        var rows = catalogoDAO.buscarOpcion(catalog, name);
        if (
            rows.isEmpty() ||
            (!Boolean.TRUE.equals(rows.get(0).get("activo")) && !name.equals(previous))
        ) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona una opción disponible de " + catalog + "."
        );
        return name;
    }

    /** Valida los datos y crea al estudiante con una suscripción gratuita. */
    @Transactional
    public Map<String, Object> register(Registro r) {
        String email = required(r.correo(), "correo", 120).toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@utp\\.edu\\.pe$")) throw new ResponseStatusException(
            BAD_REQUEST,
            "Usa tu correo institucional terminado en @utp.edu.pe."
        );
        password(r.contrasena());
        String career = catalogChoice("carrera", r.carrera(), null);
        String cycle = catalogChoice("ciclo", r.ciclo(), null);
        long student = estudianteDAO.crear(
            email,
            passwords.encode(r.contrasena()),
            required(r.nombre(), "nombre", 100),
            career,
            cycle
        );
        suscripcionDAO.crearGratuita(student);
        return estudianteDAO.perfil(student);
    }

    /** Comprueba el correo y la contraseña, sin devolver el hash guardado. */
    public Map<String, Object> login(Map<String, String> input) {
        String email = text(input, "correo", 120).toLowerCase(Locale.ROOT);
        String raw = input.get("contrasena");
        var rows = estudianteDAO.credenciales(email);
        if (
            raw == null ||
            rows.isEmpty() ||
            !passwords.matches(raw, String.valueOf(rows.get(0).get("contrasena_hash")))
        ) throw new ResponseStatusException(UNAUTHORIZED, "Correo o contraseña incorrectos.");
        return estudianteDAO.perfil(((Number) id(rows.get(0), "id")).longValue());
    }

    /** Actualiza el perfil después de validar la carrera y el ciclo. */
    @Transactional
    public Map<String, Object> updateProfile(long student, Map<String, Object> input) {
        var current = estudianteDAO.perfil(student);
        String career = catalogChoice(
            "carrera",
            text(input, "carrera", 100),
            (String) current.get("carrera")
        );
        String cycle = catalogChoice(
            "ciclo",
            text(input, "ciclo", 20),
            (String) current.get("ciclo")
        );
        estudianteDAO.actualizarPerfil(
            text(input, "nombre", 100),
            career,
            cycle,
            (String) Objects.toString(input.get("bio"), "").substring(
                0,
                Math.min(Objects.toString(input.get("bio"), "").length(), 2000)
            ),
            Boolean.TRUE.equals(input.get("push_activo")),
            student
        );
        return estudianteDAO.perfil(student);
    }

    /** Comprueba la clave actual antes de guardar la nueva con BCrypt. */
    public void changePassword(long student, Map<String, String> input) {
        var stored = estudianteDAO.claveGuardada(student);
        if (
            !passwords.matches(
                Objects.toString(input.get("actual"), ""),
                stored.get("contrasena_hash").toString()
            )
        ) throw new ResponseStatusException(BAD_REQUEST, "La contraseña actual no coincide.");
        password(input.get("nueva"));
        estudianteDAO.cambiarClave((String) passwords.encode(input.get("nueva")), student);
    }

    @Transactional
    public Object delete(long student) {
        estudianteDAO.desactivar(student);

        return Map.of("mensaje", "Cuenta desactivada.");
    }

    public Object other(long id) {
        return Map.of(
            "perfil",
            estudianteDAO.perfilPublico(id),
            "materias",
            materiaDAO.listarPublicas(id),
            "horario",
            disponibilidadDAO.listarPublica(id)
        );
    }

    /** Obtiene los datos públicos de la cuenta, sin incluir su contraseña. */
    public Map<String, Object> perfil(long estudianteId) {
        return estudianteDAO.perfil(estudianteId);
    }
}
