package com.studymatch.tdd;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.studymatch.dao.*;
import com.studymatch.service.*;
import java.sql.Time;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos y DAO falsos exclusivos de los tests. No abre conexiones a Supabase. */
public abstract class BaseTdd {

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path directorioTemporal;

    protected final EstudianteDAO estudiantes = mock(
        EstudianteDAO.class
    );
    protected final CatalogoDAO catalogos = mock(CatalogoDAO.class);
    protected final MateriaDAO materias = mock(MateriaDAO.class);
    protected final DisponibilidadDAO horarios = mock(
        DisponibilidadDAO.class
    );
    protected final SesionDAO sesiones = mock(SesionDAO.class);
    protected final NotificacionDAO avisos = mock(
        NotificacionDAO.class
    );
    protected final InsigniaDAO insignias = mock(InsigniaDAO.class);
    protected final CalificacionDAO calificaciones = mock(
        CalificacionDAO.class
    );
    protected final ConversacionDAO conversaciones = mock(
        ConversacionDAO.class
    );
    protected final SuscripcionDAO suscripciones = mock(
        SuscripcionDAO.class
    );
    protected final EmparejamientoDAO matches = mock(
        EmparejamientoDAO.class
    );
    protected final ReportesDAO reportes = mock(ReportesDAO.class);
    protected final CertificadoDAO certificados = mock(
        CertificadoDAO.class
    );
    protected final Map<
        String,
        Map<String, Object>
    > certificadosGuardados = new LinkedHashMap<>();
    protected ReportesService reportesService;
    protected CertificadoService certificadoService;
    protected PdfService pdfService;
    protected final TokenDAO tokens = mock(TokenDAO.class);
    protected final DocumentoDAO documentos = mock(DocumentoDAO.class);
    protected final SoporteDAO soporte = mock(SoporteDAO.class);
    protected final Clock reloj = Clock.fixed(
        Instant.parse("2026-10-27T15:00:00Z"),
        ZoneId.of("America/Bogota")
    );
    protected final List<Map<String, Object>> sesionesGuardadas =
        new ArrayList<>();
    protected final List<Map<String, Object>> avisosGuardados =
        new ArrayList<>();
    protected final Map<Long, Map<String, Object>> cuentas =
        new LinkedHashMap<>();
    protected final List<Map<String, Object>> suscripcionesGuardadas =
        new ArrayList<>();
    protected RecoveryService correoService;
    protected NotificacionService notificacionService;
    protected EmailVerificationService verificacionService;
    protected AuthSessionService autenticacionService;
    protected Map<String, Object> sesionActual;
    protected EstudianteService estudianteService;
    protected MateriaService materiaService;
    protected DisponibilidadService disponibilidadService;
    protected BusquedaService busquedaService;
    protected ProgresoService progresoService;
    protected SesionService sesionService;
    protected ConversacionService conversacionService;
    protected SuscripcionService suscripcionService;
    protected SupportService supportService;
    protected RecordatorioService recordatorioService;
    protected DocumentService documentService;

