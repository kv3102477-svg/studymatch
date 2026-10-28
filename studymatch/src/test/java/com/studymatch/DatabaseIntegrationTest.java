package com.studymatch;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymatch.dao.*;
import com.studymatch.dao.jdbc.SqlExecutor;
import com.studymatch.model.Registro;
import com.studymatch.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.*;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Consultas reales a PostgreSQL. Todas las escrituras se revierten al terminar cada prueba. */
@SpringBootTest(properties = "studymatch.jobs.enabled=false")
@AutoConfigureMockMvc
@Transactional
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
    named = "STUDYMATCH_DB_TESTS",
    matches = "true"
)
class DatabaseIntegrationTest {

    @Autowired
    BusquedaService busquedaService;

    @Autowired
    ConversacionService conversacionService;

    @Autowired
    DisponibilidadService disponibilidadService;

    @Autowired
    MateriaService materiaService;

    @Autowired
    ProgresoService progresoService;

    @Autowired
    RecordatorioService recordatorioService;

    @Autowired
    SesionService sesionService;

    @Autowired
    SuscripcionService suscripcionService;

    @Autowired
    EstudianteDAO estudianteDAO;

    @Autowired
    SesionDAO sesionDAO;

    @Autowired
    EstudianteService service;

    @org.springframework.boot.test.mock.mockito.SpyBean
    RecoveryService verificationMail;

    @Autowired
    DocumentService files;

    @Autowired
    SqlExecutor db;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private final List<String> emails = new ArrayList<>();
    private String testCareer, secondaryCareer;

