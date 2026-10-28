"use strict";

// Presentación del ZIP: consume los mismos controladores y servicios reales.
let displayMode = "APRENDER",
    notificationRows = [],
    designSearch = 0;
const baseProfile = profile,
    baseSubjects = loadMySubjects,
    baseSessions = loadSessions,
    baseHours = loadHours;
profile = function () {
    baseProfile();
    if (!currentUser) return;
    const avatar = $("topbar-user-avatar");
    avatar.textContent = initials(currentUser.nombre);
    if (currentUser.foto_url) {
        const photo = document.createElement("img");
        photo.src = "/api/perfil/foto";
        photo.alt = "Tu foto de perfil";
        photo.style.cssText = "width:100%;height:100%;border-radius:50%;object-fit:cover;";
        avatar.replaceChildren(photo);
    }
    avatar.title = currentUser.nombre;
    updateMode();
};
loadMySubjects = async function () {
    await baseSubjects();
    for (const [role, target] of [
        ["ENSENAR", "perfil-showcase-teach"],
        ["APRENDER", "perfil-showcase-learn"],
    ]) {
        const rows = mySubjects.filter((s) => s.tipo_rol === role);
        $(target).innerHTML = rows.length
            ? rows
                  .map(
                      (s) =>
                          `<span class="tag ${role === "ENSENAR" ? "tag-purple" : "tag-green"}">${escapeHtml(s.nombre)}</span>`,
                  )
                  .join("")
            : empty("Todavía no agregaste materias.");
    }
};
loadSessions = async function () {
    await baseSessions();
    updateSessionPanels();
};
loadHours = async function () {
    await baseHours();
    updateSessionPanels();
};
function updateMode() {
    const mentor = displayMode === "ENSENAR";
    show("dash-aprendiz-panel", !mentor);
    show("dash-mentor-panel", mentor);
    setText("txt-modo-rol", mentor ? "Modo Mentor" : "Modo Aprendiz");
    setText("dash-mode-badge", mentor ? "Modo Mentor Activo" : "Modo Aprendiz Activo");
    setText(
        "quick-match-headline",
        mentor ? "¿En qué curso puedes ayudar hoy?" : "¿En qué curso necesitas apoyo hoy?",
    );
    setText(
        "dash-mode-desc",
        mentor
            ? "Revisa las solicitudes de apoyo y las materias que puedes enseñar."
            : "Revisa tus próximas actividades y encuentra apoyo en tus materias.",
    );
    const toggle = $("btn-toggle-rol");
    toggle.setAttribute("aria-pressed", String(mentor));
    toggle.lastElementChild.textContent = mentor ? "⮂ Modo Aprendiz" : "⮂ Modo Mentor";
    updateSessionPanels();
}
function updateSessionPanels() {
    if (!currentUser) return;
    const requests = sessions.filter(
        (s) =>
            s.estado === "PROPUESTA" &&
            Number(s.receptor_id) === Number(currentUser.id) &&
            Number(s.mentor_id) === Number(currentUser.id),
    );
    setText("mentor-pending-num", requests.length);
    $("mentor-requests").innerHTML = requests.length
        ? requests.map(sessionCard).join("")
        : empty("No tienes solicitudes pendientes para enseñar.");
    const completed = sessions.filter((s) => s.estado === "COMPLETADA");
    setText("profile-history-count", completed.length + " sesiones registradas");
    $("profile-history-live").innerHTML = completed.length
        ? completed
              .map(
                  (s) =>
                      `<div class="session-item"><div class="session-avatar" style="background:var(--primary);">${escapeHtml(initials(s.companero))}</div><div class="session-info"><h4>${escapeHtml(s.materia)}</h4><p>${escapeHtml(s.companero)} · ${escapeHtml(dateText(s.fecha_hora))}</p></div><span class="badge badge-success">${Number(s.duracion_real_min)} min</span></div>`,
              )
              .join("")
        : empty("Todavía no hay sesiones completadas.");
}
studentCard = function (s) {
    const relevant = displayMode === "APRENDER" ? s.materias_ensenar : s.materias_aprender;
    const score = s.coincidencia_materias,
        label = score === null ? "—" : Number(score) + "%";
    return `<div class="suggested-card" style="border:1px solid var(--border-color);border-radius:14px;padding:18px;background:#fff;display:flex;flex-direction:column;justify-content:space-between;gap:14px;box-shadow:0 2px 8px rgba(0,0,0,.03);transition:all .2s ease;">
    <div><div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px;gap:12px;"><div style="display:flex;align-items:center;gap:12px;"><div class="user-avatar-square" style="width:44px;height:44px;border-radius:12px;background:var(--primary);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:15px;flex-shrink:0;box-shadow:0 2px 8px rgba(0,0,0,.12);">${escapeHtml(initials(s.nombre))}</div><div><h4 style="font-size:15px;margin:0 0 2px;color:#0f172a;font-weight:700;">${escapeHtml(s.nombre)}</h4><p style="font-size:12px;color:var(--text-muted);margin:0;">${escapeHtml(s.carrera || "")} · ${escapeHtml(s.ciclo || "")}</p></div></div><div style="background:rgba(46,204,113,.12);color:#16a34a;padding:4px 8px;border-radius:20px;font-weight:700;font-size:12px;white-space:nowrap;"><i class="fa-solid fa-bolt"></i> ${label}</div></div>
    <div style="font-size:12px;color:var(--text-muted);margin-bottom:12px;">${displayMode === "APRENDER" ? "Puede ayudarte en" : "Busca apoyo en"}: <strong style="color:#0f172a;font-weight:600;">${escapeHtml((relevant || []).join(", ") || "Sin materias registradas")}</strong></div><div class="match-bar-container" style="display:flex;align-items:center;gap:10px;margin-bottom:4px;"><div class="match-bar-bg" style="flex:1;height:6px;background:#e2e8f0;border-radius:4px;overflow:hidden;"><div class="progress-bar-fill" style="height:100%;background:var(--primary);width:${Number(score) || 0}%;border-radius:4px;"></div></div><div class="match-val" style="font-size:11px;font-weight:700;color:var(--primary);">${label}</div></div></div>
    <button class="btn-light" style="width:100%;padding:10px;font-size:13px;font-weight:700;border-radius:8px;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:6px;" data-student="${s.id}">Ver perfil →</button></div>`;
};
loadSearch = async function () {
    if (!currentUser) return;
    const request = ++designSearch;
    const access = await api("/suscripciones/acceso");
    for (const id of ["quick-filter-day", "filter-career"]) {
        const field = $(id);
        field.disabled = !access.pro;
        field.title = access.pro ? "" : "Filtro avanzado disponible con Pro-Ciclo";
        if (!access.pro) field.value = id === "filter-career" ? "Todos" : "";
    }
    const params = new URLSearchParams({
        q: $("search-input").value.trim(),
        curso: $("quick-filter-course").value.trim(),
        dia: $("quick-filter-day").value,
        carrera: $("filter-career").value === "Todos" ? "" : $("filter-career").value,
        rol: displayMode,
    });
    const [all, recommended] = await Promise.all([
        api("/estudiantes?" + params),
        api("/estudiantes?" + params + "&recomendados=true"),
    ]);
    if (request !== designSearch) return;
    $("search-results").innerHTML = all.length
        ? all.map(studentCard).join("")
        : empty("No se encontraron compañeros con estos filtros.");
    setText("quick-match-count", all.length + " compañeros encontrados");
    const cards = recommended.length
        ? recommended.slice(0, 3).map(studentCard).join("")
        : empty("No hay compañeros con materias y horarios compatibles para estos filtros.");
    $("suggestions-container").innerHTML = cards;
    $("dashboard-suggested").innerHTML = cards;
    show("btn-clear-search", !!$("search-input").value);
};
async function loadQuickCourses() {
    const first = await api("/materias");
    for (let page = 2; first.items.length < first.total; page++) {
        const next = await api("/materias?pagina=" + page);
        if (!next.items.length) break;
        first.items.push(...next.items);
    }
    $("quick-courses-list").replaceChildren(
        ...first.items.map((row) => new Option(row.nombre, row.nombre)),
    );
}
function renderNotificationDropdown() {
    const onlyUnread = $("toggle-notif-no-leidos").checked,
        rows = notificationRows.filter((n) => !onlyUnread || !n.leida);
    const count = notificationRows.filter((n) => !n.leida).length;
    setText("topbar-notif-count-pill", count + " nuevas");
    show("topbar-punto-rojo-campana", count > 0, "inline-block");
    $("notif-scroll-list").innerHTML = rows.length
        ? rows
              .map(
                  (n) =>
                      `<button type="button" class="notif-item ${n.leida ? "notif-read" : "notif-unread"}" data-design-notification="${n.id}" style="display:block;width:100%;text-align:left;border:0;background:white;"><strong style="display:block;font-size:13px;color:var(--text-dark);">${n.leida ? "" : "● "}${escapeHtml(n.titulo)}</strong><span style="display:block;font-size:12px;color:var(--text-muted);margin:6px 0;">${escapeHtml(n.mensaje)}</span><small>${escapeHtml(dateText(n.creada_en))}</small></button>`,
              )
              .join("")
        : empty("No tienes notificaciones para mostrar.");
}
loadNotifications = async function () {
    notificationRows = await api("/notificaciones");
    $("notifications-live").innerHTML = notificationRows.length
        ? notificationRows
              .map(
                  (n) =>
                      `<div class="section-card" style="margin:0;${n.leida ? "" : "border-left:4px solid var(--primary);"}"><h3 style="font-size:15px;">${escapeHtml(n.titulo)}</h3><p style="font-size:13px;margin:8px 0;color:var(--text-muted);">${escapeHtml(n.mensaje)}</p><small>${escapeHtml(dateText(n.creada_en))}</small><div style="display:flex;gap:10px;margin-top:12px;">${!n.leida ? `<button class="btn btn-secondary" data-read="${n.id}">Marcar como leída</button>` : ""}${n.tipo_evento === "PROPUESTA_SESION" ? '<button class="btn btn-primary" data-go-sessions>Ver sesiones</button>' : ""}</div></div>`,
              )
              .join("")
        : empty("No tienes notificaciones.");
    show(
        "rf38-badge-sidebar-notif",
        notificationRows.some((n) => !n.leida),
        "inline-flex",
    );
    show(
        "rf38-punto-rojo-campana",
        notificationRows.some((n) => !n.leida),
    );
    renderNotificationDropdown();
};
async function loadCertificate() {
    const data = await api("/perfil/constancia"),
        h = data.horas,
        u = data.perfil,
        ready =
            Number(h.sesiones_completadas) > 0 &&
            Number(h.horas_ensenadas) + Number(h.horas_aprendidas) > 0;
    setText("rf49-student-nombre", u.nombre);
    setText("rf49-student-carrera", [u.ciclo, u.carrera].filter(Boolean).join(" · "));
    setText("rf49-avatar-badge", initials(u.nombre));
    setText("rf49-cert-nombre", u.nombre);
    setText("rf49-cert-horas", (Number(h.horas_ensenadas) + Number(h.horas_aprendidas)).toFixed(1));
    setText("rf49-cert-ciclo", "Historial acumulado");
    setText(
        "rf49-cert-calif",
        data.resumen.calificacion_promedio === null
            ? "Sin calificaciones"
            : Number(data.resumen.calificacion_promedio).toFixed(1) + " / 5",
    );
    setText(
        "rf49-cert-fecha",
        new Date(data.emitido_en + "T12:00:00").toLocaleDateString("es-PE", { dateStyle: "long" }),
    );
    setText("certificate-state", ready ? "Registro disponible" : "Sin constancia emitida");
    show("rf49-preview-doc", ready);
    show("rf50-alerta-no-disponible", !ready, "flex");
    $("btn-descargar-cert-pdf").disabled = !ready;
}
const faqs = [
    [
        "¿Cómo encuentro un compañero?",
        "En Buscar Compañeros puedes filtrar por materia, carrera y día. Las recomendaciones requieren materias complementarias y al menos una hora de disponibilidad en común.",
    ],
    [
        "¿Cómo propongo una sesión desde el chat?",
        "Abre una conversación y pulsa Proponer sesión. También puedes hacer clic derecho sobre una conversación. Solo podrás elegir materias donde uno enseñe y el otro aprenda.",
    ],
    [
        "¿Cómo verifico mi correo?",
        "Regístrate con tu correo @utp.edu.pe e ingresa el código recibido. Vence en diez minutos. Si el envío falla puedes volver a intentar; revisa también la carpeta de spam.",
    ],
    [
        "¿Cómo recupero mi contraseña?",
        "En la pantalla de acceso, pulsa Olvidé mi contraseña. El enlace de recuperación recibido por correo vence en treinta minutos.",
    ],
    [
        "¿Cómo se registran las horas?",
        "Las horas se calculan cuando ambos participantes confirman la finalización de una sesión. El tiempo registrado corresponde a la duración calculada por el servidor.",
    ],
    [
        "¿Se realizan cobros reales?",
        "La pasarela está en modo de demostración: no cobra dinero real. Al aprobar la simulación se activa Pro-Ciclo y se guarda un comprobante identificado como DEMO.",
    ],
    [
        "¿Cómo consigo insignias?",
        "Debes alcanzar las metas de horas enseñadas que estén registradas en el catálogo de insignias. El sistema las asigna al completar sesiones.",
    ],
];
function renderFaq() {
    const query = $("faq-search-input").value.trim().toLocaleLowerCase(),
        rows = faqs.filter((row) => row.join(" ").toLocaleLowerCase().includes(query));
    $("faq-accordion-container").innerHTML = rows
        .map(
            ([question, answer]) =>
                `<details class="section-card" style="padding:18px;margin-bottom:12px;"><summary style="font-size:14px;font-weight:600;cursor:pointer;">${escapeHtml(question)}</summary><p style="font-size:13px;line-height:1.7;color:var(--text-muted);margin-top:12px;">${escapeHtml(answer)}</p></details>`,
        )
        .join("");
    show("faq-no-match-alert", !rows.length);
}
function selectProfileTab(id) {
    document
        .querySelectorAll("#perfil-main-tabs .tab")
        .forEach((e) => e.classList.toggle("active", e.dataset.target === id));
    document
        .querySelectorAll("#perfil-section > .perfil-tab-content")
        .forEach((e) => e.classList.toggle("active", e.id === id));
    if (id === "tab-historial-sesiones") loadSessions().catch((e) => notify(e.message, true));
    if (id === "tab-certificados-logros") loadCertificate().catch((e) => notify(e.message, true));
}
window.navegarASeccion = (id) => navigate(id);
window.designNavigated = (id) => {
    if (id === "buscar-section") loadQuickCourses().catch((e) => notify(e.message, true));
    if (id === "perfil-section") updateSessionPanels();
};
document.addEventListener("DOMContentLoaded", () => {
    on("btn-toggle-rol", async () => {
        displayMode = displayMode === "APRENDER" ? "ENSENAR" : "APRENDER";
        updateMode();
        await loadSearch();
    });
    on("rf38-btn-campana-topbar", async () => {
        const open = $("topbar-notif-dropdown").style.display !== "none";
        show("topbar-notif-dropdown", !open);
        if (!open) await loadNotifications();
    });
    on("btn-close-notif-dropdown", () => show("topbar-notif-dropdown", false));
    $("toggle-notif-no-leidos").addEventListener("change", renderNotificationDropdown);
    on("btn-quick-search-apply", loadSearch);
    $("quick-filter-course").addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            loadSearch().catch((e) => notify(e.message, true));
        }
    });
    on("btn-preview-public-profile", () => otherProfile(currentUser.id));
    $("topbar-user-avatar").setAttribute("role", "button");
    $("topbar-user-avatar").tabIndex = 0;
    on("topbar-user-avatar", () => navigate("perfil-section"));
    $("topbar-user-avatar").addEventListener("keydown", (event) => {
        if (event.key === "Enter") navigate("perfil-section");
    });
    for (const tab of document.querySelectorAll("#perfil-main-tabs .tab")) {
        tab.setAttribute("role", "button");
        tab.tabIndex = 0;
        tab.addEventListener("click", () => selectProfileTab(tab.dataset.target));
        tab.addEventListener("keydown", (event) => {
            if (event.key === "Enter") selectProfileTab(tab.dataset.target);
        });
    }
    $("faq-search-input").addEventListener("input", renderFaq);
    on("btn-faq-clear-search", () => {
        $("faq-search-input").value = "";
        renderFaq();
    });
    renderFaq();
    on("btn-descargar-cert-pdf", async () => {
        const boton=$("btn-descargar-cert-pdf");
        const titulo=boton.textContent;boton.disabled=true;boton.textContent="Generando PDF…";
        try{
            const documentos=await api("/certificados");
            const url=documentos.length?"/api/certificados/"+encodeURIComponent(documentos[0].codigo)+"/pdf":"/api/perfil/constancia/pdf";
            await downloadDocument(url,documentos.length?"Certificado_StudyMatch.pdf":"Constancia_StudyMatch.pdf");
        }finally{boton.textContent=titulo;boton.disabled=false;}
    });
    window.addEventListener("afterprint", () => delete document.body.dataset.print);
    const reportModal = () => show("modal-reportar-usuario", false),
        successModal = () => show("modal-reporte-exito", false);
    for (const id of ["btn-cancelar-reporte", "btn-cerrar-modal-reporte"]) on(id, reportModal);
    on("btn-cerrar-exito-reporte", successModal);
    on("btn-reportar-companero", () => {
        if (!selectedStudent || selectedStudent.id === currentUser.id) return;
        setText("reporte-modal-titulo", "Reportar a " + selectedStudent.nombre);
        $("form-reporte-usuario").reset();
        show("modal-companero-perfil", false);
        show("modal-reportar-usuario", true, "flex");
    });
    $("form-soporte-ticket").addEventListener(
        "submit",
        handle(async () => {
            const button = $("btn-enviar-soporte");
            if (button.disabled) return;
            button.disabled = true;
            try {
                const result = await api("/soporte", "POST", {
                    categoria: $("soporte-categoria").value,
                    asunto: $("soporte-asunto").value,
                    mensaje: $("soporte-mensaje").value,
                });
                setText("soporte-ticket-codigo", result.codigo);
                show("soporte-ticket-confirmacion", true);
                $("form-soporte-ticket").reset();
                notify("Solicitud registrada con estado pendiente.");
            } finally {
                button.disabled = false;
            }
        }),
    );
    $("form-reporte-usuario").addEventListener(
        "submit",
        handle(async () => {
            if (!selectedStudent) throw new Error("Selecciona un compañero.");
            const button = $("btn-enviar-reporte");
            if (button.disabled) return;
            button.disabled = true;
            try {
                const result = await api("/reportes-usuario", "POST", {
                    reportadoId: selectedStudent.id,
                    categoria: $("reporte-motivo").value,
                    mensaje: $("reporte-descripcion").value,
                });
                reportModal();
                setText(
                    "reporte-exito-titulo",
                    result.existente ? "Reporte pendiente existente" : "Reporte registrado",
                );
                setText(
                    "reporte-exito-descripcion",
                    "Código " + result.codigo + ". Estado: pendiente de revisión.",
                );
                show("modal-reporte-exito", true, "flex");
            } finally {
                button.disabled = false;
            }
        }),
    );
    document.addEventListener(
        "click",
        handle(async (event) => {
            const target = event.target.closest(
                "[data-go],[data-action],[data-peer-message],[data-design-notification]",
            );
            if (!target) return;
            if (target.dataset.go) {
                await navigate(target.dataset.go);
                return;
            }
            if (target.dataset.peerMessage) {
                const row = await api("/conversaciones", "POST", {
                    estudiante_id: Number(target.dataset.peerMessage),
                });
                await loadConversations();
                await openConversation(row.id);
                await navigate("chat-section");
                return;
            }
            if (target.dataset.designNotification) {
                const row = notificationRows.find(
                    (n) => String(n.id) === target.dataset.designNotification,
                );
                if (!row) return;
                await api("/notificaciones/" + row.id + "/leer", "POST");
                show("topbar-notif-dropdown", false);
                await navigate(
                    row.tipo_evento === "PROPUESTA_SESION"
                        ? "mis-sesiones-section"
                        : "notificaciones-section",
                );
                return;
            }
            if (target.dataset.action === "peer-chat") await window.irAlChatDesdeCompanero();
            if (target.dataset.action === "peer-propose")
                await window.proponerSesionDesdeCompanero();
            if (target.dataset.action === "peer-close") window.cerrarModalCompanero();
        }),
    );
    // Ajustes del perfil desde el menú de notificaciones y la vista previa propia.
    for (const icon of document.querySelectorAll("#topbar-notif-dropdown .fa-gear")) {
        icon.setAttribute("role", "button");
        icon.tabIndex = 0;
        icon.addEventListener("click", () => {
            show("topbar-notif-dropdown", false);
            navigate("perfil-section");
        });
    }
    for (const button of document.querySelectorAll("#modal-comp-actions-self button"))
        button.addEventListener("click", () => {
            window.cerrarModalCompanero();
            navigate("perfil-section");
            selectProfileTab("tab-informacion");
        });
});
