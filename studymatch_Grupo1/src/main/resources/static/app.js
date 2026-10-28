"use strict";

// La vista conserva el diseño. Cada lectura y escritura se realiza mediante el backend.
const $ = (id) => document.getElementById(id);
const escapeHtml = (value) =>
    String(value ?? "").replace(
        /[&<>"']/g,
        (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c],
    );
const initials = (name) =>
    String(name || "")
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((x) => x[0])
        .join("")
        .toUpperCase();
const setText = (id, value) => {
    if ($(id)) $(id).textContent = value ?? "—";
};
const show = (id, visible, display = "block") => {
    if ($(id)) $(id).style.display = visible ? display : "none";
};
const dateText = (value) =>
    value
        ? new Date(value).toLocaleString("es-PE", {
              timeZone: "America/Bogota",
              dateStyle: "medium",
              timeStyle: "short",
          })
        : "—";
const empty = (text) =>
    `<div style="padding:24px;text-align:center;color:var(--text-muted);grid-column:1/-1;">${escapeHtml(text)}</div>`;
let csrf = "",
    currentUser = null,
    mySubjects = [],
    sessions = [],
    badges = [],
    conversations = [],
    subscriptions = [],
    plans = [];
let selectedStudent = null,
    selectedConversation = null,
    selectedRating = null,
    selectedStars = 0,
    selectedCancellation = null,
    pastSessions = false;
let savedSchedule = [],
    selectedSlots = new Set(),
    poll = null,
    statusTimer = null;

let catalogRequest = 0;
async function loadCatalogs() {
    const request = ++catalogRequest;
    const ids = ["reg-career", "perfil-carrera", "reg-cycle", "perfil-ciclo"];
    const selected = Object.fromEntries(ids.map((id) => [id, $(id).value]));
    for (const id of ids) {
        $(id).replaceChildren(new Option("Cargando opciones…", ""));
        $(id).disabled = true;
    }
    $("btn-register").disabled = true;
    $("btn-save-profile").disabled = true;
    $("filter-career").replaceChildren(new Option("Todas las carreras", "Todos"));
    try {
        const catalogs = await api("/auth/catalogos");
        if (request !== catalogRequest) return;
        for (const [id, rows, label] of [
            ["reg-career", catalogs.carreras, "carreras"],
            ["perfil-carrera", catalogs.carreras, "carreras"],
            ["reg-cycle", catalogs.ciclos, "ciclos"],
            ["perfil-ciclo", catalogs.ciclos, "ciclos"],
        ]) {
            const select = $(id);
            select.replaceChildren(
                new Option(rows.length ? "Selecciona…" : `No hay ${label} registradas`, ""),
            );
            for (const row of rows) select.add(new Option(row.nombre, row.nombre));
            if (rows.some((row) => row.nombre === selected[id])) select.value = selected[id];
            select.disabled = !rows.length;
        }
        const unavailable = !catalogs.carreras.length || !catalogs.ciclos.length;
        $("btn-register").disabled = unavailable;
        $("btn-save-profile").disabled = unavailable;
        for (const row of catalogs.carreras)
            $("filter-career").add(new Option(row.nombre, row.nombre));
    } catch (error) {
        if (request !== catalogRequest) return;
        for (const id of ids)
            $(id).replaceChildren(new Option("No se pudo cargar el catálogo", ""));
        throw error;
    }
}

function notify(text, error = false) {
    const box = $("live-status");
    box.textContent = text;
    box.style.background = error ? "#8e1230" : "#161616";
    box.style.display = "block";
    clearTimeout(statusTimer);
    statusTimer = setTimeout(() => (box.style.display = "none"), 7000);
}
window.showToast = (text, type) => notify(text, type === "error");
async function api(path, method = "GET", data) {
    const headers = {};
    const options = { method, headers, credentials: "same-origin", cache: "no-store" };
    if (method !== "GET") headers["X-CSRF-Token"] = csrf;
    if (data instanceof FormData) options.body = data;
    else if (data !== undefined) {
        headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(data);
    }
    const response = await fetch("/api" + path, options);
    const result = await response
        .json()
        .catch(() => ({ error: "No se pudo interpretar la respuesta del servidor." }));
    if (!response.ok) {
        if (response.status === 401 && currentUser) {
            loggedOut();
            await token();
        }
        throw new Error(result.error || result.message || "No se pudo completar la operación.");
    }
    return result;
}
const handle = (fn) => async (event) => {
    if (event?.currentTarget !== document) event?.preventDefault();
    const button = event?.currentTarget;
    if (button?.disabled) return;
    if (button?.tagName === "BUTTON") button.disabled = true;
    try {
        await fn(event);
    } catch (error) {
        notify(error.message, true);
    } finally {
        if (button?.tagName === "BUTTON" && !button.hasAttribute("data-managed-disabled"))
            button.disabled = false;
    }
};
const on = (id, fn, event = "click") => {
    if ($(id)) $(id).addEventListener(event, handle(fn));
};
// Enter ejecuta el mismo botón del formulario; los selectores y textos largos conservan su teclado.
function setupEnterActions() {
    const actions = {
        "login-email": "btn-login",
        "login-password": "btn-login",
        "reg-name": "btn-register",
        "reg-email": "btn-register",
        "reg-password": "btn-register",
        "recover-email": "btn-recover",
        "reset-password": "btn-reset",
        "verify-code": "btn-verify",
        "perfil-nombre": "btn-save-profile",
        "chat-input-mensaje": "btn-chat-enviar",
    };
    for (const [id, action] of Object.entries(actions))
        $(id)?.addEventListener("keydown", (event) => {
            if (
                event.key !== "Enter" ||
                event.isComposing ||
                event.repeat ||
                event.shiftKey ||
                event.ctrlKey ||
                event.altKey ||
                event.metaKey
            )
                return;
            event.preventDefault();
            const button = $(action);
            if (button && !button.disabled && button.getClientRects().length) button.click();
        });
}
let dialogResolve = null,
    dialogIsLink = false,
    dialogFocus = null;
function appDialog({ title, description, link = false, value = "", confirm = "Confirmar" }) {
    if (dialogResolve) return Promise.resolve(null);
    dialogIsLink = link;
    dialogFocus = document.activeElement;
    setText("app-dialog-title", title);
    setText("app-dialog-description", description);
    setText("app-dialog-submit", confirm);
    $("app-dialog-icon").className = link ? "fa-solid fa-link" : "fa-solid fa-circle-question";
    $("app-dialog-field").hidden = !link;
    $("app-dialog-input").value = value;
    setText("app-dialog-error", "");
    return new Promise((resolve) => {
        dialogResolve = resolve;
        $("app-dialog").showModal();
        (link ? $("app-dialog-input") : $("app-dialog-cancel")).focus();
    });
}
function setupAppDialog() {
    const dialog = $("app-dialog");
    for (const id of ["app-dialog-close", "app-dialog-cancel"])
        $(id).addEventListener("click", () => dialog.close());
    dialog.addEventListener("close", () => {
        const resolve = dialogResolve;
        dialogResolve = null;
        resolve?.(null);
        if (dialogFocus?.isConnected) dialogFocus.focus();
    });
    $("app-dialog-form").addEventListener("submit", (event) => {
        event.preventDefault();
        let value = true;
        if (dialogIsLink) {
            value = $("app-dialog-input").value.trim();
            try {
                const url = new URL(value);
                if (url.protocol !== "https:" || !url.hostname || url.username || url.password)
                    throw new Error();
            } catch {
                setText("app-dialog-error", "Pega un enlace válido que empiece por https://.");
                $("app-dialog-input").focus();
                return;
            }
        }
        const resolve = dialogResolve;
        dialogResolve = null;
        dialog.close();
        resolve?.(value);
    });
}
function authPage(id) {
    if (id !== "verify-section") {
        ++verificationRequest;
        clearInterval(verificationTimer);
    }
    document
        .querySelectorAll("#auth-wrapper .auth-box")
        .forEach((e) => (e.style.display = e.id === id ? "block" : "none"));
}
async function token() {
    csrf = (await api("/auth/csrf")).token;
}
function loggedOut() {
    clearInterval(poll);
    currentUser = null;
    mySubjects = [];
    sessions = [];
    badges = [];
    conversations = [];
    subscriptions = [];
    plans = [];
    selectedStudent = null;
    selectedConversation = null;
    selectedRating = null;
    savedSchedule = [];
    selectedSlots.clear();
    document
        .querySelectorAll('[id^="modal-"],#rf38-modal-popup-vencimiento,#rf46-toast-msg')
        .forEach((e) => (e.style.display = "none"));
    show("app-container", false);
    show("auth-wrapper", true, "flex");
    authPage("login-section");
}
function profile() {
    if (!currentUser) return;
    const u = currentUser;
    setText("sidebar-name", u.nombre);
    setText("sidebar-subtitle", [u.ciclo, u.carrera].filter(Boolean).join(" · "));
    setText("card-nombre", u.nombre);
    setText("card-carrera", u.carrera);
    setText("card-ciclo", u.ciclo);
    setText("dash-greeting", `Bienvenido de nuevo, ${u.nombre.split(" ")[0]}`);
    for (const id of ["sidebar-avatar", "card-avatar", "form-avatar"]) {
        const e = $(id);
        e.textContent = initials(u.nombre);
        if (u.foto_url) {
            const image = document.createElement("img");
            image.src = "/api/perfil/foto?v=" + Date.now();
            image.alt = "Tu foto de perfil";
            image.style.cssText = "width:100%;height:100%;object-fit:cover;border-radius:inherit;";
            e.replaceChildren(image);
        }
    }
    $("perfil-nombre").value = u.nombre || "";
    $("perfil-bio").value = u.bio || "";
    for (const [id, value] of [
        ["perfil-carrera", u.carrera],
        ["perfil-ciclo", u.ciclo],
    ]) {
        const el = $(id);
        el.value = Array.from(el.options).some((o) => o.value === value) ? value : "";
    }
}
let verificationRequest = 0,
    verificationTimer = null,
    verificationEmail = "";
function verificationDelivery(sent, seconds = 0, failed = false) {
    const button = $("btn-resend-code");
    clearInterval(verificationTimer);
    button.dataset.label = failed ? "Volver a intentar" : "Reenviar código";
    $("btn-verify").disabled = !sent;
    const deadline = Date.now() + Number(seconds) * 1000;
    const update = () => {
        const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        button.disabled = remaining > 0;
        button.textContent = button.dataset.label + (remaining ? " (" + remaining + " s)" : "");
        if (!remaining) clearInterval(verificationTimer);
    };
    update();
    if (seconds > 0) verificationTimer = setInterval(update, 1000);
}
async function sendVerificationEmail() {
    const request = ++verificationRequest,
        email = verificationEmail;
    clearInterval(verificationTimer);
    $("btn-resend-code").disabled = true;
    $("btn-verify").disabled = true;
    setText("verify-delivery-status", "Enviando código…");
    setText("btn-resend-code", "Enviando…");
    try {
        const result = await api("/auth/reenviar-codigo", "POST");
        if (request !== verificationRequest || email !== verificationEmail) return;
        setText("verify-description", "Ingresa el código de seis dígitos enviado a:");
        setText(
            "verify-delivery-status",
            "Código enviado. Revisa tu bandeja de entrada y la carpeta de spam.",
        );
        $("verify-code").value = "";
        verificationDelivery(true, result.reenviar_en_segundos);
        $("verify-code").focus();
    } catch (error) {
        if (request !== verificationRequest || email !== verificationEmail) return;
        let status = { codigo_enviado: false, reenviar_en_segundos: 5 };
        try {
            status = await api("/auth/verificacion");
        } catch {}
        if (request !== verificationRequest || email !== verificationEmail) return;
        setText("verify-description", "Verifica el correo de tu cuenta:");
        setText(
            "verify-delivery-status",
            "No pudimos enviar el código. " +
                (status.codigo_enviado
                    ? "Puedes usar el último que recibiste o volver a intentar."
                    : "Vuelve a intentarlo en unos segundos."),
        );
        verificationDelivery(status.codigo_enviado, status.reenviar_en_segundos, true);
    }
}
async function signedIn(result) {
    if (result.requiere_verificacion) {
        const request = ++verificationRequest;
        clearInterval(poll);
        currentUser = null;
        csrf = result.csrf;
        verificationEmail = result.correo;
        show("app-container", false);
        show("auth-wrapper", true, "flex");
        authPage("verify-section");
        setText("verify-email-display", result.correo);
        setText("verify-description", "Verifica el correo de tu cuenta:");
        $("verify-code").value = "";
        $("login-password").value = "";
        $("reg-password").value = "";
        verificationDelivery(result.codigo_enviado, result.reenviar_en_segundos);
        if (result.codigo_enviado) {
            setText("verify-delivery-status", "Código enviado. Revisa tu correo.");
            $("verify-code").focus();
        } else if (Number(result.reenviar_en_segundos) > 0) {
            setText(
                "verify-delivery-status",
                "Puedes volver a intentar el envío cuando termine la espera.",
            );
            verificationDelivery(false, result.reenviar_en_segundos, true);
        } else {
            setText("verify-delivery-status", "Enviando código…");
            $("btn-resend-code").disabled = true;
            // Dejar que el navegador dibuje la pantalla antes de solicitar el envío.
            requestAnimationFrame(() =>
                requestAnimationFrame(() => {
                    if (request === verificationRequest) sendVerificationEmail();
                }),
            );
        }
        return;
    }
    ++verificationRequest;
    clearInterval(verificationTimer);
    currentUser = result.estudiante;
    csrf = result.csrf;
    profile();
    $("login-password").value = "";
    $("reg-password").value = "";
    show("auth-wrapper", false);
    show("app-container", true, "flex");
    navigate("inicio-section", false);
    await Promise.all([refresh(), loadSearch().catch((e) => notify(e.message, true))]);
    clearInterval(poll);
    poll = setInterval(() => {
        if (!currentUser || document.hidden) return;
        if (document.querySelector("#chat-section.active") && selectedConversation)
            loadMessages().catch((e) => notify(e.message, true));
        loadNotifications().catch(() => {});
    }, 8000);
}
async function refresh() {
    const results = await Promise.allSettled([
        loadSubjects(),
        loadMySubjects(),
        loadSchedule(),
        loadSessions(),
        loadBadges(),
        loadHours(),
        loadNotifications(),
        loadSubscriptions(),
        loadConversations(),
        loadCycle(),
    ]);
    for (const result of results)
        if (result.status === "rejected") notify(result.reason.message, true);
}
async function loadCycle() {
    const rows = await api("/ciclo");
    document
        .querySelectorAll("[data-campus]")
        .forEach(
            (e) => (e.textContent = "Campus virtual" + (rows.length ? " · " + rows[0].nombre : "")),
        );
}
const subjectSelectors = ["teach-subject", "learn-subject", "select-filtro-curso-reporte"];
const subjectRequests = {},
    subjectOptions = {};
function subjectValue(prefix) {
    const input = $(prefix + "-subject");
    const row = (subjectOptions[prefix + "-subject"] || []).find(
        (row) => row.nombre.toLocaleLowerCase() === input.value.trim().toLocaleLowerCase(),
    );
    if (!row) throw new Error("Selecciona una materia registrada de la lista.");
    return row.id;
}
async function loadSubjectPage(id) {
    const request = (subjectRequests[id] || 0) + 1;
    subjectRequests[id] = request;
    const input = $(id),
        isInput = input.tagName === "INPUT";
    const query = isInput ? input.value : $(id + "-search")?.value || "";
    const selected = input.value;
    const result = await api("/materias?q=" + encodeURIComponent(query) + "&pagina=1");
    for (let page = 2; result.items.length < result.total; page++) {
        const next = await api("/materias?q=" + encodeURIComponent(query) + "&pagina=" + page);
        if (!next.items.length) break;
        result.items.push(...next.items);
    }
    if (subjectRequests[id] !== request) return;
    subjectOptions[id] = result.items;
    if (isInput) {
        $(id + "-list").replaceChildren(
            ...result.items.map((row) => new Option(row.nombre, row.nombre)),
        );
        input.placeholder = result.items.length
            ? "Escribe o selecciona una materia..."
            : "No se encontraron materias";
    } else {
        input.replaceChildren(
            new Option("Todos los cursos", ""),
            ...result.items.map((row) => new Option(row.nombre, row.id)),
        );
        if (result.items.some((row) => String(row.id) === selected)) input.value = selected;
        input.disabled = false;
        if (id === "select-filtro-curso-reporte") reports();
    }
}
async function loadSubjects() {
    await Promise.all(subjectSelectors.map((id) => loadSubjectPage(id)));
}
function setupSubjectSearch() {
    for (const id of subjectSelectors) {
        const input = $(id).tagName === "INPUT" ? $(id) : $(id + "-search");
        if (!input) continue;
        let timer;
        input.addEventListener("input", () => {
            subjectRequests[id] = (subjectRequests[id] || 0) + 1;
            clearTimeout(timer);
            timer = setTimeout(
                () => loadSubjectPage(id).catch((error) => notify(error.message, true)),
                250,
            );
        });
    }
}

async function loadMySubjects() {
    mySubjects = await api("/perfil/materias");
    for (const [role, id] of [
        ["ENSENAR", "teach-tags"],
        ["APRENDER", "learn-tags"],
    ]) {
        const rows = mySubjects.filter((s) => s.tipo_rol === role);
        $(id).innerHTML = rows.length
            ? rows
                  .map(
                      (s) =>
                          `<div class="tag ${role === "ENSENAR" ? "tag-purple" : "tag-green"}">${escapeHtml(s.nombre)} <span class="level">${escapeHtml(s.nivel.toLowerCase())}</span><button type="button" class="close" data-remove-subject="${s.id}" aria-label="Quitar materia" style="border:0;background:transparent;cursor:pointer;"><i class="fa-solid fa-xmark"></i></button></div>`,
                  )
                  .join("")
            : empty("Aún no has agregado materias.");
    }
    setText("count-teach", mySubjects.filter((s) => s.tipo_rol === "ENSENAR").length);
    setText("count-learn", mySubjects.filter((s) => s.tipo_rol === "APRENDER").length);
}
function slotKey(day, hour) {
    return `${day}:${hour}`;
}
function drawSchedule() {
    const grid = $("schedule-grid");
    grid.innerHTML =
        '<div class="grid-header"></div>' +
        ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]
            .map((d) => `<div class="grid-header">${d}</div>`)
            .join("");
    for (let hour = 6; hour < 23; hour++) {
        grid.insertAdjacentHTML(
            "beforeend",
            `<div class="grid-time">${String(hour).padStart(2, "0")}:00</div>`,
        );
        for (let day = 1; day <= 7; day++)
            grid.insertAdjacentHTML(
                "beforeend",
                `<button type="button" class="grid-cell ${selectedSlots.has(slotKey(day, hour)) ? "selected" : ""}" data-slot="${slotKey(day, hour)}" aria-label="Día ${day}, ${hour}:00" aria-pressed="${selectedSlots.has(slotKey(day, hour))}" style="border:1px solid var(--border-color);min-height:40px;cursor:pointer;${selectedSlots.has(slotKey(day, hour)) ? "background:var(--secondary-light);color:var(--secondary);" : ""}">${selectedSlots.has(slotKey(day, hour)) ? '<i class="fa-solid fa-check"></i>' : ""}</button>`,
            );
    }
}
function resetSchedule() {
    selectedSlots = new Set();
    for (const row of savedSchedule) {
        const start = Number(String(row.hora_inicio).slice(0, 2)),
            end = Number(String(row.hora_fin).slice(0, 2));
        for (let h = start; h < end; h++) selectedSlots.add(slotKey(row.dia_semana, h));
    }
    drawSchedule();
}
async function loadSchedule() {
    savedSchedule = await api("/perfil/horario");
    resetSchedule();
    setText("count-availability", `${savedSchedule.length} franjas disponibles`);
}
async function loadHours() {
    const h = await api("/perfil/horas");
    setText("perfil-hours-teach", Number(h.horas_ensenadas).toFixed(1) + "h");
    setText("perfil-hours-learn", Number(h.horas_aprendidas).toFixed(1) + "h");
    setText("rf029-ensenadas", Number(h.horas_ensenadas).toFixed(1) + "h");
    setText("rf029-aprendidas", Number(h.horas_aprendidas).toFixed(1) + "h");
    const values = [
        Number(h.horas_ensenadas).toFixed(1) + "h",
        h.sesiones_completadas,
        "—",
        new Set(
            sessions.map((s) =>
                s.proponente_id === currentUser.id ? s.receptor_id : s.proponente_id,
            ),
        ).size,
    ];
    document
        .querySelectorAll("[data-metric]")
        .forEach((e) => (e.textContent = values[Number(e.dataset.metric)]));
    const ratings = await api("/perfil/resumen");
    document.querySelector('[data-metric="2"]').textContent =
        ratings.calificacion_promedio === null
            ? "Sin calificaciones"
            : Number(ratings.calificacion_promedio).toFixed(1);
    document.querySelector('[data-metric="3"]').textContent = ratings.companeros;
}
async function loadSessions() {
    sessions = await api("/sesiones");
    renderSessions();
    const next = sessions
        .filter((s) => ["CONFIRMADA", "PROPUESTA", "EN_CURSO"].includes(s.estado))
        .sort((a, b) => new Date(a.fecha_hora) - new Date(b.fecha_hora))[0];
    setText("dashboard-next-subject", next ? next.materia : "Sin próximas sesiones");
    setText(
        "dashboard-next-detail",
        next
            ? `${dateText(next.fecha_hora)} · con ${next.companero}`
            : "Propón una sesión para empezar.",
    );
    const weekStart = new Date();
    weekStart.setDate(weekStart.getDate() - ((weekStart.getDay() + 6) % 7));
    weekStart.setHours(0, 0, 0, 0);
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekEnd.getDate() + 7);
    const weekly = sessions.filter(
        (s) =>
            new Date(s.fecha_hora) >= weekStart &&
            new Date(s.fecha_hora) < weekEnd &&
            s.estado !== "CANCELADA",
    );
    const count = weekly.filter((s) => s.estado === "COMPLETADA").length;
    setText(
        "dashboard-week-count",
        weekly.length ? `${count} de ${weekly.length} completadas` : "Sin sesiones esta semana",
    );
    $("dashboard-week-progress").style.width =
        (weekly.length ? (count / weekly.length) * 100 : 0) + "%";
    const recent = $("dashboard-recent-sessions");
    recent.querySelectorAll(".session-item,.live-empty").forEach((e) => e.remove());
    recent.insertAdjacentHTML(
        "beforeend",
        sessions.length
            ? sessions
                  .slice(0, 3)
                  .map(
                      (s) =>
                          `<div class="session-item"><div class="session-avatar" style="background:var(--primary);">${escapeHtml(initials(s.companero))}</div><div class="session-info"><h4>${escapeHtml(s.companero)}</h4><p>${escapeHtml(s.materia)} · ${escapeHtml(dateText(s.fecha_hora))}</p></div><div class="session-status"><div class="badge badge-success">${escapeHtml(s.estado)}</div></div></div>`,
                  )
                  .join("")
            : '<div class="live-empty">' + empty("Todavía no tienes sesiones.") + "</div>",
    );
}
function sessionCard(s) {
    const button = (action, label) =>
        `<button class="btn btn-primary" data-session="${s.id}" data-session-action="${action}">${label}</button>`;
    let actions = "";
    if (s.estado === "PROPUESTA" && s.receptor_id === currentUser.id)
        actions += button("aceptar", "Aceptar");
    if (["PROPUESTA", "CONFIRMADA"].includes(s.estado))
        actions += button("cancelar", "Cancelar") + button("enlace", "Añadir enlace");
    if (
        ["CONFIRMADA", "EN_CURSO"].includes(s.estado) &&
        s.enlace_videollamada?.startsWith("https://")
    )
        actions += button("iniciar", "Iniciar reunión");
    if (s.estado === "EN_CURSO")
        actions += button(
            "finalizar",
            (s.proponente_id === currentUser.id ? s.marca_proponente : s.marca_receptor)
                ? "Esperando a tu compañero"
                : "Confirmar finalización",
        );
    if (s.estado === "COMPLETADA")
        actions += button("calificar", s.estrellas ? "Actualizar calificación" : "Calificar");
    const card = $("session-card-template").content.firstElementChild.cloneNode(true);
    const fill = (name, value) => {
        card.querySelector(`[data-field="${name}"]`).textContent = value;
    };
    fill("materia", s.materia);
    fill("estado", s.estado);
    fill("companero", s.companero);
    fill(
        "fecha",
        new Date(s.fecha_hora).toLocaleDateString("es-PE", {
            timeZone: "America/Bogota",
            dateStyle: "full",
        }),
    );
    fill(
        "hora",
        new Date(s.fecha_hora).toLocaleTimeString("es-PE", {
            timeZone: "America/Bogota",
            timeStyle: "short",
        }),
    );
    fill(
        "duracion",
        (s.duracion_real_min === null
            ? `${s.duracion_programada_min} minutos programados`
            : `${s.duracion_real_min} minutos registrados`) +
            (s.estrellas ? ` · Tu calificación: ${s.estrellas}/5` : ""),
    );
    card.querySelector('[data-field="acciones"]').innerHTML = actions;
    return card.outerHTML;
}
function renderSessions() {
    const rows = sessions.filter((s) =>
        pastSessions
            ? ["COMPLETADA", "CANCELADA"].includes(s.estado)
            : !["COMPLETADA", "CANCELADA"].includes(s.estado),
    );
    $("historial-container").innerHTML = rows.length
        ? rows.map(sessionCard).join("")
        : empty("No tienes sesiones en esta sección.");
}
async function loadBadges() {
    badges = await api("/insignias");
    const earned = badges.filter((b) => b.obtenida_en);
    $("insignias-container").replaceChildren();
    if (!badges.length)
        $("insignias-container").innerHTML = empty("Todavía no hay insignias disponibles.");
    for (const b of badges) {
        const card = $("badge-card-template").content.firstElementChild.cloneNode(true);
        for (const [key, value] of [
            ["nombre", b.nombre],
            ["descripcion", b.descripcion || ""],
            ["horas", `${Number(b.horas_actuales).toFixed(1)} / ${b.meta_horas} hrs`],
            ["estado", b.obtenida_en ? "Obtenida el " + dateText(b.obtenida_en) : "Por obtener"],
        ])
            card.querySelector(`[data-field="${key}"]`).textContent = value;
        card.querySelector('[data-field="progreso"]').style.width =
            Math.min(100, (Number(b.horas_actuales) / Number(b.meta_horas)) * 100) + "%";
        if (!b.obtenida_en) {
            card.style.background = "white";
            card.style.border = "1px solid var(--border-color)";
            card.querySelector('[data-field="icono-fondo"]').style.background = "#94a3b8";
        }
        $("insignias-container").append(card);
    }
    $("dashboard-badges").innerHTML = earned.length
        ? earned
              .slice(0, 5)
              .map(
                  (b) =>
                      `<div class="badge-item"><div class="badge-icon" style="color:#e3a008;"><i class="fa-solid fa-award"></i></div><div class="badge-name">${escapeHtml(b.nombre)}</div></div>`,
              )
              .join("")
        : empty("Aún no has obtenido insignias.");
    setText("sidebar-badge-count", earned.length);
    $("dashboard-badge-progress").querySelector(".progress-text").textContent =
        `${earned.length} de ${badges.length} insignias obtenidas`;
    $("dashboard-badge-progress").querySelector(".progress-bar-fill").style.width =
        (badges.length ? (earned.length / badges.length) * 100 : 0) + "%";
}
// La presentación y búsqueda de compañeros se enlazan en design.js.
let loadSearch, studentCard;
async function otherProfile(id) {
    const [result, mine] = await Promise.all([api("/estudiantes/" + id), api("/perfil/materias")]);
    selectedStudent = result.perfil;
    setText("modal-comp-nombre", selectedStudent.nombre);
    setText(
        "modal-comp-carrera",
        [selectedStudent.carrera, selectedStudent.ciclo].filter(Boolean).join(" · "),
    );
    setText("modal-comp-avatar", initials(selectedStudent.nombre));
    setText(
        "modal-comp-bio",
        selectedStudent.bio || "Este estudiante todavía no agregó una presentación.",
    );
    show("peer-email-verified", Boolean(selectedStudent.correo_verificado), "inline-block");
    for (const [role, target] of [
        ["ENSENAR", "modal-comp-teach-list"],
        ["APRENDER", "modal-comp-learn-list"],
    ]) {
        const subjects = result.materias.filter((m) => m.tipo_rol === role);
        $(target).innerHTML = subjects.length
            ? subjects
                  .map(
                      (m) =>
                          `<span class="tag ${role === "ENSENAR" ? "tag-purple" : "tag-green"}">${escapeHtml(m.nombre)} <span class="level">${escapeHtml(m.nivel.toLowerCase())}</span></span>`,
                  )
                  .join("")
            : empty("Sin materias registradas.");
    }
    const complementary = mine.filter((m) =>
        result.materias.some(
            (peer) => peer.materia_id === m.materia_id && peer.tipo_rol !== m.tipo_rol,
        ),
    ).length;
    setText(
        "modal-comp-match-val",
        mine.length ? Math.round((complementary * 100) / mine.length) + "%" : "—",
    );
    const self = Number(id) === Number(currentUser.id);
    show("modal-comp-match", !self, "inline-flex");
    show("modal-comp-actions-other", !self, "grid");
    show("modal-comp-actions-self", self, "flex");
    show("modal-comp-report-footer", !self);
    show("modal-comp-preview-badge", self, "inline-flex");
    setText(
        "modal-comp-disponibilidad",
        result.horario.length
            ? "Disponibilidad: " +
                  result.horario
                      .map(
                          (h) =>
                              `${["", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"][h.dia_semana]} ${h.hora_inicio.slice(0, 5)}–${h.hora_fin.slice(0, 5)}`,
                      )
                      .join(" · ")
            : "Sin disponibilidad registrada.",
    );
    show("modal-companero-perfil", true, "flex");
}
window.cerrarModalCompanero = () => show("modal-companero-perfil", false);
let sessionMatches = [],
    sessionAvailability = [],
    sessionBusy = [];
