# StudyMatch Grupo 1
Equipo_1 Universidad Tecnológica del Perú

# Nombre Proyecto
Sistema de Emparejamiento y Seguimiento de Mentorías - UTP Lima Sur

## Integrantes
- Untiveros Vargas Yummy Lucero
- Quihui Rodriguez Frank Amadeo
- Mostacero Cortez Orlando
- Regalado Vasquez Kevin Jean Pierre

## Carrera
- Ingenieria de sistemas e informatica


Plataforma web para el intercambio de tutorías entre estudiantes de la Universidad Tecnológica del Perú. Permite encontrar compañeros con materias y horarios compatibles, coordinar sesiones de estudio y registrar el progreso académico.

## Funcionalidades

- Registro con correo institucional y verificación por código.
- Gestión del perfil, materias y disponibilidad.
- Búsqueda y recomendaciones de compañeros.
- Programación y seguimiento de sesiones.
- Chat y notificaciones.
- Registro de horas de mentoría, calificaciones e insignias.
- Gestión del Plan Gratuito y del Plan Pro-Ciclo.
- Generación de reportes, constancias y certificados PDF.

## Tecnologías

- Java y Spring Boot.
- Maven.
- Thymeleaf, HTML, CSS y JavaScript.
- PostgreSQL alojado en Supabase.

## Organización del proyecto

El código se encuentra en la carpeta `studymatch_Grupo1`.

- `controller`: recibe las solicitudes de la aplicación.
- `service`: contiene las reglas de negocio.
- `dao`: define el acceso a los datos.
- `dao/jdbc`: implementa las consultas a PostgreSQL.
- `model`: contiene los modelos de datos.
- `resources/templates`: contiene las vistas Thymeleaf.
- `resources/static`: contiene los estilos, imágenes y JavaScript.
- `src/test`: contiene pruebas del proyecto.

## Requisitos

- JDK 17 o superior.
- Maven.
- Conexión a Supabase.
- Servicio SMTP para enviar correos de verificación y recuperación.

## Configuración

Antes de iniciar, configurar las siguientes variables de entorno:

- `SUPABASE_DB_URL`
- `SUPABASE_DB_USERNAME`
- `SUPABASE_DB_PASSWORD`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`

La URL de conexión debe utilizar el formato JDBC de PostgreSQL.

También se admite un archivo privado
`src/main/resources/application-local.properties`.
Este archivo está excluido de Git y no debe publicarse.

## Ejecución

Desde la carpeta del repositorio:

    cd studymatch_Grupo1
    mvn spring-boot:run

Abrir en el navegador:

    http://localhost:8080/

En NetBeans, abrir `studymatch_Grupo1` como proyecto Maven.

## Planes

- **Gratuito:** hasta 5 emparejamientos mensuales y búsqueda básica.
- **Pro-Ciclo:** emparejamientos ilimitados durante su vigencia,
  filtros avanzados y acceso al ranking.

El procesamiento de pagos es de demostración y no realiza cobros reales.
La aprobación permite guardar y activar la suscripción en la plataforma.

## Trabajo colaborativo

Cada integrante trabaja en su rama personal. Los cambios se integran
en `main` mediante Pull Requests.

