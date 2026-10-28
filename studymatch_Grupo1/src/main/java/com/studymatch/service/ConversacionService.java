package com.studymatch.service;

import static com.studymatch.service.Validaciones.id;
import static com.studymatch.service.Validaciones.text;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.studymatch.dao.ConversacionDAO;
import com.studymatch.dao.EstudianteDAO;
import com.studymatch.dao.NotificacionDAO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Mantiene una conversación por pareja y valida el acceso a sus mensajes. */
@Service
public class ConversacionService {

    private final ConversacionDAO conversacionDAO;
    private final EstudianteDAO estudianteDAO;
    private final NotificacionDAO notificacionDAO;
    private final ModeracionService moderacion;
    public static final String MODERACION_PREFIX="[STUDYMATCH_MODERACION]";
    private record Lectura(long conversacion, java.time.Instant instante) {}
    private final java.util.concurrent.ConcurrentMap<Long,Lectura> lectores =
        new java.util.concurrent.ConcurrentHashMap<>();

    public ConversacionService(
        ConversacionDAO conversacionDAO,
        EstudianteDAO estudianteDAO,
        NotificacionDAO notificacionDAO,
        ModeracionService moderacion
    ) {
        this.conversacionDAO = conversacionDAO;
        this.estudianteDAO = estudianteDAO;
        this.notificacionDAO = notificacionDAO;
        this.moderacion=moderacion;
    }

    /** Comprueba que el estudiante sea participante de la conversación. */
    public Map<String, Object> conversation(long student, long conversation) {
        var row = conversacionDAO.buscarParticipante(conversation, student);
        row.put("estado", "ACTIVA");
        return row;
    }

    /** Reutiliza el chat de la pareja o crea uno si todavía no existe. */
    @Transactional
    public long directConversation(long student, long other) {
        if (student == other) throw new ResponseStatusException(
            BAD_REQUEST,
            "Selecciona otro estudiante."
        );
        estudianteDAO.perfil(other);
        estudianteDAO.bloquear(Math.min(student, other));
        var rows = conversacionDAO.buscarDirecta(student, other);
        if (!rows.isEmpty()) return id(rows.get(0), "id");
        return conversacionDAO.crearDirecta(student, other);
    }

    /** Marca como leídos los mensajes recibidos y reúne el historial de la pareja. */
    @Transactional
    public List<Map<String, Object>> messages(long student, long conversation) {
        var chat = conversation(student, conversation);
        lectores.put(student,new Lectura(conversation,java.time.Instant.now()));
        long first = id(chat, "estudiante_a"),
            second = id(chat, "estudiante_b");
        conversacionDAO.marcarMensajesLeidos(first, second, student);
        var messages = new java.util.ArrayList<Map<String,Object>>();
        for(var original:conversacionDAO.listarMensajes(first,second)){
            var copia=new java.util.LinkedHashMap<String,Object>(original);
            if(Objects.toString(copia.get("texto"),"").startsWith(MODERACION_PREFIX)){
                if(((Number)copia.get("emisor_id")).longValue()!=student)continue;
                copia.put("texto","[Mensaje retenido para revisión]");copia.put("estado_moderacion","PENDIENTE");
            }
            messages.add(copia);
        }
        for (var message : messages) {
            var file = DocumentService.chatFile(message.get("texto").toString());
            if (file != null) {
                message.put("texto", "");
                message.put(
                    "archivo",
                    Map.of(
                        "nombre",
                        file.nombre(),
                        "tipo",
                        file.tipo(),
                        "url",
                        "/api/conversaciones/" +
                            message.get("conversacion_id") +
                            "/mensajes/" +
                            message.get("id") +
                            "/archivo"
                    )
                );
            }
        }
        return messages;
    }

    /** Valida y guarda el mensaje, y avisa al otro participante. */
    @Transactional(noRollbackFor=MensajeEnRevisionException.class)
    public long message(long student, long conversation, Map<String, Object> input) {
        var row = conversation(student, conversation);
        if (!"ACTIVA".equals(row.get("estado"))) throw new ResponseStatusException(
            CONFLICT,
            "Esta conversación está cerrada."
        );
        String text = text(input, "texto", 2000);
        if (text.startsWith(DocumentService.FILE_PREFIX)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Usa el botón de adjuntar para compartir archivos."
        );
        if(text.startsWith(MODERACION_PREFIX))throw new ResponseStatusException(BAD_REQUEST,"El formato del mensaje no es válido.");
        if(moderacion.requiereRevision(text)){
            conversacionDAO.crearMensaje(conversation,student,MODERACION_PREFIX+text);
            throw new MensajeEnRevisionException();
        }
        long message = conversacionDAO.crearMensaje(conversation, student, text);
        long other =
            id(row, "estudiante_a") == student ? id(row, "estudiante_b") : id(row, "estudiante_a");
        Lectura lectura=lectores.get(other);
        boolean viendo=lectura!=null && lectura.conversacion()==conversation
            && lectura.instante().isAfter(java.time.Instant.now().minusSeconds(15));
        if(!viendo) notificacionDAO.crear(
            other,
            "Nuevo mensaje",
            (String) (estudianteDAO.perfil(student).get("nombre") + " te envió un mensaje."),
            "SISTEMA",
            conversation
        );
        return message;
    }

    public Object conversations(long student) {
        return conversacionDAO.listarDelEstudiante(student);
    }
}
