// Fallback de renderizacao para diagramas Mermaid.
//
// O tema Material ja renderiza os blocos ```mermaid``` automaticamente na maioria
// dos navegadores. Em alguns ambientes, porem, a inicializacao automatica falha e
// deixa os blocos vazios. Este script detecta esse caso e, apenas entao, recupera
// o codigo-fonte do diagrama e o renderiza. Quando o Material ja renderizou, este
// script nao faz nada (evita renderizacao dupla).

(function () {
  function decodeEntities(s) {
    var ta = document.createElement("textarea");
    ta.innerHTML = s;
    return ta.value;
  }

  function boot() {
    var blocks = Array.prototype.slice.call(document.querySelectorAll(".mermaid"));
    if (!blocks.length) return;

    var pending = blocks.filter(function (b) { return !b.querySelector("svg"); });
    if (!pending.length) return; // Material ja renderizou; nada a fazer.

    if (!window.mermaid) { setTimeout(boot, 80); return; }

    // Material esvazia os blocos ao processa-los, entao recuperamos o codigo-fonte
    // original a partir do HTML servido (mesma origem, geralmente vindo do cache).
    fetch(location.href, { cache: "force-cache" })
      .then(function (r) { return r.text(); })
      .then(function (html) {
        var re = /<pre class="mermaid"><code>([\s\S]*?)<\/code><\/pre>/g;
        var srcs = [], m;
        while ((m = re.exec(html)) !== null) srcs.push(decodeEntities(m[1]));

        try { window.mermaid.initialize({ startOnLoad: false }); } catch (e) {}

        blocks.forEach(function (el, i) {
          if (el.querySelector("svg") || !srcs[i]) return;
          var id = "mmd-" + i + "-" + Date.now();
          window.mermaid.render(id, srcs[i].trim())
            .then(function (res) { el.innerHTML = res.svg; })
            .catch(function (err) { console.error("Mermaid fallback:", err); });
        });
      })
      .catch(function (err) { console.error("Mermaid fallback fetch:", err); });
  }

  // Damos tempo para o tema tentar renderizar primeiro; so entramos em acao se ele falhar.
  function schedule() { setTimeout(boot, 400); }

  if (document.readyState !== "loading") schedule();
  else document.addEventListener("DOMContentLoaded", schedule);

  // Reexecuta na navegacao instantanea do Material.
  if (window.document$ && typeof window.document$.subscribe === "function") {
    window.document$.subscribe(schedule);
  }
})();