    @BeforeEach
    void prepararDAOFalsos() {
        when(estudiantes.perfil(anyLong())).thenAnswer(call ->
            perfil(call.getArgument(0))
        );
        when(estudiantes.bloquear(anyLong())).thenAnswer(call ->
            fila("id", call.getArgument(0))
        );
        when(
            catalogos.buscarOpcion(anyString(), anyString())
        ).thenReturn(List.of(fila("activo", true)));
        when(sesiones.tieneRoles()).thenReturn(true);
        when(materias.validarRoles(101L, 202L, 10L)).thenReturn(
            List.of(fila("id", 1L))
        );
        when(horarios.bloquesDelDia(anyLong(), anyInt())).thenReturn(
            List.of(
                fila(
                    "hora_inicio",
                    Time.valueOf("09:00:00"),
                    "hora_fin",
                    Time.valueOf("22:00:00")
                )
            )
        );
        when(sesiones.buscarCruces(anyLong(), any(), any())).thenReturn(
            List.of()
        );
        when(
            sesiones.crearPropuesta(
                anyLong(),
                anyLong(),
                anyLong(),
                any(),
                anyInt(),
                anyLong(),
                anyLong()
            )
        ).thenAnswer(call -> {
            long id = sesionesGuardadas.size() + 1L;
            sesionesGuardadas.add(
                fila(
                    "id",
                    id,
                    "proponente_id",
                    call.getArgument(0),
                    "receptor_id",
                    call.getArgument(1),
                    "materia_id",
                    call.getArgument(2),
                    "fecha_hora",
                    call.getArgument(3),
                    "estado",
                    "PROPUESTA",
                    "mentor_id",
                    call.getArgument(5),
                    "aprendiz_id",
                    call.getArgument(6)
                )
            );
            return id;
        });
        doAnswer(call -> {
            avisosGuardados.add(
                fila(
                    "receptor_id",
                    call.getArgument(0),
                    "titulo",
                    call.getArgument(1),
                    "mensaje",
                    call.getArgument(2),
                    "evento",
                    call.getArgument(3),
                    "referencia_id",
                    call.getArgument(4)
                )
            );
            return null;
        })
            .when(avisos)
            .crear(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
            );
        pdfService = spy(new PdfService());
        progresoService = new ProgresoService(
            estudiantes,
            insignias,
            sesiones,
            pdfService
        );
        estudianteService = new EstudianteService(
            catalogos,
            horarios,
            estudiantes,
            materias,
            suscripciones,
            sesiones
        );
        materiaService = new MateriaService(estudiantes, materias);
        disponibilidadService = new DisponibilidadService(
            horarios,
            estudiantes
        );
        busquedaService = new BusquedaService(
            estudiantes,
            materias,
            horarios
        );
        sesionService = new SesionService(
            calificaciones,
            horarios,
            estudiantes,
            materias,
            avisos,
            sesiones,
            progresoService,
            new ModeracionService(),
            matches
        );
        ReflectionTestUtils.setField(sesionService, "clock", reloj);
        conversacionService = new ConversacionService(
            conversaciones,
            estudiantes,
            avisos,
            new ModeracionService()
        );
        suscripcionService = new SuscripcionService(
            estudiantes,
            suscripciones,
            pdfService
        );
        supportService = new SupportService(estudiantes, soporte);
        recordatorioService = new RecordatorioService(
            calificaciones,
            avisos,
            suscripciones
        );
        documentService = new DocumentService(
            conversaciones,
            documentos,
            estudiantes,
            avisos
        );
        reportesService = new ReportesService(reportes);
        certificadoService = new CertificadoService(
            certificados,
            pdfService,
            avisos,
            reloj,
            new FirmaService(directorioTemporal.resolve("firma.pk8"))
        );
        prepararDatosEnMemoria();
    }

