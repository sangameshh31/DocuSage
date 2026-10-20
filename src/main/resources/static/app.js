"use strict";

/* DocuSage web UI — a thin client over the REST API in this same app.
   Endpoints used:
     GET    /api/health
     GET    /api/documents
     POST   /api/documents        (multipart, field "file")
     DELETE /api/documents/{id}
     POST   /api/ask              { question, documentId? }
*/

const state = {
  documents: [],
  selectedDocId: null, // null = search across all documents
  chosenFile: null,
};

const $ = (id) => document.getElementById(id);

async function api(path, options) {
  const res = await fetch(path, options);
  if (!res.ok) {
    let message = "Request failed (" + res.status + ")";
    try {
      const body = await res.json();
      if (body && body.message) message = body.message;
    } catch (ignored) { /* keep default message */ }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

function setMsg(el, text, kind) {
  el.textContent = text || "";
  el.className = "msg" + (kind ? " " + kind : "");
}

/* ---------- health / status pill ---------- */

async function loadHealth() {
  const pill = $("statusPill");
  try {
    const h = await api("/api/health");
    pill.textContent = "● " + h.documents + " docs · " + h.indexedChunks + " chunks · "
      + h.embeddingProvider + " · " + h.chatProvider;
    pill.className = "pill pill-ok";
  } catch (e) {
    pill.textContent = "● API unreachable";
    pill.className = "pill pill-bad";
  }
}

/* ---------- documents ---------- */

function renderDocuments() {
  const list = $("docList");
  list.textContent = "";
  $("docEmpty").style.display = state.documents.length ? "none" : "block";

  for (const doc of state.documents) {
    const li = document.createElement("li");
    li.className = "doc-item" + (state.selectedDocId === doc.id ? " selected" : "");
    li.title = "Click to scope questions to this document";

    const meta = document.createElement("div");
    meta.className = "doc-meta";
    const name = document.createElement("div");
    name.className = "doc-name";
    name.textContent = doc.name;
    const sub = document.createElement("div");
    sub.className = "doc-sub";
    sub.textContent = doc.chunkCount + " chunks · " + formatBytes(doc.sizeBytes);
    meta.append(name, sub);

    const del = document.createElement("button");
    del.className = "doc-delete";
    del.textContent = "✕";
    del.title = "Delete document";
    del.addEventListener("click", (ev) => {
      ev.stopPropagation();
      deleteDocument(doc.id);
    });

    li.append(meta, del);
    li.addEventListener("click", () => {
      state.selectedDocId = state.selectedDocId === doc.id ? null : doc.id;
      renderDocuments();
      renderScope();
    });
    list.appendChild(li);
  }
}

function renderScope() {
  const doc = state.documents.find((d) => d.id === state.selectedDocId);
  $("scopeLabel").textContent = doc ? doc.name : "all documents";
}

async function loadDocuments() {
  try {
    state.documents = await api("/api/documents");
    if (state.selectedDocId && !state.documents.some((d) => d.id === state.selectedDocId)) {
      state.selectedDocId = null;
    }
    renderDocuments();
    renderScope();
  } catch (e) {
    setMsg($("uploadMsg"), e.message, "error");
  }
}

async function deleteDocument(id) {
  try {
    await api("/api/documents/" + encodeURIComponent(id), { method: "DELETE" });
    if (state.selectedDocId === id) state.selectedDocId = null;
    await loadDocuments();
    await loadHealth();
  } catch (e) {
    setMsg($("uploadMsg"), e.message, "error");
  }
}

function formatBytes(n) {
  if (n < 1024) return n + " B";
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB";
  return (n / (1024 * 1024)).toFixed(1) + " MB";
}

/* ---------- upload ---------- */

function chooseFile(file) {
  state.chosenFile = file || null;
  $("dropLabel").textContent = file ? file.name : "Click to choose a file, or drop it here";
  $("uploadBtn").disabled = !file;
}

async function upload() {
  if (!state.chosenFile) return;
  const btn = $("uploadBtn");
  btn.disabled = true;
  setMsg($("uploadMsg"), "Uploading and indexing…", "");
  try {
    const form = new FormData();
    form.append("file", state.chosenFile);
    const doc = await api("/api/documents", { method: "POST", body: form });
    setMsg($("uploadMsg"), "Indexed \"" + doc.name + "\" into " + doc.chunkCount + " chunks.", "ok");
    chooseFile(null);
    $("fileInput").value = "";
    await loadDocuments();
    await loadHealth();
  } catch (e) {
    setMsg($("uploadMsg"), e.message, "error");
  } finally {
    btn.disabled = !state.chosenFile;
  }
}

/* ---------- ask ---------- */

async function ask() {
  const question = $("questionInput").value.trim();
  if (!question) {
    setMsg($("askMsg"), "Type a question first.", "error");
    return;
  }
  const btn = $("askBtn");
  btn.disabled = true;
  setMsg($("askMsg"), "Searching your documents…", "");
  try {
    const body = { question };
    if (state.selectedDocId) body.documentId = state.selectedDocId;
    const res = await api("/api/ask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    renderAnswer(res);
    setMsg($("askMsg"), "", "");
  } catch (e) {
    setMsg($("askMsg"), e.message, "error");
  } finally {
    btn.disabled = false;
  }
}

function renderAnswer(res) {
  $("answerCard").classList.remove("hidden");
  $("answerText").textContent = res.answer;
  $("providerLine").textContent =
    "Embeddings: " + res.embeddingProvider + " · Answers: " + res.chatProvider;

  const list = $("citationList");
  list.textContent = "";
  if (!res.citations || res.citations.length === 0) {
    const p = document.createElement("p");
    p.className = "hint";
    p.textContent = "No passages were retrieved for this question.";
    list.appendChild(p);
    return;
  }
  res.citations.forEach((c, i) => {
    const box = document.createElement("div");
    box.className = "citation";

    const head = document.createElement("div");
    head.className = "citation-head";
    const title = document.createElement("span");
    title.textContent = "[" + (i + 1) + "] " + c.documentName + " · chunk " + (c.chunkIndex + 1);
    const score = document.createElement("span");
    score.className = "score-label";
    const pct = Math.max(0, Math.min(100, Math.round(c.score * 100)));
    score.textContent = "similarity " + pct + "%";
    head.append(title, score);

    const excerpt = document.createElement("p");
    excerpt.className = "citation-excerpt";
    excerpt.textContent = c.excerpt;

    const bar = document.createElement("div");
    bar.className = "scorebar";
    const fill = document.createElement("div");
    fill.style.width = pct + "%";
    bar.appendChild(fill);

    box.append(head, excerpt, bar);
    list.appendChild(box);
  });
}

/* ---------- wiring ---------- */

$("fileInput").addEventListener("change", (e) => chooseFile(e.target.files[0]));
$("uploadBtn").addEventListener("click", upload);
$("askBtn").addEventListener("click", ask);
$("questionInput").addEventListener("keydown", (e) => {
  if (e.key === "Enter") ask();
});

const dropZone = $("dropZone");
dropZone.addEventListener("dragover", (e) => {
  e.preventDefault();
  dropZone.classList.add("dragging");
});
dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragging"));
dropZone.addEventListener("drop", (e) => {
  e.preventDefault();
  dropZone.classList.remove("dragging");
  if (e.dataTransfer.files.length) chooseFile(e.dataTransfer.files[0]);
});

loadHealth();
loadDocuments();
