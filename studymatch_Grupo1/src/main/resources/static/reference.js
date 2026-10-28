"use strict";
// Adaptación de datos y acciones sobre el documento original de la referencia.
const originalReferenceHours = loadHours,
    originalReferenceSubscriptions = loadSubscriptions;
loadHours = async function () {
    await originalReferenceHours();
    const [h, summary] = await Promise.all([api("/perfil/horas"), api("/perfil/resumen")]);
    setText("mentor-hours", Number(h.horas_ensenadas).toFixed(1) + " hrs");
    setText(
        "mentor-rating",
        summary.calificacion_promedio === null
            ? "Sin calificaciones"
            : Number(summary.calificacion_promedio).toFixed(1) + " / 5",
    );
    const students = new Set(
        sessions
            .filter(
                (s) => s.estado === "COMPLETADA" && Number(s.mentor_id) === Number(currentUser.id),
            )
            .map((s) => s.aprendiz_id),
    );
    setText("mentor-learners", students.size + " estudiantes");
    setText("dashboard-week-description", "Sesiones completadas durante esta semana.");
};
loadSubscriptions = async function () {
    await originalReferenceSubscriptions();
    const paid = plans.find((p) => Number(p.precio) > 0),
        active = subscriptions.find((s) => ["ACTIVO", "TRIAL"].includes(s.estado_actual));
    document
        .querySelectorAll("[data-plan-price]")
        .forEach(
            (e) =>
                (e.textContent =
                    e.dataset.planPrice === "free"
                        ? "Sin costo"
                        : paid
                          ? `${paid.moneda} ${Number(paid.precio).toFixed(2)}`
                          : "No disponible"),
        );
    setText(
        "plan-summary-description",
        active
            ? "Consulta los beneficios y la vigencia de tu plan."
            : "Tu cuenta utiliza el plan gratuito.",
    );
    setText(
        "mi-plan-dias-restantes",
        active?.fecha_vencimiento
            ? Math.max(
                  0,
                  Math.ceil(
                      (new Date(active.fecha_vencimiento + "T23:59:59-05:00") - new Date()) /
                          86400000,
                  ),
              )
            : "—",
    );
    for (const id of [
        "btn-contratar-pro-card",
        "btn-confirmar-plan-real",
        "btn-contratar-plan-oficial",
    ])
        if ($(id)) $(id).disabled = !paid;
};
function openReferenceOffer() {
    const plan = plans.find((p) => Number(p.precio) > 0);
    if (!plan) throw new Error("No hay planes disponibles registrados.");
    openPlan(plan.id);
    show("contratacion-vista-oferta", true);
    show("contratacion-vista-confirmacion", false);
}
const referenceNotices = loadNotifications;
loadNotifications = async function () {
    await referenceNotices();
    setText(
        "notification-active-count",
        notificationRows.filter((n) => !n.leida).length + " alertas sin leer",
    );
};
document.addEventListener("DOMContentLoaded", () => {
    document.addEventListener(
        "click",
        handle(async (event) => {
            const list = event.target.closest("[data-open-datalist]");
            if (list) {
                const input = $(list.dataset.openDatalist);
                await loadSubjectPage(input.id);
                input.focus();
                try {
                    input.showPicker();
                } catch {}
            }
            const file = event.target.closest("[data-file-trigger]");
            if (file) $(file.dataset.fileTrigger).click();
            const tab = event.target.closest("[data-profile-tab]");
            if (tab) {
                show("modal-companero-perfil", false);
                await navigate("perfil-section");
                document
                    .querySelector(
                        '#tab-informacion .tab[data-target="' + tab.dataset.profileTab + '"]',
                    )
                    .click();
            }
        }),
    );
    on("btn-contratar-pro-card", openReferenceOffer);
    on("btn-cerrar-contratacion-x", () => show("modal-contratacion-plan", false));
    on("btn-confirmar-pagar", openDemoPayment);
    on("btn-rf37-editar-metodo-pago", () =>
        notify("La pasarela actual es de prueba y no almacena tarjetas."),
    );
    on("btn-mi-plan-ver-opciones", openReferenceOffer);
    on("btn-cerrar-banner-mod", () => show("chat-banner-moderacion", false));
    for (const button of document.querySelectorAll(
        '#modal-companero-perfil [data-action="peer-chat"],#modal-companero-perfil [data-action="peer-propose"]',
    ))
        button.type = "button";
});
("use strict");
const sharedReferencePanels = updateSessionPanels;
updateSessionPanels = function () {
    if (!currentUser) return;
    const requests = sessions.filter(
        (s) =>
            s.estado === "PROPUESTA" &&
            Number(s.receptor_id) === Number(currentUser.id) &&
            Number(s.mentor_id) === Number(currentUser.id),
    );
    setText("mentor-pending-num", requests.length);
    const requestCard = (s) => {
        const card = $("reference-request-template").content.firstElementChild.cloneNode(true);
        const fill = (name, value) => {
            const e = card.querySelector('[data-field="' + name + '"]');
            if (e) e.textContent = value;
        };
        fill("avatar", initials(s.companero));
        fill("companero", s.companero);
        fill("materia", s.materia);
        fill("estado", "Propuesta");
        fill("modalidad", "Videollamada");
        fill("fecha", dateText(s.fecha_hora));
        fill("lugar", s.duracion_programada_min + " minutos");
        card.querySelector('[data-field="acciones"]').innerHTML =
            `<button class="btn btn-secondary" data-session="${s.id}" data-session-action="cancelar">Cancelar</button><button class="btn btn-primary" data-session="${s.id}" data-session-action="aceptar">Aceptar</button>`;
        return card.outerHTML;
    };
    $("mentor-requests").innerHTML = requests.length
        ? requests.map(requestCard).join("")
        : empty("No tienes solicitudes pendientes para enseñar.");
    const completed = sessions.filter((s) => s.estado === "COMPLETADA");
    setText("profile-history-count", completed.length + " sesiones registradas");
    $("profile-history-live").innerHTML = completed.length
        ? completed
              .map((s) => {
                  const card = $("reference-history-template").content.firstElementChild.cloneNode(
                          true,
                      ),
                      mentor = Number(s.mentor_id) === Number(currentUser.id);
                  for (const [name, value] of [
                      ["materia", s.materia],
                      ["rol", mentor ? "ROL MENTOR" : "ROL APRENDIZ"],
                      ["detalle", s.companero + " · " + dateText(s.fecha_hora)],
                      [
                          "horas",
                          (Number(s.duracion_real_min) / 60).toFixed(1) + " hrs · Completada",
                      ],
                  ])
                      card.querySelector('[data-field="' + name + '"]').textContent = value;
                  return card.outerHTML;
              })
              .join("")
        : empty("Todavía no hay sesiones completadas.");
};