function fillSessionMentor() {
    const selected = $("sesion-materia").selectedOptions[0];
    const rows = sessionMatches.filter(
        (m) =>
            String(m.id) === $("sesion-materia").value &&
            (!selected?.dataset.mentorId || String(m.mentor_id) === selected.dataset.mentorId),
    );
    const mentors = [...new Set(rows.map((m) => String(m.mentor_id)))];
    $("sesion-mentor").replaceChildren(
        ...mentors.map(
            (id) =>
                new Option(
                    id === String(currentUser.id)
                        ? "Yo enseñaré"
                        : selectedStudent.nombre + " enseñará",
                    id,
                ),
        ),
    );
    $("sesion-mentor").disabled = mentors.length <= 1;
    $("btn-enviar-propuesta-sesion").disabled = !rows.length;
}
function fillSessionSubjects() {
    const selected = $("sesion-materia").value,
        query = ($("sesion-materia-search")?.value || "").trim().toLocaleLowerCase();
    const rows = sessionMatches.filter((m) => m.nombre.toLocaleLowerCase().includes(query));
    const unique = [...new Map(rows.map((m) => [m.id + ":" + m.mentor_id, m])).values()];
    $("sesion-materia").replaceChildren(
        ...(unique.length
            ? unique.map((m) => {
                  const option = new Option(
                      m.nombre +
                          " · " +
                          (Number(m.mentor_id) === Number(currentUser.id)
                              ? "Tú enseñas"
                              : selectedStudent.nombre + " enseña"),
                      m.id,
                  );
                  option.dataset.mentorId = String(m.mentor_id);
                  return option;
              })
            : [new Option("No hay materias coincidentes", "")]),
    );
    if (unique.some((m) => String(m.id) === selected)) $("sesion-materia").value = selected;
    $("sesion-materia").disabled = !unique.length;
    setText(
        "sesion-materia-status",
        unique.length
            ? "Solo materias donde uno enseña y el otro aprende."
            : "Registren materias complementarias en sus perfiles.",
    );
    fillSessionMentor();
}
function commonAvailability() {
    const date = $("sesion-fecha").value;
    if (!date) {
        setText("sesion-disponibilidad", "Selecciona una fecha para ver la disponibilidad común.");
        return [];
    }
    const day = new Date(date + "T12:00:00").getDay() || 7;
    const minutes = (value) => Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5));
    const merge = (rows) => {
        const merged = [];
        for (const row of rows
            .filter((h) => Number(h.dia_semana) === day)
            .map((h) => [minutes(h.hora_inicio), minutes(h.hora_fin)])
            .sort((a, b) => a[0] - b[0])) {
            const last = merged.at(-1);
            if (last && row[0] <= last[1]) last[1] = Math.max(last[1], row[1]);
            else merged.push(row);
        }
        return merged;
    };
    const common = [];
    for (const a of merge(sessionAvailability[0]))
        for (const b of merge(sessionAvailability[1])) {
            const start = Math.max(a[0], b[0]),
                end = Math.min(a[1], b[1]);
            if (start < end) common.push([start, end]);
        }
    const time = (n) =>
        String(Math.floor(n / 60)).padStart(2, "0") + ":" + String(n % 60).padStart(2, "0");
    setText(
        "sesion-disponibilidad",
        common.length
            ? "Disponibilidad común: " +
                  common.map((r) => time(r[0]) + "–" + time(r[1])).join(" · ")
            : "No tienen disponibilidad en común este día.",
    );
    const duration = Number($("sesion-duracion").value) * 60;
    const allowed = common.filter((r) => r[1] - r[0] >= duration);
    $("sesion-hora").disabled = !allowed.length;
    if (allowed.length) {
        $("sesion-hora").min = time(allowed[0][0]);
        $("sesion-hora").max = time(allowed.at(-1)[1] - duration);
    }
    return common;
}
function validateSessionTime() {
    const ranges = commonAvailability(),
        date = $("sesion-fecha").value,
        time = $("sesion-hora").value;
    if (!date || !time) throw new Error("Selecciona la fecha y hora de la sesión.");
    const start = Number(time.slice(0, 2)) * 60 + Number(time.slice(3, 5)),
        duration = Number($("sesion-duracion").value) * 60;
    if (!ranges.some((r) => start >= r[0] && start + duration <= r[1]))
        throw new Error("La sesión completa debe estar dentro de la disponibilidad de ambos.");
    const begin = new Date(date + "T" + time),
        end = new Date(begin.getTime() + duration * 60000);
    if (begin <= new Date()) throw new Error("Selecciona una fecha futura.");
    if (
        sessionBusy.some((s) => {
            const a = new Date(s.fecha_hora),
                b = new Date(a.getTime() + Number(s.duracion_programada_min) * 60000);
            return a < end && b > begin;
        })
    )
        throw new Error("Ya tienes otra sesión en ese horario.");
}
window.proponerSesionDesdeCompanero = async () => {
    if (!selectedStudent) return;
    try {
        const [matches, mine, other, busy] = await Promise.all([
            api("/estudiantes/" + selectedStudent.id + "/coincidencias"),
            api("/perfil/horario"),
            api("/estudiantes/" + selectedStudent.id),
            api("/sesiones"),
        ]);
        sessionMatches = matches;
        sessionAvailability = [mine, other.horario];
        sessionBusy = busy.filter((s) =>
            ["PROPUESTA", "CONFIRMADA", "EN_CURSO"].includes(s.estado),
        );
        if ($("sesion-materia-search")) $("sesion-materia-search").value = "";
        $("sesion-fecha").value = "";
        $("sesion-hora").value = "";
        $("sesion-hora").disabled = true;
        const today = new Date();
        $("sesion-fecha").min = [
            today.getFullYear(),
            String(today.getMonth() + 1).padStart(2, "0"),
            String(today.getDate()).padStart(2, "0"),
        ].join("-");
        fillSessionSubjects();
        commonAvailability();
        show("modal-companero-perfil", false);
        setText("session-recipient-label", "Proponer una sesión a " + selectedStudent.nombre);
        show("modal-sesion", true, "flex");
    } catch (error) {
        notify(error.message, true);
    }
};
window.irAlChatDesdeCompanero = async () => {
    try {
        if (!selectedStudent) return;
        const row = await api("/conversaciones", "POST", { estudiante_id: selectedStudent.id });
        show("modal-companero-perfil", false);
        await loadConversations();
        await openConversation(row.id);
        navigate("chat-section");
    } catch (e) {
        notify(e.message, true);
    }
};
async function loadNotifications() {
    const rows = await api("/notificaciones");
    $("notifications-live").innerHTML = rows.length
        ? rows
              .map(
                  (n) =>
                      `<div class="section-card" style="margin:0;${n.leida ? "" : "border-left:4px solid var(--primary);"}"><h3 style="font-size:15px;">${escapeHtml(n.titulo)}</h3><p style="font-size:13px;margin:8px 0;color:var(--text-muted);">${escapeHtml(n.mensaje)}</p><small>${escapeHtml(dateText(n.creada_en))}</small><div style="display:flex;gap:10px;margin-top:12px;">${!n.leida ? `<button class="btn btn-secondary" data-read="${n.id}">Marcar como leída</button>` : ""}${n.tipo_evento === "PROPUESTA_SESION" ? '<button class="btn btn-primary" data-go-sessions>Ver sesiones</button>' : ""}</div></div>`,
              )
              .join("")
        : empty("No tienes notificaciones.");
    const unread = rows.some((n) => !n.leida);
    show("rf38-badge-sidebar-notif", unread, "inline-flex");
    show("rf38-punto-rojo-campana", unread);
}
async function loadConversations() {
    conversations = await api("/conversaciones");
    renderConversations();
}
function renderConversations() {
    const q = $("chat-search-input").value.toLowerCase(),
        rows = conversations.filter((c) => c.nombre.toLowerCase().includes(q));
    $("chat-conversation-list").innerHTML = rows.length
        ? rows
              .map(
                  (c) =>
                      `<button class="chat-conversation-item ${c.id === selectedConversation?.id ? "active" : ""}" data-conversation="${c.id}" style="display:flex;gap:12px;text-align:left;padding:16px;border:0;border-radius:10px;background:${c.id === selectedConversation?.id ? "var(--primary-light)" : "white"};cursor:pointer;width:100%;"><div class="user-avatar-small" style="background:var(--primary);">${escapeHtml(initials(c.nombre))}</div><div><strong>${escapeHtml(c.nombre)}</strong><p style="font-size:12px;color:var(--text-muted);margin-top:5px;">Conversación${Number(c.no_leidos) ? " · " + c.no_leidos + " sin leer" : ""}</p></div></button>`,
              )
              .join("")
        : empty("No tienes conversaciones. Busca un compañero para empezar.");
    if (!selectedConversation) {
        setText("chat-active-name", "Selecciona una conversación");
        setText("chat-active-avatar", "");
        setText("chat-active-badge", "");
        $("chat-messages-container").innerHTML = empty("Aquí aparecerán tus mensajes.");
        $("chat-input-mensaje").disabled = true;
    }
}
async function openConversation(id) {
    selectedConversation = conversations.find((c) => c.id === Number(id));
    if (!selectedConversation) return;
    setText("chat-active-name", selectedConversation.nombre);
    setText("chat-active-avatar", initials(selectedConversation.nombre));
    setText("chat-active-badge", "Conversación");
    show("chat-banner-cerrado", selectedConversation.estado !== "ACTIVA", "flex");
    $("chat-input-mensaje").disabled = selectedConversation.estado !== "ACTIVA";
    renderConversations();
    await loadMessages();
}
async function loadMessages() {
    if (!selectedConversation) return;
    const id = selectedConversation.id,
        rows = await api(`/conversaciones/${id}/mensajes`);
    if (selectedConversation?.id !== id) return;
    $("chat-messages-container").innerHTML = rows.length
        ? rows.map(messageBubble).join("")
        : empty("Todavía no hay mensajes en esta conversación.");
}
function messageBubble(m) {
    const mine = m.emisor_id === currentUser.id;
    let content = String(m.texto)
        .split(/(https?:\/\/[^\s]+)/g)
        .map((part) =>
            /^https?:\/\//.test(part)
                ? `<a href="${escapeHtml(part)}" target="_blank" rel="noopener noreferrer" style="color:inherit;text-decoration:underline;font-weight:600;">${escapeHtml(part)}</a>`
                : escapeHtml(part),
        )
        .join("");
    if (m.archivo) {
        const file = m.archivo;
        content = `<a href="${escapeHtml(file.url)}" target="_blank" rel="noopener noreferrer" style="color:inherit;">${file.tipo.startsWith("image/") ? `<img src="${escapeHtml(file.url)}" alt="${escapeHtml(file.nombre)}" style="max-width:260px;max-height:260px;object-fit:contain;border-radius:10px;display:block;margin-bottom:8px;">` : '<i class="fa-solid fa-file-arrow-down" style="margin-right:8px;"></i>'}${escapeHtml(file.nombre)}</a>`;
    }
    return `<div style="display:flex;justify-content:${mine ? "flex-end" : "flex-start"};align-items:flex-end;gap:8px;margin-bottom:14px;width:100%;">${mine ? "" : `<div data-chat-peer="true" role="button" tabindex="0" title="Ver perfil · Clic derecho para proponer sesión" style="cursor:pointer;width:32px;height:32px;border-radius:50%;background:var(--primary);color:white;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;flex-shrink:0;margin-bottom:18px;">${escapeHtml(initials(m.autor))}</div>`}<div style="max-width:72%;display:flex;flex-direction:column;align-items:${mine ? "flex-end" : "flex-start"};"><div style="background:${mine ? "linear-gradient(135deg,#b30832 0%,#910729 100%)" : "#f8fafc"};border:${mine ? "none" : "1px solid #e2e8f0"};color:${mine ? "white" : "#1e293b"};padding:11px 16px;border-radius:${mine ? "16px 16px 3px 16px" : "16px 16px 16px 3px"};font-size:13.5px;line-height:1.45;box-shadow:0 2px 8px #0001;white-space:pre-wrap;word-break:break-word;">${content}</div><div style="display:flex;align-items:center;margin-top:4px;font-size:11px;color:#94a3b8;"><span>${escapeHtml(dateText(m.enviado_en))}</span>${mine ? `<i class="fa-solid ${m.leido ? "fa-check-double" : "fa-check"}" title="${m.leido ? "Leído" : "Enviado"}" style="color:${m.leido ? "#0891b2" : "#94a3b8"};font-size:11px;margin-left:6px;"></i>` : ""}</div></div></div>`;
}
function chatPeerId() {
    if (!selectedConversation) throw new Error("Selecciona una conversación.");
    return Number(selectedConversation.estudiante_a) === Number(currentUser.id)
        ? selectedConversation.estudiante_b
        : selectedConversation.estudiante_a;
}
async function proposeFromChat() {
    $("chat-peer-menu").hidePopover();
    const peer = await api("/estudiantes/" + chatPeerId());
    selectedStudent = peer.perfil;
    await window.proponerSesionDesdeCompanero();
}
function openChatMenu(event) {
    if (!selectedConversation) return;
    event?.preventDefault?.();
    const anchor = $("chat-peer-profile").getBoundingClientRect(),
        menu = $("chat-peer-menu");
    menu.style.left =
        Math.max(8, Math.min(event?.clientX || anchor.left, window.innerWidth - 220)) + "px";
    menu.style.top =
        Math.max(8, Math.min(event?.clientY || anchor.bottom, window.innerHeight - 110)) + "px";
    menu.showPopover();
}
async function sendMessage() {
    if (!selectedConversation) throw new Error("Selecciona una conversación.");
    await api(`/conversaciones/${selectedConversation.id}/mensajes`, "POST", {
        texto: $("chat-input-mensaje").value,
    });
    $("chat-input-mensaje").value = "";
    await loadMessages();
    $("chat-messages-container").scrollTop = $("chat-messages-container").scrollHeight;
}
async function loadRanking() {
    const rows = await api("/ranking");
    show("ranking-empty-state", !rows.length);
    show("ranking-table-container", !!rows.length);
    show("ranking-podium-container", !!rows.length, "grid");
    $("ranking-table-body").innerHTML = rows
        .map(
            (r, i) =>
                `<tr style="border-bottom:1px solid var(--border-color);"><td style="padding:16px;">${i + 1}</td><td style="padding:16px;"><strong>${escapeHtml(r.nombre)}</strong><p style="font-size:11px;color:var(--text-muted);">${escapeHtml(r.carrera)}</p></td><td>${escapeHtml(r.insignia || "Sin insignias")}</td><td>${r.sesiones_completadas}</td><td>${Number(r.horas_ensenadas).toFixed(1)}h</td></tr>`,
        )
        .join("");
    $("ranking-podium-container").replaceChildren();
    for (const rank of [2, 1, 3]) {
        const r = rows[rank - 1];
        if (!r) {
            $("ranking-podium-container").append(document.createElement("div"));
            continue;
        }
        const card = $("podium-card-" + rank).content.firstElementChild.cloneNode(true);
        for (const [field, value] of [
            ["nombre", r.nombre],
            ["carrera", r.carrera],
            ["horasEnsenadas", Number(r.horas_ensenadas).toFixed(1) + " hrs"],
            ["sesionesCompletadas", r.sesiones_completadas + " sesiones"],
        ])
            card.querySelector(`[data-field="${field}"]`).textContent = value;
        $("ranking-podium-container").append(card);
    }
    const index = rows.findIndex((r) => r.id === currentUser.id);
    show("ranking-propio-card", index >= 0, "flex");
    if (index >= 0) {
        setText("mi-ranking-avatar", initials(currentUser.nombre));
        setText("mi-ranking-titulo", `Puesto #${index + 1} · ${currentUser.nombre}`);
        setText("mi-ranking-horas", Number(rows[index].horas_ensenadas).toFixed(1) + "h");
        setText("mi-ranking-badge", "Tu posición actual");
    }
}
async function loadSubscriptions() {
    [subscriptions, plans] = await Promise.all([api("/suscripciones"), api("/planes")]);
    const active = subscriptions.find((s) => ["ACTIVO", "TRIAL"].includes(s.estado_actual));
    const latest = active || subscriptions[0];
    const name = latest?.plan || "Plan gratuito",
        state = latest?.estado_actual || "Sin suscripción";
    for (const id of ["mi-plan-card-titulo", "rf37-titulo-plan"]) setText(id, name);
    for (const id of ["rf34-top-text", "mi-plan-card-badge", "rf37-badge-activa"])
        setText(id, state);
    setText("mi-plan-fecha-vence", latest?.fecha_vencimiento || "Sin vencimiento");
    setText("rf37-fecha-vigencia", latest?.fecha_vencimiento || "Sin vencimiento");
    setText(
        "mi-plan-dias-subtext",
        active
            ? "Consulta la fecha de vencimiento de tu plan."
            : "Puedes consultar los planes disponibles.",
    );
    const activePlan = plans.find((p) => p.id === active?.plan_id);
    document
        .querySelectorAll("[data-plan-benefits]")
        .forEach(
            (e) =>
                (e.innerHTML = activePlan
                    ? activePlan.beneficios
                          .map((b) => `<li style="margin:8px 0;">${escapeHtml(b.texto)}</li>`)
                          .join("")
                    : "<li>Sin beneficios adicionales registrados.</li>"),
        );
    show("sidebar-pro-badge", !!active, "inline-block");
    setText("sidebar-pro-badge", state);
    const days = active?.fecha_vencimiento
        ? Math.ceil(
              (new Date(active.fecha_vencimiento + "T23:59:59-05:00") - new Date()) / 86400000,
          )
        : null;
    show("rf38-banner-amarillo-dashboard", days !== null && days >= 0 && days <= 7, "flex");
    if (days !== null) {
        setText("rf38-txt-dias-banner", days);
        setText("rf38-txt-fecha-banner", active.fecha_vencimiento);
    }
    const payments = await api("/pagos");
    setText("rf37-contador-transacciones", payments.length + " transacciones");
    $("rf37-tbody-historial").innerHTML = payments.length
        ? payments
              .map(
                  (p) =>
                      `<tr><td style="padding:14px;">${escapeHtml(dateText(p.pagado_en))}</td><td>${escapeHtml(p.modalidad)}</td><td>${escapeHtml(p.moneda)} ${escapeHtml(p.monto)}</td><td>${escapeHtml(p.marca_tarjeta || "—")}</td><td>${escapeHtml(p.numero_comprobante || "—")}</td><td>—</td><td>${escapeHtml(p.estado)}</td><td>${p.estado === "COMPLETADO" ? `<button class="btn" data-receipt="${p.id}">Ver registro</button>` : "—"}</td></tr>`,
              )
              .join("")
        : '<tr><td colspan="8" style="padding:28px;text-align:center;">No tienes pagos registrados.</td></tr>';
}
let selectedPlan = null;
function openPlan(id) {
    selectedPlan = plans.find((p) => p.id === Number(id));
    if (!selectedPlan) return;
    setText("rf35-modal-nombre-plan", selectedPlan.nombre);
    setText("rf35-modal-periodo", selectedPlan.periodo);
    setText(
        "rf35-modal-precio",
        `${selectedPlan.moneda} ${Number(selectedPlan.precio).toFixed(2)}`,
    );
    if ($("rf35-modal-lista-beneficios"))
        $("rf35-modal-lista-beneficios").innerHTML = selectedPlan.beneficios
            .map((b) => `<li>${escapeHtml(b.texto)}</li>`)
            .join("");
    show("modal-contratacion-plan", true, "flex");
}
function openDemoPayment() {
    if (!selectedPlan) throw new Error("Selecciona un plan.");
    show("modal-contratacion-plan", false);
    show("modal-pasarela-pago", true, "flex");
    show("rf36-banner-error", false);
    const price = `${selectedPlan.moneda} ${Number(selectedPlan.precio).toFixed(2)}`;
    setText("rf36-txt-monto-final", price);
    setText("rf36-btn-monto", price);
    setText("demo-payment-price", price);
}
async function paymentDemo(scenario = "APROBADO") {
    if (!selectedPlan) throw new Error("Selecciona un plan.");
    const result = await api("/pagos/prueba", "POST", {
        plan_id: selectedPlan.id,
        escenario: scenario,
    });
    if (result.estado === "APROBADO") {
        show("modal-pasarela-pago", false);
        setText(
            "rf36-exito-monto",
            `${result.plan.moneda} ${Number(result.plan.precio).toFixed(2)} (demostración)`,
        );
        show("modal-pago-exito", true, "flex");
    } else {
        setText(
            "rf36-banner-error-title",
            result.estado === "TIMEOUT" ? "Demora de prueba" : "Rechazo de prueba",
        );
        setText("rf36-banner-error-desc", result.mensaje);
        show("rf36-banner-error", true, "flex");
    }
    notify(result.mensaje, result.estado !== "APROBADO");
}
async function loadDocuments() {
    const rows = await api("/documentos");
    $("uploaded-files-list").innerHTML = rows.length
        ? rows
              .map(
                  (d) =>
                      `<div style="display:flex;align-items:center;justify-content:space-between;padding:16px;border:1px solid var(--border-color);border-radius:10px;"><div><i class="fa-solid fa-file-lines" style="color:var(--primary);margin-right:10px;"></i><a href="/api/documentos/${d.id}">${escapeHtml(d.nombre_archivo)}</a><p style="font-size:12px;color:var(--text-muted);margin-top:5px;">${escapeHtml(d.tamano_mb)} MB · ${escapeHtml(dateText(d.subido_en))}</p></div><button class="btn" data-remove-document="${d.id}" aria-label="Eliminar documento"><i class="fa-solid fa-trash"></i></button></div>`,
              )
              .join("")
        : empty("Aún no has subido documentos.");
}
function reports() {
    const subject = $("select-filtro-curso-reporte").value,
        rows = sessions.filter(
            (s) => s.estado === "COMPLETADA" && (!subject || s.materia_id === Number(subject)),
        );
    const peers = new Set(
        rows.map((s) => (s.proponente_id === currentUser.id ? s.receptor_id : s.proponente_id)),
    );
    setText("reporte-metric-estudiantes", peers.size + " compañeros");
    setText("reporte-metric-asesorias", rows.length + " sesiones");
    setText(
        "reporte-metric-horas",
        (rows.reduce((sum, s) => sum + Number(s.duracion_real_min || 0), 0) / 60).toFixed(1) +
            " horas",
    );
    setText("reporte-tabla-titulo", "Detalle de tus sesiones completadas");
    $("reporte-tabla-body").innerHTML = rows.length
        ? rows
              .map(
                  (s) =>
                      `<tr><td style="padding:14px;">${escapeHtml(dateText(s.fecha_hora))}</td><td>${escapeHtml(s.mentor_id === currentUser.id ? currentUser.nombre : s.companero)}</td><td>${escapeHtml(s.materia)}</td><td style="text-align:center;">2</td><td style="text-align:right;">${escapeHtml(s.duracion_real_min)} min</td></tr>`,
              )
              .join("")
        : '<tr><td colspan="5" style="padding:24px;text-align:center;">No hay sesiones completadas para este filtro.</td></tr>';
}
async function navigate(id, reload = true) {
    document
        .querySelectorAll(".page-section")
        .forEach((e) => e.classList.toggle("active", e.id === id));
    document
        .querySelectorAll(".nav-item")
        .forEach((e) => e.classList.toggle("active", e.dataset.target === id));
    if (!reload) return;
    const tasks = {
        "inicio-section": async () => {
            await Promise.all([loadSessions(), loadBadges(), loadSearch()]);
            await loadHours();
        },
        "perfil-section": async () => {
            await loadCatalogs();
            currentUser = await api("/auth/yo");
            profile();
            await Promise.all([
                loadSubjects(),
                loadMySubjects(),
                loadSchedule(),
                loadHours(),
                loadDocuments(),
            ]);
        },
        "buscar-section": async () => {
            await loadCatalogs();
            await loadSearch();
        },
        "mis-sesiones-section": loadSessions,
        "notificaciones-section": loadNotifications,
        "chat-section": loadConversations,
        "insignias-section": loadBadges,
        "ranking-section": loadRanking,
        "suscripcion-section": loadSubscriptions,
        "reportes-section": async () => {
            await loadSessions();
            reports();
        },
    };
    if (tasks[id])
        try {
            await tasks[id]();
        } catch (e) {
            notify(e.message, true);
        }
    window.designNavigated?.(id);
}
window.navegarASeccion = navigate;