    @BeforeEach
    void academicCatalogFixtures() {
        org.mockito.Mockito.doNothing()
            .when(verificationMail)
            .sendVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
            );
        // Solo existen dentro de la transacción de cada prueba; no se cargan en producción.
        testCareer = "Carrera de prueba " + UUID.randomUUID();
        secondaryCareer = "Sistemas " + UUID.randomUUID();
        db.update("INSERT INTO carrera(nombre) VALUES (?),(?)", testCareer, secondaryCareer);
        db.update(
            "INSERT INTO ciclo_estudio(nombre,orden) VALUES ('Ciclo 1',1),('Ciclo 2',2) ON CONFLICT(nombre) DO NOTHING"
        );
    }

    private long student() {
        String email = "integration-" + UUID.randomUUID() + "@utp.edu.pe";
        emails.add(email);
        return (
            (Number) service
                .register(
                    new Registro(
                        "Prueba transaccional",
                        email,
                        "Testing-Only-2026",
                        testCareer,
                        "Ciclo 1"
                    )
                )
                .get("id")
        ).longValue();
    }

    private MockHttpSession session(long student) {
        db.update("UPDATE estudiante SET correo_verificado=true WHERE id=?", student);
        var session = new MockHttpSession();
        session.setAttribute("studentId", student);
        session.setAttribute("csrf", "integration-csrf");
        session.setAttribute(
            "authVersion",
            db
                .one("SELECT contrasena_hash FROM estudiante WHERE id=?", student)
                .get("contrasena_hash")
        );
        return session;
    }

    private long subject() {
        return db.insert(
            "INSERT INTO materia(nombre) VALUES (?)",
            "integration-" + UUID.randomUUID()
        );
    }

    private LocalDateTime proposalDate() {
        return LocalDate.now(ZoneId.of("America/Bogota")).plusDays(2).atTime(10, 0);
    }

    private void match(long teacher, long learner, long subject) {
        db.update(
            "INSERT INTO estudiante_materia(estudiante_id,materia_id,tipo_rol,nivel) VALUES (?,?,'ENSENAR','INTERMEDIO')",
            teacher,
            subject
        );
        db.update(
            "INSERT INTO estudiante_materia(estudiante_id,materia_id,tipo_rol,nivel) VALUES (?,?,'APRENDER','BASICO') ON CONFLICT DO NOTHING",
            learner,
            subject
        );
    }

    private void available(long student, int day, String start, String end) {
        db.update(
            "INSERT INTO disponibilidad_horaria(estudiante_id,dia_semana,hora_inicio,hora_fin) VALUES (?,?,?,?)",
            student,
            day,
            java.sql.Time.valueOf(start),
            java.sql.Time.valueOf(end)
        );
    }

    private Map<String, Object> proposal(
        long other,
        long subject,
        long teacher,
        LocalDateTime date,
        int duration
    ) {
        return Map.of(
            "receptor_id",
            other,
            "materia_id",
            subject,
            "mentor_id",
            teacher,
            "duracion_programada_min",
            duration,
            "fecha_hora",
            date.toString()
        );
    }

    private long proposed(long first, long second) {
        long subject = subject();
        match(first, second, subject);
        for (long id : new long[] { first, second })
            available(id, proposalDate().getDayOfWeek().getValue(), "09:00:00", "14:00:00");
        return sesionService.propose(first, proposal(second, subject, first, proposalDate(), 60));
    }

    @AfterTransaction
    void nothingLeftBehind() {
        for (String email : emails)
            assertThat(db.list("SELECT id FROM estudiante WHERE correo=?", email)).isEmpty();
    }

    @Test
    void registrationPersistsHashAndReturnsNoPassword() {
        long id = student();
        var stored = db.one("SELECT * FROM estudiante WHERE id=?", id);
        assertThat(stored.get("contrasena_hash").toString())
            .startsWith("$2a$")
            .isNotEqualTo("Testing-Only-2026");
        assertThat(estudianteDAO.perfil(id)).doesNotContainKeys("contrasena", "contrasena_hash");
        assertThat(
            db.one("SELECT estado FROM suscripcion WHERE estudiante_id=?", id).get("estado")
        ).isEqualTo("GRATUITO");
        assertThat(progresoService.hours(id).get("sesiones_completadas")).isEqualTo(0L);
        assertThat(
            db.list("SELECT * FROM estudiante_insignia WHERE estudiante_id=?", id)
        ).isEmpty();
    }

    @Test
    void loginAcceptsOnlyActualPassword() {
        long id = student();
        String email = estudianteDAO.perfil(id).get("correo").toString();
        assertThat(
            service.login(Map.of("correo", email, "contrasena", "Testing-Only-2026")).get("id")
        ).isEqualTo(id);
        assertThatThrownBy(() ->
            service.login(Map.of("correo", email, "contrasena", "incorrecta"))
        ).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void apiRejectsAnonymousAccessAndMissingCsrf() throws Exception {
        mvc.perform(get("/api/sesiones")).andExpect(status().isUnauthorized());
        mvc.perform(
            post("/api/auth/login").contentType("application/json").content("{}")
        ).andExpect(status().isForbidden());
        var session = session(student());
        mvc.perform(
            put("/api/perfil").session(session).contentType("application/json").content("{}")
        ).andExpect(status().isForbidden());
    }

    @Test
    void registrationAndLoginRotateSessionAndProtectIdentity() throws Exception {
        String email = "integration-" + UUID.randomUUID() + "@utp.edu.pe";
        emails.add(email);
        var anonymous = new MockHttpSession();
        anonymous.setAttribute("csrf", "start-token");
        var response = mvc
            .perform(
                post("/api/auth/registro")
                    .session(anonymous)
                    .header("X-CSRF-Token", "start-token")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            new Registro(
                                "Cuenta transaccional",
                                email,
                                "Testing-Only-2026",
                                secondaryCareer,
                                "Ciclo 1"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiere_verificacion").value(true))
            .andExpect(jsonPath("$.codigo_enviado").value(false))
            .andExpect(jsonPath("$.estudiante").doesNotExist())
            .andReturn();
        var pending = (MockHttpSession) response.getRequest().getSession(false);
        assertThat(pending.getId()).isNotEqualTo(anonymous.getId());
        mvc.perform(get("/api/auth/yo").session(pending)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/verificacion").session(pending))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correo").value(email));
        org.mockito.Mockito.verify(
            verificationMail,
            org.mockito.Mockito.never()
        ).sendVerificationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        );
        mvc.perform(
            post("/api/auth/reenviar-codigo")
                .session(pending)
                .header("X-CSRF-Token", pending.getAttribute("csrf").toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo_enviado").value(true));
        var code = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(verificationMail).sendVerificationCode(
            org.mockito.ArgumentMatchers.eq(email),
            code.capture()
        );
        assertThat(code.getValue()).matches("[0-9]{6}");
        var verified = mvc
            .perform(
                post("/api/auth/verificar")
                    .session(pending)
                    .header("X-CSRF-Token", pending.getAttribute("csrf").toString())
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("codigo", code.getValue())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estudiante.correo_verificado").value(true))
            .andReturn();
        var authenticated = (MockHttpSession) verified.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(pending.getId());
        mvc.perform(get("/api/auth/yo").session(authenticated))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correo").value(email));
        mvc.perform(
            post("/api/auth/verificar")
                .session(authenticated)
                .header("X-CSRF-Token", authenticated.getAttribute("csrf").toString())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("codigo", code.getValue())))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotReadAnotherStudentsConversationOrDocument() throws Exception {
        long a = student(),
            b = student(),
            outsider = student();
        long conversation = conversacionService.directConversation(a, b);
        var session = session(outsider);
        mvc.perform(
            get("/api/conversaciones/" + conversation + "/mensajes").session(session)
        ).andExpect(status().isNotFound());
        long document = files.upload(
            a,
            new MockMultipartFile(
                "archivo",
                "prueba.pdf",
                "application/pdf",
                "%PDF-1.4\n%%EOF".getBytes()
            )
        );
        mvc.perform(get("/api/documentos/" + document).session(session)).andExpect(
            status().isNotFound()
        );
        assertThat(sesionDAO.listarDelEstudiante(outsider)).isEmpty();
    }

    @Test
    void messagesAndReadReceiptsAreStoredInDatabase() {
        long a = student(),
            b = student(),
            conversation = conversacionService.directConversation(a, b);
        assertThat(conversacionService.directConversation(a, b)).isEqualTo(conversation);
        long message = conversacionService.message(
            a,
            conversation,
            Map.of("texto", "Mensaje transaccional <script>")
        );
        assertThat(db.one("SELECT leido FROM mensaje WHERE id=?", message).get("leido")).isEqualTo(
            false
        );
        assertThat(conversacionService.messages(b, conversation)).hasSize(1);
        assertThat(db.one("SELECT leido FROM mensaje WHERE id=?", message).get("leido")).isEqualTo(
            true
        );
    }

    @Test
    void onlyParticipantsCanAdvanceSessionAndBothMustComplete() {
        long cycle = db.insert(
            "INSERT INTO ciclo_academico(nombre,activo) VALUES (?,true)",
            "it-" + UUID.randomUUID().toString().substring(0, 8)
        );
        long badge = db.insert(
            "INSERT INTO insignia(nombre,meta_horas) VALUES (?,0.1)",
            "it-" + UUID.randomUUID()
        );
        long a = student(),
            b = student(),
            outsider = student(),
            s = proposed(a, b);
        assertThatThrownBy(() ->
            sesionService.sessionAction(outsider, s, "aceptar", Map.of())
        ).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() ->
            sesionService.sessionAction(a, s, "aceptar", Map.of())
        ).isInstanceOf(ResponseStatusException.class);
        sesionService.sessionAction(b, s, "aceptar", Map.of());
        sesionService.sessionAction(
            a,
            s,
            "enlace",
            Map.of("enlace", "https://zoom.us/j/123456789")
        );
        assertThatThrownBy(() ->
            sesionService.sessionAction(a, s, "iniciar", Map.of())
        ).isInstanceOf(ResponseStatusException.class);
        db.update(
            "UPDATE sesion SET fecha_hora=? WHERE id=?",
            LocalDateTime.now(ZoneId.of("America/Bogota")).minusHours(1),
            s
        );
        sesionService.sessionAction(a, s, "iniciar", Map.of());
        db.update(
            "UPDATE sesion SET inicio_real=? WHERE id=?",
            LocalDateTime.now(ZoneId.of("America/Bogota")).minusMinutes(30),
            s
        );
        sesionService.sessionAction(a, s, "finalizar", Map.of());
        assertThat(sesionDAO.buscarParticipante(s, a).get("estado")).isEqualTo("EN_CURSO");
        sesionService.sessionAction(b, s, "finalizar", Map.of());
        assertThat(sesionDAO.buscarParticipante(s, a).get("estado")).isEqualTo("COMPLETADA");
        assertThat(
            ((Number) progresoService.hours(a).get("horas_ensenadas")).doubleValue()
        ).isBetween(0.49, 0.52);
        assertThat(
            ((Number) progresoService.hours(b).get("horas_aprendidas")).doubleValue()
        ).isBetween(0.49, 0.52);
        assertThat(
            db.list(
                "SELECT * FROM estudiante_insignia WHERE estudiante_id=? AND insignia_id=?",
                a,
                badge
            )
        ).hasSize(1);
        assertThat(
            db.list(
                "SELECT * FROM estadistica_horas WHERE estudiante_id=? AND ciclo_id=?",
                a,
                cycle
            )
        ).hasSize(1);
        assertThat(progresoService.ranking()).anyMatch(row -> row.get("id").equals(a));
        sesionService.rate(b, s, Map.of("estrellas", 5, "texto", "Opinión transaccional"));
        sesionService.rate(b, s, Map.of("estrellas", 4, "texto", "Actualización transaccional"));
        assertThat(db.list("SELECT id FROM calificacion WHERE sesion_id=?", s)).hasSize(1);
        assertThat(
            db
                .one(
                    "SELECT calificacion_promedio FROM estadistica_horas WHERE estudiante_id=? AND ciclo_id=?",
                    a,
                    cycle
                )
                .get("calificacion_promedio")
        ).isEqualTo(new java.math.BigDecimal("4.00"));
        assertThatThrownBy(() ->
            sesionService.sessionAction(a, s, "finalizar", Map.of())
        ).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void availabilityReplacementIsAtomic() {
        long a = student();
        disponibilidadService.replaceAvailability(
            a,
            List.of(Map.of("dia_semana", 1, "hora_inicio", "08:00", "hora_fin", "09:00"))
        );
        assertThatThrownBy(() ->
            disponibilidadService.availability(
                a,
                Map.of("dia_semana", 1, "hora_inicio", "08:30", "hora_fin", "09:30")
            )
        ).isInstanceOf(ResponseStatusException.class);
        assertThat(
            db.list("SELECT * FROM disponibilidad_horaria WHERE estudiante_id=?", a)
        ).hasSize(1);
    }

    @Test
    void documentsStoreActualBytesAndDeletionUnlinksLargeObject() throws Exception {
        long a = student();
        byte[] content = "%PDF-1.4\n%%EOF".getBytes();
        long file = files.upload(
            a,
            new MockMultipartFile("archivo", "transaccion.pdf", "application/pdf", content)
        );
        assertThat((byte[]) files.download(a, file).get("bytes")).isEqualTo(content);
        long oid = Long.parseLong(
            db
                .one("SELECT url_archivo FROM documento_apoyo WHERE id=?", file)
                .get("url_archivo")
                .toString()
                .substring(5)
        );
        files.delete(a, file);
        assertThat(
            db.list("SELECT oid FROM pg_largeobject_metadata WHERE oid=CAST(? AS oid)", oid)
        ).isEmpty();
    }

    @Test
    void paymentsNeverPretendToCharge() throws Exception {
        var session = session(student());
        mvc.perform(
            post("/api/pagos")
                .session(session)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content("{}")
        ).andExpect(status().isServiceUnavailable());
        assertThat(
            db.list("SELECT id FROM pago WHERE estudiante_id=?", session.getAttribute("studentId"))
        ).isEmpty();
    }

    @Test
    void profileUpdateAndSubjectSelectionComeBackFromPostgres() throws Exception {
        long a = student(),
            subject = subject();
        var session = session(a);
        mvc.perform(
            put("/api/perfil")
                .session(session)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "nombre",
                            "Nombre actualizado",
                            "carrera",
                            secondaryCareer,
                            "ciclo",
                            "Ciclo 2",
                            "bio",
                            "Presentación guardada",
                            "push_activo",
                            false
                        )
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Nombre actualizado"));
        assertThat(estudianteDAO.perfil(a).get("bio")).isEqualTo("Presentación guardada");
        materiaService.subject(
            a,
            Map.of("materia_id", subject, "tipo_rol", "ENSENAR", "nivel", "BASICO")
        );
        materiaService.subject(
            a,
            Map.of("materia_id", subject, "tipo_rol", "ENSENAR", "nivel", "AVANZADO")
        );
        assertThat(
            db.list("SELECT nivel FROM estudiante_materia WHERE estudiante_id=?", a)
        ).hasSize(1);
        mvc.perform(get("/api/perfil/resumen").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/perfil/materias").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nivel").value("AVANZADO"));
    }

    @Test
    void trialIsStoredOnceAndCannotBeRepeated() {
        long a = student(),
            plan = db.insert(
                "INSERT INTO plan(nombre,precio) VALUES (?,10)",
                "it-" + UUID.randomUUID()
            );
        suscripcionService.trial(a, Map.of("plan_id", plan));
        assertThatThrownBy(() -> suscripcionService.trial(a, Map.of("plan_id", plan))).isInstanceOf(
            ResponseStatusException.class
        );
        assertThat(
            db.list("SELECT id FROM suscripcion WHERE estudiante_id=? AND es_trial=true", a)
        ).hasSize(1);
    }

    @Test
    void searchReturnsDatabaseProfilesAndRealAvailableDays() {
        long a = student(),
            b = student();
        long subject = subject();
        materiaService.subject(
            a,
            Map.of("materia_id", subject, "tipo_rol", "APRENDER", "nivel", "BASICO")
        );
        materiaService.subject(
            b,
            Map.of("materia_id", subject, "tipo_rol", "ENSENAR", "nivel", "AVANZADO")
        );
        disponibilidadService.replaceAvailability(
            b,
            List.of(Map.of("dia_semana", 2, "hora_inicio", "10:00", "hora_fin", "11:00"))
        );
        var rows = busquedaService.search(a, "Prueba transaccional");
        assertThat(rows).noneMatch(row -> row.get("id").equals(a));
        assertThat(rows).anyMatch(
            row -> row.get("id").equals(b) && row.get("dias_disponibles").equals(List.of(2))
        );
        assertThat(rows).anyMatch(
            row -> row.get("id").equals(b) && row.get("coincidencia_materias").equals(100L)
        );
    }

    @Test
    void thymeleafServesOriginalSectionsWithoutDemoScripts() throws Exception {
        String page = mvc
            .perform(get("/"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(page).contains(
            "id=\"perfil-section\"",
            "id=\"insignias-section\"",
            "id=\"chat-section\"",
            "src=\"/app.js?v=reference-3\""
        );
        assertThat(page).doesNotContain(
            "th:src",
            "Tips de prueba",
            "Math.random",
            "Alejandro Ruiz"
        );
    }

    @Test
    void remindersUseActualDatesAndDoNotDuplicate() {
        long a = student(),
            b = student(),
            session = proposed(a, b);
        sesionService.sessionAction(b, session, "aceptar", Map.of());
        db.update(
            "UPDATE sesion SET fecha_hora=CURRENT_TIMESTAMP+INTERVAL '1 hour' WHERE id=?",
            session
        );
        recordatorioService.processReminders();
        recordatorioService.processReminders();
        assertThat(
            db.list(
                "SELECT id FROM notificacion WHERE tipo_evento='RECORDATORIO' AND referencia_id=?",
                session
            )
        ).hasSize(2);
        assertThat(sesionDAO.buscarParticipante(session, a).get("estado")).isEqualTo("CONFIRMADA");
    }

    @Test
    void passwordChangeRevokesExistingSessions() throws Exception {
        long a = student();
        var session = session(a);
        service.changePassword(
            a,
            Map.of("actual", "Testing-Only-2026", "nueva", "Testing-Only-Changed-2026")
        );
        mvc.perform(get("/api/auth/yo").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void demoGatewayDoesNotCreatePaymentsOrPaidSubscriptions() throws Exception {
        long a = student(),
            plan = db.insert(
                "INSERT INTO plan(nombre,precio) VALUES (?,12.5)",
                "it-" + UUID.randomUUID()
            );
        var session = session(a);
        for (String scenario : List.of("APROBADO", "RECHAZADO", "TIMEOUT")) {
            mvc.perform(
                post("/api/pagos/prueba")
                    .session(session)
                    .header("X-CSRF-Token", "integration-csrf")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(Map.of("plan_id", plan, "escenario", scenario))
                    )
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modo").value("PRUEBA"))
                .andExpect(jsonPath("$.estado").value(scenario));
        }
        assertThat(db.list("SELECT id FROM pago WHERE estudiante_id=?", a)).isEmpty();
        assertThat(
            db.list(
                "SELECT id FROM suscripcion WHERE estudiante_id=? AND estado IN ('ACTIVO','TRIAL')",
                a
            )
        ).isEmpty();
    }

    @Test
    void chatAttachmentsPersistAndOnlyParticipantsCanDownload() throws Exception {
        long a = student(),
            b = student(),
            outsider = student(),
            conversation = conversacionService.directConversation(a, b);
        byte[] content = "%PDF-1.4\n%%EOF".getBytes();
        long message = files.uploadChat(
            a,
            conversation,
            new MockMultipartFile("archivo", "adjunto.pdf", "application/pdf", content)
        );
        assertThat((byte[]) files.chatDownload(b, conversation, message).get("bytes")).isEqualTo(
            content
        );
        var delivered = conversacionService.messages(b, conversation).get(0);
        assertThat(delivered.get("texto")).isEqualTo("");
        assertThat(delivered).containsKey("archivo");
        mvc.perform(
            get(
                "/api/conversaciones/" + conversation + "/mensajes/" + message + "/archivo"
            ).session(session(outsider))
        ).andExpect(status().isNotFound());
        assertThatThrownBy(() ->
            conversacionService.message(
                a,
                conversation,
                Map.of("texto", DocumentService.FILE_PREFIX + "{}")
            )
        ).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publicCatalogsReturnDatabaseOptionsWithoutLogin() throws Exception {
        String name = "Catálogo-" + UUID.randomUUID();
        db.insert("INSERT INTO carrera(nombre) VALUES (?)", name);
        var result = mvc.perform(get("/api/auth/catalogos")).andExpect(status().isOk()).andReturn();
        var catalogs = json.readTree(result.getResponse().getContentAsByteArray());
        assertThat(catalogs.get("carreras").findValuesAsText("nombre")).contains(name);
        assertThat(catalogs.get("ciclos").findValuesAsText("nombre")).contains(
            "Ciclo 1",
            "Ciclo 2"
        );
        db.update("UPDATE carrera SET activo=false WHERE nombre=?", name);
        catalogs = json.readTree(
            mvc
                .perform(get("/api/auth/catalogos"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray()
        );
        assertThat(catalogs.get("carreras").findValuesAsText("nombre")).doesNotContain(name);
    }

    @Test
    void registrationRejectsMissingOrInactiveCatalogChoices() {
        String email = "integration-" + UUID.randomUUID() + "@utp.edu.pe";
        emails.add(email);
        assertThatThrownBy(() ->
            service.register(
                new Registro(
                    "Prueba",
                    email,
                    "Testing-Only-2026",
                    "No existe " + UUID.randomUUID(),
                    "Ciclo 1"
                )
            )
        ).isInstanceOf(ResponseStatusException.class);
        String inactive = "Inactiva-" + UUID.randomUUID();
        db.insert("INSERT INTO carrera(nombre,activo) VALUES (?,false)", inactive);
        assertThatThrownBy(() ->
            service.register(
                new Registro("Prueba", email, "Testing-Only-2026", inactive, "Ciclo 1")
            )
        ).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() ->
            service.register(
                new Registro(
                    "Prueba",
                    email,
                    "Testing-Only-2026",
                    secondaryCareer,
                    "Ciclo inexistente"
                )
            )
        ).isInstanceOf(ResponseStatusException.class);
        assertThat(db.list("SELECT id FROM estudiante WHERE correo=?", email)).isEmpty();
    }

    @Test
    void renamingCatalogEntryUpdatesStudentReference() {
        long a = student();
        String renamed = "Renombrada-" + UUID.randomUUID();
        db.update("UPDATE carrera SET nombre=? WHERE nombre=?", renamed, testCareer);
        assertThat(estudianteDAO.perfil(a).get("carrera")).isEqualTo(renamed);
        service.updateProfile(
            a,
            Map.of(
                "nombre",
                "Prueba",
                "carrera",
                renamed,
                "ciclo",
                "Ciclo 2",
                "bio",
                "Guardado",
                "push_activo",
                true
            )
        );
        assertThat(estudianteDAO.perfil(a).get("ciclo")).isEqualTo("Ciclo 2");
    }

    @Test
    void emptyCatalogsReturnNoInventedChoices() throws Exception {
        db.update("UPDATE carrera SET activo=false");
        db.update("UPDATE ciclo_estudio SET activo=false");
        mvc.perform(get("/api/auth/catalogos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.carreras").isEmpty())
            .andExpect(jsonPath("$.ciclos").isEmpty());
    }

    @Test
    void courseSearchIsPaginatedAndReadsOnlyDatabaseRows() throws Exception {
        long student = student();
        var session = session(student);
        String prefix = "it-search-" + UUID.randomUUID() + "-";
        var values = new ArrayList<Object[]>();
        for (int i = 1; i <= 23; i++) values.add(new Object[] {
            prefix + String.format("%02d", i),
        });
        db.batch("INSERT INTO materia(nombre) VALUES (?)", values);
        mvc.perform(
            get("/api/materias")
                .session(session)
                .param("q", prefix.toUpperCase())
                .param("pagina", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(23))
            .andExpect(jsonPath("$.items.length()").value(20))
            .andExpect(jsonPath("$.items[0].nombre").value(prefix + "01"));
        mvc.perform(get("/api/materias").session(session).param("q", prefix).param("pagina", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].nombre").value(prefix + "21"));
        mvc.perform(
            get("/api/materias")
                .session(session)
                .param("q", "no-match-" + UUID.randomUUID())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/materias").session(session).param("pagina", "0")).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void proposalOnlyAllowsComplementarySubjectsAndCorrectTeacher() throws Exception {
        long a = student(),
            b = student(),
            course = subject(),
            unrelated = subject();
        match(a, b, course);
        var matches = materiaService.matchingSubjects(b, a);
        assertThat(matches).hasSize(1);
        assertThat(((Number) matches.get(0).get("mentor_id")).longValue()).isEqualTo(a);
        mvc.perform(get("/api/estudiantes/" + a + "/coincidencias").session(session(b)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(course))
            .andExpect(jsonPath("$[0].mentor_id").value(a));
        assertThatThrownBy(() ->
            sesionService.propose(a, proposal(b, unrelated, a, proposalDate(), 60))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("coincidir");
        assertThatThrownBy(() ->
            sesionService.propose(a, proposal(b, course, b, proposalDate(), 60))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("coincidir");
        for (long id : new long[] { a, b })
            available(id, proposalDate().getDayOfWeek().getValue(), "09:00:00", "12:00:00");
        long created = sesionService.propose(b, proposal(a, course, a, proposalDate(), 60));
        assertThat(
            ((Number) sesionDAO.buscarParticipante(created, b).get("mentor_id")).longValue()
        ).isEqualTo(a);
    }

    @Test
    void proposalRequiresFullAvailabilityOfBothStudents() {
        long a = student(),
            b = student(),
            course = subject();
        match(a, b, course);
        int day = proposalDate().getDayOfWeek().getValue();
        available(a, day, "09:00:00", "13:00:00");
        assertThatThrownBy(() ->
            sesionService.propose(a, proposal(b, course, a, proposalDate(), 60))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("disponibilidad");
        available(b, day, "10:00:00", "11:00:00");
        assertThatThrownBy(() ->
            sesionService.propose(a, proposal(b, course, a, proposalDate(), 120))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("disponibilidad");
        available(b, day, "11:00:00", "12:00:00");
        assertThat(
            sesionService.propose(a, proposal(b, course, a, proposalDate(), 120))
        ).isPositive();
    }

    @Test
    void proposalRejectsOverlapsButAllowsAdjacentSessions() {
        long a = student(),
            b = student(),
            c = student(),
            course = subject();
        match(a, b, course);
        match(c, b, course);
        int day = proposalDate().getDayOfWeek().getValue();
        for (long id : new long[] { a, b, c }) available(id, day, "09:00:00", "14:00:00");
        sesionService.propose(a, proposal(b, course, a, proposalDate(), 60));
        assertThatThrownBy(() ->
            sesionService.propose(c, proposal(b, course, c, proposalDate().plusMinutes(30), 60))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("cruza");
        assertThat(
            sesionService.propose(c, proposal(b, course, c, proposalDate().plusHours(1), 60))
        ).isPositive();
    }

    @Test
    void recommendationsRequireComplementarySubjectsAndAnHourInCommonAcrossCareers()
        throws Exception {
        long a = student(),
            b = student(),
            noTime = student(),
            noSubject = student(),
            course = subject();
        match(b, a, course);
        match(noTime, a, course);
        db.update("UPDATE estudiante SET carrera=? WHERE id=?", secondaryCareer, b);
        available(a, 2, "09:00:00", "12:00:00");
        available(b, 2, "10:00:00", "10:30:00");
        available(b, 2, "10:30:00", "11:00:00");
        available(noTime, 2, "12:00:00", "14:00:00");
        available(noSubject, 2, "10:00:00", "12:00:00");
        var rows = busquedaService.search(a, "", true);
        assertThat(rows)
            .extracting(row -> ((Number) row.get("id")).longValue())
            .contains(b)
            .doesNotContain(a, noTime, noSubject);
        assertThat(
            (
                (Number) rows
                    .stream()
                    .filter(row -> ((Number) row.get("id")).longValue() == b)
                    .findFirst()
                    .orElseThrow()
                    .get("minutos_en_comun")
            ).doubleValue()
        ).isEqualTo(60.0);
        mvc.perform(get("/api/estudiantes").param("recomendados", "true").session(session(a)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(b));
        assertThatThrownBy(() -> busquedaService.search(noSubject, "", true))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Completa tus materias y disponibilidad para ver sugerencias");
    }

    @Test
    void chatIsOnePerPersonAndContainsAllHistoricalSessionMessages() throws Exception {
        long a = student(),
            b = student(),
            outsider = student(),
            meeting = proposed(a, b);
        sesionService.sessionAction(b, meeting, "aceptar", Map.of());
        assertThat(
            db.list("SELECT id FROM conversacion WHERE estudiante_a=? OR estudiante_b=?", a, a)
        ).isEmpty();
        long chat = conversacionService.directConversation(a, b);
        assertThat(conversacionService.directConversation(b, a)).isEqualTo(chat);
        long historical = db.insert(
            "INSERT INTO conversacion(sesion_id,estudiante_a,estudiante_b,estado) VALUES (?,?,?,'FINALIZADO')",
            meeting,
            b,
            a
        );
        db.insert(
            "INSERT INTO mensaje(conversacion_id,emisor_id,texto) VALUES (?,?,?)",
            historical,
            b,
            "Mensaje del curso anterior"
        );
        conversacionService.message(a, chat, Map.of("texto", "Seguimos aquí para otro curso"));
        long attachment = files.uploadChat(
            b,
            historical,
            new MockMultipartFile(
                "archivo",
                "apunte.pdf",
                "application/pdf",
                "%PDF-1.4\n%%EOF".getBytes()
            )
        );
        mvc.perform(get("/api/conversaciones").session(session(a)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(chat));
        var history = conversacionService.messages(a, chat);
        assertThat(history).hasSize(3);
        assertThat(history)
            .extracting(row -> row.get("texto"))
            .contains("Mensaje del curso anterior", "Seguimos aquí para otro curso");
        assertThat(history.get(2).get("archivo").toString()).contains(
            "/conversaciones/" + historical + "/mensajes/" + attachment
        );
        assertThat(
            db.list("SELECT id FROM mensaje WHERE conversacion_id=? AND leido=false", historical)
        ).isEmpty();
        mvc.perform(get("/api/conversaciones").session(session(outsider)))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
        assertThatThrownBy(() -> conversacionService.messages(outsider, chat)).isInstanceOf(
            ResponseStatusException.class
        );
        sesionService.sessionAction(a, meeting, "cancelar", Map.of("motivo", "Cambio de fecha"));
        assertThat(
            conversacionService.message(a, chat, Map.of("texto", "Podemos seguir conversando"))
        ).isPositive();
    }

    @Test
    void registrationRequiresExactUtpDomainAndNormalizesUppercase() throws Exception {
        for (String email : List.of(
            "alumno@gmail.com",
            "alumno@prueba.test",
            "alumno@utp.edu.pe.otro.com",
            "alumno@otra.utp.edu.pe",
            "alumno@utp.edu.pe@otro.com"
        )) {
            assertThatThrownBy(() ->
                service.register(
                    new Registro("Prueba", email, "Testing-Only-2026", secondaryCareer, "Ciclo 1")
                )
            )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("@utp.edu.pe");
            assertThat(db.list("SELECT id FROM estudiante WHERE correo=?", email)).isEmpty();
        }
        var anonymous = new MockHttpSession();
        anonymous.setAttribute("csrf", "integration-csrf");
        mvc.perform(
            post("/api/auth/registro")
                .session(anonymous)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        new Registro(
                            "Prueba",
                            "alumno@prueba.test",
                            "Testing-Only-2026",
                            secondaryCareer,
                            "Ciclo 1"
                        )
                    )
                )
        ).andExpect(status().isBadRequest());
        String email = "integration-" + UUID.randomUUID() + "@utp.edu.pe";
        emails.add(email);
        var registered = service.register(
            new Registro(
                "Prueba",
                email.toUpperCase(Locale.ROOT),
                "Testing-Only-2026",
                secondaryCareer,
                "Ciclo 1"
            )
        );
        assertThat(registered.get("correo")).isEqualTo(email);
    }

    private MockHttpSession pendingVerification(long student) throws Exception {
        var anonymous = new MockHttpSession();
        anonymous.setAttribute("csrf", "start-token");
        var result = mvc
            .perform(
                post("/api/auth/login")
                    .session(anonymous)
                    .header("X-CSRF-Token", "start-token")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "correo",
                                estudianteDAO.perfil(student).get("correo"),
                                "contrasena",
                                "Testing-Only-2026"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiere_verificacion").value(true))
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void verificationLimitsAttemptsResendsAndExpiredCodes() throws Exception {
        long id = student();
        var pending = pendingVerification(id);
        String csrf = pending.getAttribute("csrf").toString();
        mvc.perform(
            post("/api/auth/reenviar-codigo").session(pending).header("X-CSRF-Token", csrf)
        ).andExpect(status().isOk());
        mvc.perform(
            post("/api/auth/reenviar-codigo").session(pending).header("X-CSRF-Token", csrf)
        ).andExpect(status().isTooManyRequests());
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(verificationMail).sendVerificationCode(
            org.mockito.ArgumentMatchers.anyString(),
            captured.capture()
        );
        String wrong = captured.getValue().equals("000000") ? "999999" : "000000";
        for (int i = 0; i < 5; i++) mvc.perform(
            post("/api/auth/verificar")
                .session(pending)
                .header("X-CSRF-Token", csrf)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("codigo", wrong)))
        ).andExpect(status().isBadRequest());
        mvc.perform(
            post("/api/auth/verificar")
                .session(pending)
                .header("X-CSRF-Token", csrf)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("codigo", captured.getValue())))
        ).andExpect(status().isTooManyRequests());
        assertThat(estudianteDAO.perfil(id).get("correo_verificado")).isEqualTo(false);
        pending.setAttribute("otpResendAt", 0L);
        mvc.perform(post("/api/auth/reenviar-codigo").session(pending).header("X-CSRF-Token", csrf))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo_enviado").value(true));
        pending.setAttribute("otpExpires", 0L);
        mvc.perform(
            post("/api/auth/verificar")
                .session(pending)
                .header("X-CSRF-Token", csrf)
                .contentType("application/json")
                .content("{\"codigo\":\"123456\"}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void mailFailureDoesNotAuthenticateOrClaimTheCodeWasSent() throws Exception {
        org.mockito.Mockito.doThrow(
            new org.springframework.mail.MailSendException("No SMTP in test")
        )
            .when(verificationMail)
            .sendVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
            );
        long id = student();
        var pending = pendingVerification(id);
        mvc.perform(
            post("/api/auth/reenviar-codigo")
                .session(pending)
                .header("X-CSRF-Token", pending.getAttribute("csrf").toString())
        ).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/auth/verificacion").session(pending))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo_enviado").value(false));
        mvc.perform(get("/api/sesiones").session(pending)).andExpect(status().isUnauthorized());
        assertThat(estudianteDAO.perfil(id).get("correo_verificado")).isEqualTo(false);
        assertThat(pending.getAttribute("otpHash")).isNull();
    }

    @Test
    void supportAndReportsPersistForAuthenticatedStudentAndGroupDuplicates() throws Exception {
        long a = student(),
            b = student();
        var session = session(a);
        mvc.perform(
            post("/api/soporte")
                .session(session)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "categoria",
                            "Cuenta y Perfil",
                            "asunto",
                            "Prueba transaccional",
                            "mensaje",
                            "Solicitud que se revierte al terminar la prueba"
                        )
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.codigo").isString());
        assertThat(
            db.list("SELECT id FROM solicitud_soporte WHERE estudiante_id=? AND tipo='SOPORTE'", a)
        ).hasSize(1);
        for (boolean duplicate : List.of(false, true))
            mvc.perform(
                post("/api/reportes-usuario")
                    .session(session)
                    .header("X-CSRF-Token", "integration-csrf")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "categoria",
                                "Otro motivo",
                                "mensaje",
                                "Descripción de prueba",
                                "reportadoId",
                                b
                            )
                        )
                    )
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existente").value(duplicate));
        assertThat(
            db.list(
                "SELECT id FROM solicitud_soporte WHERE estudiante_id=? AND reportado_id=?",
                a,
                b
            )
        ).hasSize(1);
        mvc.perform(
            post("/api/reportes-usuario")
                .session(session)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("categoria", "Otro motivo", "mensaje", "Prueba", "reportadoId", a)
                    )
                )
        ).andExpect(status().isBadRequest());
        mvc.perform(
            post("/api/soporte")
                .session(session)
                .header("X-CSRF-Token", "integration-csrf")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("categoria", "No existe", "asunto", "Prueba", "mensaje", "Prueba")
                    )
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void internalCertificateUsesOnlyTheSignedInStudentsActualActivity() throws Exception {
        long a = student();
        mvc.perform(get("/api/perfil/constancia").session(session(a)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil.id").value(a))
            .andExpect(jsonPath("$.horas.sesiones_completadas").value(0))
            .andExpect(jsonPath("$.perfil.contrasena_hash").doesNotExist());
        mvc.perform(get("/api/perfil/constancia")).andExpect(status().isUnauthorized());
    }

    @Test
    void separatedRegistrationVerificationAndLoginKeepTheSameSessionContract() throws Exception {
        String email = "integration-" + UUID.randomUUID() + "@utp.edu.pe";
        emails.add(email);
        var anonymous = new MockHttpSession();
        anonymous.setAttribute("csrf", "register-token");
        var registration = mvc
            .perform(
                post("/api/auth/registro")
                    .session(anonymous)
                    .header("X-CSRF-Token", "register-token")
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            new Registro(
                                "Registro separado",
                                email,
                                "Testing-Only-2026",
                                testCareer,
                                "Ciclo 1"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiere_verificacion").value(true))
            .andReturn();
        var pending = (MockHttpSession) registration.getRequest().getSession(false);
        assertThat(pending.getAttribute("studentId")).isNull();
        mvc.perform(
            post("/api/auth/reenviar-codigo")
                .session(pending)
                .header("X-CSRF-Token", pending.getAttribute("csrf").toString())
        ).andExpect(status().isOk());
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(verificationMail).sendVerificationCode(
            org.mockito.ArgumentMatchers.eq(email),
            captured.capture()
        );
        var verified = mvc
            .perform(
                post("/api/auth/verificar")
                    .session(pending)
                    .header("X-CSRF-Token", pending.getAttribute("csrf").toString())
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("codigo", captured.getValue())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estudiante.correo").value(email))
            .andReturn();
        var signed = (MockHttpSession) verified.getRequest().getSession(false);
        assertThat(pending.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/yo").session(signed))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correo_verificado").value(true));
        mvc.perform(
            post("/api/auth/logout")
                .session(signed)
                .header("X-CSRF-Token", signed.getAttribute("csrf").toString())
        ).andExpect(status().isOk());
        var loginSession = new MockHttpSession();
        loginSession.setAttribute("csrf", "login-token");
        mvc.perform(
            post("/api/auth/login")
                .session(loginSession)
                .header("X-CSRF-Token", "login-token")
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("correo", email, "contrasena", "Testing-Only-2026")
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estudiante.correo").value(email));
    }

    @Test
    void combinedSearchFiltersRespectCourseRoleAndCommonDay() throws Exception {
        long me = student(),
            teacher = student(),
            otherDay = student(),
            learner = student(),
            course = subject();
        match(teacher, me, course);
        match(otherDay, me, course);
        db.update(
            "INSERT INTO estudiante_materia(estudiante_id,materia_id,tipo_rol,nivel) VALUES (?,?,'APRENDER','BASICO')",
            learner,
            course
        );
        available(me, 1, "09:00:00", "12:00:00");
        available(me, 2, "09:00:00", "12:00:00");
        available(teacher, 1, "10:00:00", "11:00:00");
        available(otherDay, 2, "10:00:00", "11:00:00");
        available(learner, 1, "10:00:00", "11:00:00");
        String name = db
            .one("SELECT nombre FROM materia WHERE id=?", course)
            .get("nombre")
            .toString();
        for (boolean recommended : List.of(false, true)) {
            var rows = busquedaService.search(me, "", recommended, name, 1, testCareer, "APRENDER");
            assertThat(rows)
                .extracting(row -> ((Number) row.get("id")).longValue())
                .containsExactly(teacher);
            assertThat(
                busquedaService.search(
                    me,
                    "nombre-que-no-existe",
                    recommended,
                    name,
                    1,
                    testCareer,
                    "APRENDER"
                )
            ).isEmpty();
            assertThat(
                busquedaService.search(
                    me,
                    "",
                    recommended,
                    "curso-que-no-existe",
                    1,
                    testCareer,
                    "APRENDER"
                )
            ).isEmpty();
        }
        mvc.perform(
            get("/api/estudiantes")
                .session(session(me))
                .param("curso", name)
                .param("dia", "1")
                .param("rol", "APRENDER")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(teacher));
        assertThatThrownBy(() ->
            busquedaService.search(me, "", false, "", 8, "", "APRENDER")
        ).isInstanceOf(ResponseStatusException.class);
    }
}
