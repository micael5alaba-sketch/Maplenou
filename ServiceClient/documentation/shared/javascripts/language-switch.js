// Keeps the reader on the same page when switching language (/user/fr/x/ <-> /user/en/x/).
// Pages have the same path in both languages; if one does not exist, the other language's home page opens.
(function () {
  var match = location.pathname.match(/^(.*?\/)(user|staff|developer)\/(fr|en)\/(.*)$/);
  if (!match) return;
  var prefix = match[1], site = match[2], lang = match[3], rest = match[4];
  document.querySelectorAll("a[hreflang]").forEach(function (link) {
    var target = link.getAttribute("hreflang");
    if (!target || target === lang) return;
    var samePage = prefix + site + "/" + target + "/" + rest + location.hash;
    var home = prefix + site + "/" + target + "/";
    link.setAttribute("href", samePage);
    link.addEventListener("click", function (event) {
      event.preventDefault();
      fetch(samePage, { method: "HEAD" })
        .then(function (r) { location.assign(r.ok ? samePage : home); })
        .catch(function () { location.assign(home); });
    });
  });
})();