document.addEventListener("DOMContentLoaded", async () => {
    setupSubjectSearch();
    setupEnterActions();
    setupAppDialog();
    for (const [id, page] of [
        ["link-to-register", "register-section"],
        ["link-to-login", "login-section"],
        ["link-to-recover", "recover-section"],
        ["link-recover-to-login", "login-section"],
        ["link-sent-to-login", "login-section"],
        ["link-verify-to-login", "login-section"],
    ])
        on(id, async () => {
            authPage(page);
            if (page === "register-section") await loadCatalogs();
        });
    on("btn-login", async () =>
        signedIn(
            await api("/auth/login", "POST", {
                correo: $("login-email").value,
                contrasena: $("login-password").value,
            }),
        ),
    );
    $("reg-email").addEventListener("input", () => $("reg-email").setCustomValidity(""));
    on("btn-register", async () => {
        const email = $("reg-email");
        email.value = email.value.trim().toLowerCase();
        email.setCustomValidity(
            /^[^\s@]+@utp\.edu\.pe$/.test(email.value)
                ? ""
                : "Usa tu correo institucional terminado en @utp.edu.pe.",
        );
        if (!email.reportValidity()) return;
        await signedIn(
            await api("/auth/registro", "POST", {
                nombre: $("reg-name").value,
                correo: email.value,
                contrasena: $("reg-password").value,
                carrera: $("reg-career").value,
                ciclo: $("reg-cycle").value,
            }),
        );
    });
    on("btn-verify", async () =>
        signedIn(await api("/auth/verificar", "POST", { codigo: $("verify-code").value.trim() })),
    );
    on("btn-resend-code", sendVerificationEmail);
    on("btn-logout", async () => {
        await api("/auth/logout", "POST");
        loggedOut();
        await token();
    });
    on("btn-recover", async () => {
        const result = await api("/auth/recuperar", "POST", { correo: $("recover-email").value });
        notify(result.mensaje);
        authPage("recover-sent-section");
    });
    on("btn-reset", async () => {
        const result = await api("/auth/restablecer", "POST", {
            token: new URLSearchParams(location.search).get("reset"),
            contrasena: $("reset-password").value,
        });
        history.replaceState(null, "", location.pathname);
        authPage("login-section");
        notify(result.mensaje);
    });
    document
        .querySelectorAll(".nav-item[data-target]")
        .forEach((e) => e.addEventListener("click", () => navigate(e.dataset.target)));
    document.querySelectorAll("#tab-informacion .sub-tabs-container .tab").forEach((e) =>
        e.addEventListener("click", () => {
            e.parentElement
                .querySelectorAll(".tab")
                .forEach((t) => t.classList.toggle("active", t === e));
            document
                .querySelectorAll("#tab-informacion > .tab-content")
                .forEach((t) => t.classList.toggle("active", t.id === e.dataset.target));
        }),
    );
    on("btn-save-profile", async () => {
        currentUser = await api("/perfil", "PUT", {
            nombre: $("perfil-nombre").value,
            carrera: $("perfil-carrera").value,
            ciclo: $("perfil-ciclo").value,
            bio: $("perfil-bio").value,
            push_activo: currentUser.push_activo,
        });
        profile();
        notify("Perfil guardado en tu cuenta.");
    });
    on("btn-cancel-profile", profile);
    on("btn-delete-account", async () => {
        if (
            await appDialog({
                title: "Desactivar cuenta",
                description: "Dejarás de aparecer en las búsquedas.",
                confirm: "Desactivar cuenta",
            })
        ) {
            await api("/cuenta", "DELETE");
            loggedOut();
            await token();
            notify("Cuenta desactivada.");
        }
    });
    for (const [prefix, role] of [
        ["teach", "ENSENAR"],
        ["learn", "APRENDER"],
    ])
        on("btn-add-" + prefix, async () => {
            await api("/perfil/materias", "POST", {
                materia_id: subjectValue(prefix),
                tipo_rol: role,
                nivel: $(prefix + "-level").value,
            });
            await loadMySubjects();
            notify("Materia guardada.");
        });
    on("btn-save-schedule", async () => {
        const slots = Array.from(selectedSlots).map((key) => {
            const [day, hour] = key.split(":").map(Number);
            return {
                dia_semana: day,
                hora_inicio: String(hour).padStart(2, "0") + ":00",
                hora_fin: String(hour + 1).padStart(2, "0") + ":00",
            };
        });
        await api("/perfil/horario", "PUT", slots);
        await loadSchedule();
        notify("Disponibilidad guardada.");
    });
    on("btn-cancel-schedule", resetSchedule);
    on(
        "file-upload",
        () => {
            const f = $("file-upload").files[0];
            if (!f) return;
            setText("confirm-file-name", f.name);
            setText("confirm-file-size", (f.size / 1024 / 1024).toFixed(2) + " MB");
            show("upload-drop-area", false);
            show("upload-confirm-area", true);
        },
        "change",
    );
    $("upload-drop-area").addEventListener("dragover", (e) => {
        e.preventDefault();
    });
    $("upload-drop-area").addEventListener("drop", (e) => {
        e.preventDefault();
        if (!e.dataTransfer.files.length) return;
        $("file-upload").files = e.dataTransfer.files;
        $("file-upload").dispatchEvent(new Event("change"));
    });
    on("btn-cancel-upload", () => {
        show("upload-confirm-area", false);
        show("upload-drop-area", true);
        $("file-upload").value = "";
    });
    on("btn-confirm-upload", async () => {
        const file = $("file-upload").files[0];
        if (!file) throw new Error("Selecciona un archivo.");
        const data = new FormData();
        data.append("archivo", file);
        await api("/documentos", "POST", data);
        show("upload-confirm-area", false);
        show("upload-drop-area", true);
        $("file-upload").value = "";
        await loadDocuments();
        notify("Documento guardado.");
    });
    on(
        "photo-upload",
        async () => {
            const file = $("photo-upload").files[0];
            if (!file) return;
            const data = new FormData();
            data.append("archivo", file);
            await api("/perfil/foto", "POST", data);
            currentUser = await api("/auth/yo");
            profile();
            notify("Foto guardada.");
        },
        "change",
    );
    on("btn-manual-search", loadSearch);
    let searchTimer;
    $("search-input").addEventListener("input", () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => loadSearch().catch((e) => notify(e.message, true)), 300);
    });
    on("btn-toggle-filters", () =>
        show("filter-panel", $("filter-panel").style.display === "none"),
    );
    on("btn-apply-filters", loadSearch);
    on("btn-clear-filters", async () => {
        $("filter-career").value = "Todos";
        $("quick-filter-course").value = "";
        $("quick-filter-day").value = "";
        $("search-input").value = "";
        await loadSearch();
    });
    on("btn-clear-search", async () => {
        $("search-input").value = "";
        await loadSearch();
    });
    $("sesion-materia-search")?.addEventListener("input", fillSessionSubjects);
    $("sesion-materia").addEventListener("change", fillSessionMentor);
    for (const id of ["sesion-fecha", "sesion-duracion"])
        $(id).addEventListener("change", commonAvailability);
    on("btn-enviar-propuesta-sesion", async () => {
        validateSessionTime();
        if (!selectedStudent) throw new Error("Selecciona un compañero.");
        await api("/sesiones", "POST", {
            receptor_id: selectedStudent.id,
            materia_id: $("sesion-materia").value,
            fecha_hora: $("sesion-fecha").value + "T" + $("sesion-hora").value,
            duracion_programada_min: Number($("sesion-duracion").value) * 60,
            mentor_id: $("sesion-mentor").value,
        });
        show("modal-sesion", false);
        await loadSessions();
        notify("Propuesta guardada y enviada al compañero.");
        navigate("mis-sesiones-section");
    });
    for (const [button, modal] of [
        ["btn-close-modal-sesion", "modal-sesion"],
        ["btn-close-modal-calificacion", "modal-calificacion"],
        ["btn-cancelar-calificacion", "modal-calificacion"],
        ["btn-close-modal-cancelacion", "modal-cancelacion"],
        ["btn-cancel-cancelacion", "modal-cancelacion"],
        ["btn-cancelar-oferta", "modal-contratacion-plan"],
        ["btn-cancelar-proceso-contratacion", "modal-contratacion-plan"],
    ])
        on(button, () => show(modal, false));
    on("btn-confirmar-cancelacion", async () => {
        await api(`/sesiones/${selectedCancellation}/cancelar`, "POST", {
            motivo: $("cancelacion-motivo").value,
        });
        show("modal-cancelacion", false);
        await loadSessions();
        notify("Sesión cancelada.");
    });
    on("tab-proximas", () => {
        pastSessions = false;
        renderSessions();
    });
    on("tab-pasadas", () => {
        pastSessions = true;
        renderSessions();
    });
    on("btn-rf027-refresh", loadSessions);
    on("btn-rf033-refresh", loadRanking);
    on("btn-back-notifs", () => navigate("notificaciones-section"));
    on("btn-back-notifs-sesion", () => navigate("notificaciones-section"));
    document.querySelectorAll(".star-item").forEach((e) =>
        e.addEventListener("click", () => {
            selectedStars = Number(e.dataset.value);
            document
                .querySelectorAll(".star-item")
                .forEach(
                    (s) =>
                        (s.style.color =
                            Number(s.dataset.value) <= selectedStars ? "#f59e0b" : "#cbd5e1"),
                );
            setText("star-label", selectedStars + " de 5 estrellas");
            show("seccion-comentario", true);
        }),
    );
    on("btn-confirmar-calificacion", async () => {
        if (!selectedStars) throw new Error("Selecciona una puntuación.");
        const result = await api(`/sesiones/${selectedRating}/calificacion`, "POST", {
            estrellas: selectedStars,
            texto: $("calif-comentario-input").value,
        });
        show("modal-calificacion", false);
        await loadSessions();
        notify(result.mensaje);
    });
    $("calif-comentario-input").addEventListener("input", () =>
        setText("calif-char-counter", $("calif-comentario-input").value.length + "/500"),
    );
    on("btn-chat-enviar", sendMessage);
    $("chat-search-input").addEventListener("input", renderConversations);
    on("btn-chat-action-more", openChatMenu);
    on("btn-chat-propose", proposeFromChat);
    on("chat-menu-propose", proposeFromChat);
    const viewChatPeer = async () => {
        $("chat-peer-menu").hidePopover();
        await otherProfile(chatPeerId());
    };
    on("chat-peer-profile", viewChatPeer);
    on("chat-menu-profile", viewChatPeer);
    $("chat-peer-profile").addEventListener("contextmenu", openChatMenu);
    $("chat-conversation-list").addEventListener("contextmenu", async (event) => {
        const peer = event.target.closest("[data-conversation]");
        if (!peer) return;
        event.preventDefault();
        const id = Number(peer.dataset.conversation),
            point = { clientX: event.clientX, clientY: event.clientY };
        try {
            await openConversation(id);
            if (selectedConversation?.id === id) openChatMenu(point);
        } catch (error) {
            notify(error.message, true);
        }
    });
    $("chat-peer-profile").addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            viewChatPeer().catch((e) => notify(e.message, true));
        }
    });
    $("chat-messages-container").addEventListener("contextmenu", (event) => {
        if (event.target.closest("[data-chat-peer]")) openChatMenu(event);
    });
    $("chat-messages-container").addEventListener("click", (event) => {
        if (event.target.closest("[data-chat-peer]"))
            viewChatPeer().catch((e) => notify(e.message, true));
    });
    $("chat-messages-container").addEventListener("keydown", (event) => {
        if (
            event.target.closest("[data-chat-peer]") &&
            (event.key === "Enter" || event.key === " ")
        ) {
            event.preventDefault();
            viewChatPeer().catch((e) => notify(e.message, true));
        }
    });

    for (const [button, input] of [
        ["btn-chat-attach", "chat-file-upload"],
        ["btn-chat-image", "chat-image-upload"],
    ]) {
        on(button, () => {
            if (!selectedConversation) throw new Error("Selecciona una conversación.");
            $(input).click();
        });
        on(
            input,
            async () => {
                const file = $(input).files[0];
                if (!file) return;
                if (!selectedConversation) throw new Error("Selecciona una conversación.");
                const data = new FormData();
                data.append("archivo", file);
                await api(`/conversaciones/${selectedConversation.id}/archivos`, "POST", data);
                $(input).value = "";
                await loadMessages();
                notify("Archivo enviado a la conversación.");
            },
            "change",
        );
    }
    for (const [id, target] of [
        ["tab-btn-mi-plan", "tab-content-mi-plan"],
        ["tab-btn-gestion-sub", "tab-content-gestion-sub"],
        ["tab-btn-comprobantes", "tab-content-comprobantes"],
    ])
        on(id, () => {
            document
                .querySelectorAll(".tab-plan-content")
                .forEach((e) => (e.style.display = e.id === target ? "block" : "none"));
            document
                .querySelectorAll(".tab-btn-plan")
                .forEach((e) => e.classList.toggle("active", e.id === id));
        });
    for (const id of ["btn-ir-mi-suscripcion", "btn-rf38-banner-renovar"])
        on(id, () => navigate("suscripcion-section"));
    on("btn-mi-plan-aprovechar", () => navigate("buscar-section"));
    on("btn-contratar-plan-oficial", () => {
        const paid = plans.find((p) => Number(p.precio) > 0);
        if (!paid) throw new Error("Todavía no hay planes disponibles.");
        openPlan(paid.id);
    });
    on("btn-rf37-cancelar-suscripcion", async () => {
        const active = subscriptions.find((s) => ["ACTIVO", "TRIAL"].includes(s.estado_actual));
        if (!active) throw new Error("No tienes una suscripción activa.");
        if (
            await appDialog({
                title: "Cancelar suscripción",
                description: "¿Quieres cancelar tu suscripción ahora?",
                confirm: "Cancelar suscripción",
            })
        ) {
            await api(`/suscripciones/${active.id}/cancelar`, "POST");
            await loadSubscriptions();
            notify("Suscripción cancelada.");
        }
    });
    on("btn-activar-prueba-real", async () => {
        if (!selectedPlan) throw new Error("Selecciona un plan.");
        await api("/suscripciones/prueba", "POST", { plan_id: selectedPlan.id });
        show("modal-contratacion-plan", false);
        await loadSubscriptions();
        notify("Prueba gratuita activada.");
    });
    on("btn-confirmar-plan-real", openDemoPayment);
    on("btn-confirmar-pago-pasarela", () => paymentDemo());
    on("btn-rf36-sim-exito", () => paymentDemo("APROBADO"));
    on("btn-rf36-sim-rechazo", () => paymentDemo("RECHAZADO"));
    on("btn-rf36-sim-timeout", () => paymentDemo("TIMEOUT"));
    on("btn-cerrar-pasarela-x", () => show("modal-pasarela-pago", false));
    on("btn-cancelar-pasarela-link", () => show("modal-pasarela-pago", false));
    on("btn-cerrar-modal-exito-inicio", () => {
        show("modal-pago-exito", false);
        navigate("inicio-section");
    });
    on("btn-cerrar-contratacion-x", () => show("modal-contratacion-plan", false));
    on("rf38-btn-campana-dashboard", () => navigate("notificaciones-section"));
    on("btn-rf38-banner-ver-popup", () => navigate("suscripcion-section"));
    on("btn-rf38-banner-cerrar", () => show("rf38-banner-amarillo-dashboard", false));
    on("select-filtro-curso-reporte", reports, "change");
    on("btn-descargar-reporte-pdf", () => window.print());
    document.addEventListener(
        "click",
        handle(async (event) => {
            const el = event.target.closest(
                "[data-remove-subject],[data-remove-document],[data-student],[data-conversation],[data-session-action],[data-read],[data-go-sessions],[data-slot],[data-plan],[data-receipt]",
            );
            if (!el) return;
            if (el.dataset.removeSubject) {
                await api("/perfil/materias/" + el.dataset.removeSubject, "DELETE");
                await loadMySubjects();
            } else if (el.dataset.removeDocument) {
                if (
                    await appDialog({
                        title: "Eliminar documento",
                        description: "¿Quieres eliminar este documento de tu perfil?",
                        confirm: "Eliminar documento",
                    })
                ) {
                    await api("/documentos/" + el.dataset.removeDocument, "DELETE");
                    await loadDocuments();
                }
            } else if (el.dataset.student) await otherProfile(el.dataset.student);
            else if (el.dataset.conversation) await openConversation(el.dataset.conversation);
            else if (el.dataset.read) {
                await api("/notificaciones/" + el.dataset.read + "/leer", "POST");
                await loadNotifications();
            } else if (el.hasAttribute("data-go-sessions")) await navigate("mis-sesiones-section");
            else if (el.dataset.slot) {
                selectedSlots.has(el.dataset.slot)
                    ? selectedSlots.delete(el.dataset.slot)
                    : selectedSlots.add(el.dataset.slot);
                drawSchedule();
            } else if (el.dataset.plan) openPlan(el.dataset.plan);
            else if (el.dataset.receipt) {
                const p = await api("/pagos/" + el.dataset.receipt + "/comprobante");
                notify(
                    `Registro ${p.numero_comprobante || p.id}: ${p.moneda} ${p.monto} · ${p.estado}`,
                );
            } else if (el.dataset.sessionAction) {
                const id = Number(el.dataset.session),
                    action = el.dataset.sessionAction,
                    row = sessions.find((s) => s.id === id);
                if (action === "calificar") {
                    selectedRating = id;
                    selectedStars = 0;
                    setText("calif-nombre-companero", row.companero);
                    $("calif-comentario-input").value = row.comentario || "";
                    show("seccion-comentario", true);
                    show("modal-calificacion", true, "flex");
                    document
                        .querySelectorAll(".star-item")
                        .forEach((s) => (s.style.color = "#cbd5e1"));
                    setText("star-label", "Selecciona una puntuación");
                    return;
                }
                if (action === "cancelar") {
                    selectedCancellation = id;
                    show("modal-cancelacion", true, "flex");
                    return;
                }
                const data = {};
                if (action === "enlace") {
                    const link = await appDialog({
                        title: "Añadir enlace",
                        description:
                            "Pega el enlace de tu reunión de Zoom, Meet u otra plataforma.",
                        link: true,
                        value: row.enlace_videollamada || "",
                        confirm: "Guardar enlace",
                    });
                    if (!link) return;
                    data.enlace = link;
                }
                const updated = await api(`/sesiones/${id}/${action}`, "POST", data);
                if (action === "iniciar") {
                    window.location.assign(updated.enlace_videollamada);
                    return;
                }
                await loadSessions();
                await loadHours();
                await loadBadges();
                notify(
                    action === "finalizar"
                        ? "Confirmación guardada. La sesión termina cuando ambos confirman."
                        : "Cambio guardado.",
                );
            }
        }),
    );
    // Restaurar una sesión solo si el servidor la reconoce. Nunca usar un usuario por defecto.
    try {
        await token();
        await loadCatalogs();
        const reset = new URLSearchParams(location.search).get("reset");
        if (reset) {
            authPage("reset-section");
            return;
        }
        const pending = await api("/auth/verificacion");
        if (pending.requiere_verificacion) {
            await signedIn(pending);
            return;
        }
        const user = await api("/auth/yo");
        await signedIn({ estudiante: user, csrf });
    } catch (error) {
        loggedOut();
        if (error.message !== "Inicia sesión para continuar.") notify(error.message, true);
    } finally {
        $("btn-login").disabled = false;
    }
});
