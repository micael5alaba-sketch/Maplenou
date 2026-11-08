// Maplenou documentation assistant widget.
// Asks the assistant service (assistant/, behind /api/assistant on the docs server) and shows the answer with
// numbered references that link to the exact documentation sections. Configuration comes from config.js,
// generated per site by documentation/build.py: window.MAPLENOU_DOCS = { site, lang, endpoint }.
(function () {
  "use strict";
  var cfg = window.MAPLENOU_DOCS || {};
  var site = cfg.site || "user";
  var lang = cfg.lang === "en" ? "en" : "fr";
  var endpoint = (cfg.endpoint || "/api/assistant").replace(/\/$/, "");

  var T = {
    fr: {
      open: "Poser une question",
      title: "Assistant de la documentation",
      subtitle: "Réponses tirées de cette documentation, avec leurs références.",
      placeholder: "Ex. : Comment ajouter une pièce jointe ?",
      send: "Envoyer",
      close: "Fermer l'assistant",
      thinking: "Recherche dans la documentation…",
      references: "Références",
      seeAlso: "Voir aussi",
      retrieval: "Réponse sans modèle de langage : sections les plus proches.",
      error: "L'assistant est indisponible. Utilisez la recherche (touche /) ou réessayez plus tard.",
      rateLimited: "Trop de questions d'affilée. Patientez une minute.",
      disclaimer: "L'assistant peut se tromper : vérifiez dans les sections citées.",
      clear: "Effacer",
      examples: { user: ["Comment créer un ticket ?", "Quels fichiers puis-je joindre ?", "Comment rouvrir un ticket résolu ?"],
                  staff: ["Quels sont les délais SLA ?", "Quand escalader un ticket ?", "Comment traiter une demande de réouverture ?"],
                  developer: ["Comment lancer le projet sans Java ?", "Comment ajouter une traduction ?", "Comment fonctionne l'assistant ?"] }
    },
    en: {
      open: "Ask a question",
      title: "Documentation assistant",
      subtitle: "Answers taken from this documentation, with their references.",
      placeholder: "E.g. How do I add an attachment?",
      send: "Send",
      close: "Close the assistant",
      thinking: "Searching the documentation…",
      references: "References",
      seeAlso: "See also",
      retrieval: "Answer without the language model: closest sections.",
      error: "The assistant is unavailable. Use the search (key /) or try again later.",
      rateLimited: "Too many questions in a row. Please wait a minute.",
      disclaimer: "The assistant can make mistakes: check the cited sections.",
      clear: "Clear",
      examples: { user: ["How do I create a ticket?", "Which files can I attach?", "How do I reopen a resolved ticket?"],
                  staff: ["What are the SLA targets?", "When should I escalate a ticket?", "How do I handle a reopening request?"],
                  developer: ["How do I run the project without Java?", "How do I add a translation?", "How does the assistant work?"] }
    }
  }[lang];

  function el(tag, attrs, children) {
    var node = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === "text") node.textContent = attrs[k];
      else if (k.indexOf("on") === 0) node.addEventListener(k.slice(2), attrs[k]);
      else node.setAttribute(k, attrs[k]);
    });
    (children || []).forEach(function (c) { if (c) node.appendChild(c); });
    return node;
  }

  // ------------------------------------------------------------------ answer rendering (no HTML from the model)

  function inline(text, refs, parent) {
    // [n] -> superscript link to the reference; **bold** kept; everything else as text
    var parts = text.split(/(\[\d{1,2}\]|\*\*[^*]+\*\*)/g);
    parts.forEach(function (part) {
      var cite = part.match(/^\[(\d{1,2})\]$/);
      if (cite) {
        var ref = refs.filter(function (r) { return r.n === Number(cite[1]); })[0];
        if (ref) {
          parent.appendChild(el("a", { class: "mla-cite", href: ref.url, title: ref.breadcrumb, text: "[" + ref.n + "]" }));
          return;
        }
      }
      var bold = part.match(/^\*\*([^*]+)\*\*$/);
      if (bold) parent.appendChild(el("strong", { text: bold[1] }));
      else if (part) parent.appendChild(document.createTextNode(part));
    });
  }

  function renderAnswer(container, text, refs) {
    container.textContent = "";
    var list = null, listType = null;
    text.split(/\n+/).forEach(function (line) {
      var trimmed = line.trim();
      if (!trimmed) return;
      var ordered = trimmed.match(/^(\d+)[.)]\s+(.*)$/);
      var bullet = trimmed.match(/^[-*•]\s+(.*)$/);
      if (ordered || bullet) {
        var type = ordered ? "ol" : "ul";
        if (!list || listType !== type) {
          list = el(type);
          listType = type;
          container.appendChild(list);
        }
        var li = el("li");
        inline(ordered ? ordered[2] : bullet[1], refs, li);
        list.appendChild(li);
      } else {
        list = null;
        var p = el("p");
        inline(trimmed.replace(/^#+\s*/, ""), refs, p);
        container.appendChild(p);
      }
    });
  }

  function renderReferences(container, refs) {
    container.textContent = "";
    if (!refs || !refs.length) return;
    var cited = refs.some(function (r) { return r.cited !== false; });
    container.appendChild(el("p", { class: "mla-refs-title", text: cited ? T.references : T.seeAlso }));
    var ol = el("ol", { class: "mla-refs" });
    refs.forEach(function (r) {
      ol.appendChild(el("li", { value: String(r.n) }, [
        el("a", { href: r.url, text: r.breadcrumb || r.title }),
        r.excerpt ? el("span", { class: "mla-excerpt", text: r.excerpt }) : null
      ]));
    });
    container.appendChild(ol);
  }

  // ------------------------------------------------------------------ widget

  var button = el("button", { type: "button", class: "mla-launcher", "aria-haspopup": "dialog", "aria-expanded": "false" }, [
    el("span", { "aria-hidden": "true", class: "mla-icon", text: "?" }),
    el("span", { text: T.open })
  ]);
  var log = el("div", { class: "mla-log", "aria-live": "polite" });
  var input = el("textarea", { class: "mla-input", rows: "2", maxlength: "500", placeholder: T.placeholder, "aria-label": T.placeholder });
  var send = el("button", { type: "submit", class: "mla-send", text: T.send });
  var form = el("form", { class: "mla-form" }, [input, send]);
  var examples = el("div", { class: "mla-examples" });
  (T.examples[site] || []).forEach(function (q) {
    examples.appendChild(el("button", { type: "button", class: "mla-chip", text: q, onclick: function () { ask(q); } }));
  });
  var panel = el("section", { class: "mla-panel", role: "dialog", "aria-modal": "false", "aria-labelledby": "mla-title", hidden: "" }, [
    el("header", { class: "mla-header" }, [
      el("div", {}, [el("h2", { id: "mla-title", text: T.title }), el("p", { text: T.subtitle })]),
      el("button", { type: "button", class: "mla-close", "aria-label": T.close, text: "×", onclick: function () { toggle(false); } })
    ]),
    log,
    examples,
    form,
    el("p", { class: "mla-disclaimer", text: T.disclaimer })
  ]);

  function toggle(open) {
    panel.hidden = !open;
    button.setAttribute("aria-expanded", String(open));
    if (open) input.focus(); else button.focus();
  }

  button.addEventListener("click", function () { toggle(panel.hidden); });
  document.addEventListener("keydown", function (e) { if (e.key === "Escape" && !panel.hidden) toggle(false); });
  input.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); form.requestSubmit ? form.requestSubmit() : send.click(); }
  });
  form.addEventListener("submit", function (e) { e.preventDefault(); ask(input.value); });

  var busy = false;

  function ask(question) {
    question = (question || "").trim();
    if (!question || busy) return;
    busy = true;
    send.disabled = true;
    examples.hidden = true;
    input.value = "";
    log.appendChild(el("div", { class: "mla-msg mla-q", text: question }));
    var answerBox = el("div", { class: "mla-answer" }, [el("p", { class: "mla-thinking", text: T.thinking })]);
    var notice = el("p", { class: "mla-notice" });
    var refsBox = el("div");
    log.appendChild(el("div", { class: "mla-msg mla-a" }, [answerBox, notice, refsBox]));
    log.scrollTop = log.scrollHeight;

    var text = "";
    var refs = [];
    fetch(endpoint + "/ask?stream=1", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question: question, site: site, lang: lang })
    }).then(function (res) {
      if (res.status === 429) throw new Error("rate");
      if (!res.ok || !res.body) throw new Error("http");
      var reader = res.body.getReader();
      var decoder = new TextDecoder();
      var buffer = "";
      function handle(event) {
        if (event.type === "references") {
          refs = event.references;
        } else if (event.type === "token") {
          text += event.text;
          renderAnswer(answerBox, text, refs);
        } else if (event.type === "done") {
          refs = event.references || [];
          renderAnswer(answerBox, event.answer || text, refs);
          renderReferences(refsBox, refs);
          if (event.mode === "retrieval") notice.textContent = T.retrieval;
        }
        log.scrollTop = log.scrollHeight;
      }
      function pump() {
        return reader.read().then(function (chunk) {
          if (chunk.done) return;
          buffer += decoder.decode(chunk.value, { stream: true });
          var lines = buffer.split("\n");
          buffer = lines.pop();
          lines.forEach(function (line) { if (line.trim()) handle(JSON.parse(line)); });
          return pump();
        });
      }
      return pump();
    }).catch(function (err) {
      answerBox.textContent = "";
      answerBox.appendChild(el("p", { class: "mla-error", text: err && err.message === "rate" ? T.rateLimited : T.error }));
    }).then(function () {
      busy = false;
      send.disabled = false;
      input.focus();
    });
  }

  function mount() {
    document.body.appendChild(button);
    document.body.appendChild(panel);
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", mount);
  else mount();
})();