    protected void prepararDatosEnMemoria() {
        when(
            matches.crear(anyLong(), anyLong(), anyLong(), anyInt())
        ).thenReturn(1L);
        when(materias.coincidencias(101L, 202L)).thenReturn(
            List.of(
                fila(
                    "id",
                    10L,
                    "nombre",
                    "Programación Web",
                    "mentor_id",
                    101L
                )
            )
        );
        when(suscripciones.comprobante(1L, 101L)).thenReturn(
            fila(
                "id",
                1L,
                "estado",
                "COMPLETADO",
                "numero_comprobante",
                "SM-0001",
                "monto",
                20.0,
                "moneda",
                "PEN"
            )
        );
        when(certificados.ciclo(anyLong())).thenReturn(
            fila(
                "id",
                1L,
                "nombre",
                "2026-II",
                "fecha_fin",
                java.sql.Date.valueOf("2026-10-26")
            )
        );
        when(certificados.candidatos(1L)).thenReturn(
            List.of(
                fila(
                    "estudiante_id",
                    101L,
                    "nombre",
                    "Ana Torres",
                    "carrera",
                    "Ingeniería de Sistemas",
                    "horas",
                    10.0
                )
            )
        );
        doAnswer(call -> {
            String codigo = call.getArgument(2);
            certificadosGuardados.put(
                codigo,
                fila(
                    "id",
                    1L,
                    "estudiante_id",
                    call.getArgument(0),
                    "ciclo_id",
                    call.getArgument(1),
                    "codigo",
                    codigo,
                    "horas",
                    call.getArgument(3),
                    "contenido",
                    call.getArgument(4),
                    "huella",
                    call.getArgument(5)
                )
            );
            return 1L;
        })
            .when(certificados)
            .guardar(
                anyLong(),
                anyLong(),
                anyString(),
                anyDouble(),
                any(),
                anyString()
            );

        doAnswer(call -> {
            for (var documento : certificadosGuardados.values()) {
                if (documento.get("id").equals(call.getArgument(0))) {
                    documento.put("firma", call.getArgument(1));
                    documento.put("clave_publica", call.getArgument(2));
                }
            }
            return null;
        })
            .when(certificados)
            .registrarFirma(anyLong(), any(), any());
        when(certificados.buscarCodigo(anyString())).thenAnswer(
            call -> {
                var documento = certificadosGuardados.get(
                    call.getArgument(0)
                );
                if (
                    documento == null
                ) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Aún no se ha generado tu certificado."
                );
                return documento;
            }
        );
        when(materias.listarPropias(anyLong())).thenReturn(
            List.of(
                fila(
                    "materia_id",
                    20L,
                    "tipo_rol",
                    "ENSENAR",
                    "nivel",
                    "BASICO"
                )
            )
        );
        when(horarios.listar(anyLong())).thenReturn(
            List.of(
                fila(
                    "dia_semana",
                    2,
                    "hora_inicio",
                    Time.valueOf("09:00:00"),
                    "hora_fin",
                    Time.valueOf("22:00:00")
                )
            )
        );
        String claveHash =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(
                4
            ).encode("Clave-Prueba2026!");
        when(estudiantes.claveGuardada(anyLong())).thenReturn(
            fila("contrasena_hash", claveHash)
        );
        when(estudiantes.credenciales(anyString())).thenReturn(
            List.of(fila("id", 101L, "contrasena_hash", claveHash))
        );
        when(
            estudiantes.validarIdentidad(anyLong(), anyString())
        ).thenReturn(List.of(fila("id", 101L)));
        when(
            estudiantes.verificarCorreo(anyLong(), anyString())
        ).thenAnswer(call -> {
            long id = call.getArgument(0);
            cuentas
                .computeIfAbsent(id, this::perfil)
                .put("correo_verificado", true);
            return 1;
        });
        doAnswer(call -> {
            long id = 303L;
            cuentas.put(
                id,
                fila(
                    "id",
                    id,
                    "correo",
                    call.getArgument(0),
                    "nombre",
                    call.getArgument(2),
                    "carrera",
                    call.getArgument(3),
                    "ciclo",
                    call.getArgument(4),
                    "correo_verificado",
                    false
                )
            );
            return id;
        })
            .when(estudiantes)
            .crear(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            );
        doAnswer(call ->
            cuentas.getOrDefault(
                call.getArgument(0),
                perfil(call.getArgument(0))
            )
        )
            .when(estudiantes)
            .perfil(anyLong());
        doAnswer(call -> {
            suscripcionesGuardadas.add(
                fila(
                    "estudiante_id",
                    call.getArgument(0),
                    "estado",
                    "GRATUITO"
                )
            );
            return 1;
        })
            .when(suscripciones)
            .crearGratuita(anyLong());
        when(suscripciones.buscarPlan(anyLong())).thenReturn(
            fila(
                "id",
                1L,
                "nombre",
                "Pro Ciclo",
                "precio",
                20,
                "moneda",
                "PEN"
            )
        );
        when(suscripciones.validarPlan(anyLong())).thenReturn(
            fila("id", 1L)
        );
        when(sesiones.horasAcumuladas(anyLong())).thenReturn(
            fila(
                "horas_ensenadas",
                10.0,
                "horas_aprendidas",
                5.0,
                "sesiones_completadas",
                10L
            )
        );
        when(estudiantes.resumen(anyLong())).thenReturn(
            fila("calificacion_promedio", 4.5, "companeros", 2L)
        );
        when(documentos.guardarContenido(any())).thenReturn(
            fila("oid", 1L)
        );
        when(
            documentos.crear(
                anyLong(),
                anyString(),
                anyString(),
                any(),
                anyString()
            )
        ).thenReturn(1L);
        when(avisos.bloquearRecordatorios()).thenReturn(
            fila("acquired", true)
        );
        correoService = mock(RecoveryService.class);
        notificacionService = new NotificacionService(
            avisos,
            estudiantes,
            correoService
        );
        verificacionService = new EmailVerificationService(
            estudiantes,
            correoService
        );
        autenticacionService = new AuthSessionService(
            estudiantes,
            verificacionService
        );
        sesionActual = fila(
            "id",
            501L,
            "proponente_id",
            101L,
            "receptor_id",
            202L,
            "mentor_id",
            101L,
            "aprendiz_id",
            202L,
            "materia_id",
            10L,
            "estado",
            "PROPUESTA",
            "enlace_videollamada",
            "https://meet.google.com/abc-defg-hij",
            "fecha_hora",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 27, 9, 0)
            ),
            "creada_en",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 26, 9, 0)
            ),
            "inicio_real",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 27, 9, 0)
            ),
            "fin_real",
            java.sql.Timestamp.valueOf(
                LocalDateTime.of(2026, 10, 27, 10, 0)
            ),
            "marca_proponente",
            false,
            "marca_receptor",
            false
        );
        when(
            sesiones.buscarParticipante(anyLong(), anyLong())
        ).thenAnswer(call -> sesionActual);
        doAnswer(call -> {
            sesionActual.put("estado", "CONFIRMADA");
            return 1;
        })
            .when(sesiones)
            .confirmar(anyLong());
        doAnswer(call -> {
            sesionActual.put("estado", "CANCELADA");
            return 1;
        })
            .when(sesiones)
            .cancelar(anyLong());
        doAnswer(call -> {
            sesionActual.put("estado", "EN_CURSO");
            return 1;
        })
            .when(sesiones)
            .iniciar(any(), anyLong());
        doAnswer(call -> {
            sesionActual.put(call.getArgument(0), true);
            return 1;
        })
            .when(sesiones)
            .marcarFinalizacion(anyString(), any(), anyLong());
        doAnswer(call -> {
            sesionActual.put("estado", "COMPLETADA");
            sesionActual.put("duracion_real_min", call.getArgument(0));
            return 1;
        })
            .when(sesiones)
            .completar(anyLong(), anyLong());
        doAnswer(call -> {
            sesionActual.put(
                "enlace_videollamada",
                call.getArgument(0)
            );
            return 1;
        })
            .when(sesiones)
            .guardarEnlace(nullable(String.class), anyLong());

        doAnswer(call -> {
            sesionActual.put("estado", "COMPLETADA");
            sesionActual.put(
                "duracion_real_min",
                (
                    (Number) sesionActual.get("duracion_programada_min")
                ).longValue()
            );
            sesionActual.put("origen_duracion", "ESTIMADO");
            return 1;
        })
            .when(sesiones)
            .completarEstimado(anyLong());
        doAnswer(call -> {
            sesionActual.put("estado", "COMPLETADA");
            sesionActual.put("duracion_real_min", null);
            return 1;
        })
            .when(sesiones)
            .cerrarSinDuracion(anyLong());
        when(
            conversaciones.buscarParticipante(anyLong(), anyLong())
        ).thenReturn(
            fila(
                "id",
                1L,
                "estudiante_a",
                101L,
                "estudiante_b",
                202L,
                "estado",
                "ACTIVA"
            )
        );
        when(
            conversaciones.crearMensaje(
                anyLong(),
                anyLong(),
                anyString()
            )
        ).thenReturn(1L);
        when(
            calificaciones.guardar(anyLong(), anyLong(), anyInt())
        ).thenReturn(1L);
        when(
            soporte.crear(
                anyLong(),
                any(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(1L);
    }

    protected com.studymatch.model.Registro registro(
        String nombre,
        String correo
    ) {
        return new com.studymatch.model.Registro(
            nombre,
            correo,
            "Clave-Prueba2026!",
            "Ingeniería de Sistemas",
            "Ciclo 3"
        );
    }

    protected org.springframework.mock.web.MockHttpSession pendienteVerificacion() {
        var session =
            new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("csrf", "csrf-prueba");
        verificacionService.begin(session, perfil(101L));
        return session;
    }

    protected RecoveryService recuperacion() {
        var plantilla = mock(org.thymeleaf.TemplateEngine.class);
        when(
            plantilla.process(
                anyString(),
                any(org.thymeleaf.context.IContext.class)
            )
        ).thenReturn("<p>Correo de prueba</p>");
        var service = new RecoveryService(
            estudiantes,
            tokens,
            plantilla
        );
        ReflectionTestUtils.setField(
            service,
            "host",
            "smtp.prueba.invalid"
        );
        ReflectionTestUtils.setField(service, "port", 587);
        ReflectionTestUtils.setField(service, "user", "");
        ReflectionTestUtils.setField(service, "password", "");
        ReflectionTestUtils.setField(
            service,
            "from",
            "remitente@prueba.invalid"
        );
        ReflectionTestUtils.setField(
            service,
            "appUrl",
            "http://prueba.invalid"
        );
        return service;
    }

    protected Map<String, Object> bloque(
        int dia,
        String inicio,
        String fin
    ) {
        return fila(
            "dia_semana",
            dia,
            "hora_inicio",
            inicio,
            "hora_fin",
            fin
        );
    }

    protected Map<String, Object> perfil(long id) {
        return fila(
            "id",
            id,
            "nombre",
            id == 101L ? "Ana Torres" : "Luis Rojas",
            "correo",
            "u" + String.format("%08d", id) + "@utp.edu.pe",
            "carrera",
            "Ingeniería de Sistemas",
            "ciclo",
            "Ciclo 3",
            "correo_verificado",
            true
        );
    }

    protected Map<String, Object> propuesta(LocalDateTime fecha) {
        return fila(
            "receptor_id",
            202L,
            "materia_id",
            10L,
            "mentor_id",
            101L,
            "fecha_hora",
            fecha.toString(),
            "duracion_programada_min",
            60
        );
    }

    protected static Map<String, Object> fila(Object... pares) {
        Map<String, Object> datos = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) datos.put(
            pares[i].toString(),
            pares[i + 1]
        );
        return datos;
    }
}
