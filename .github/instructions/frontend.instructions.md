---
applyTo: "src/main/resources/templates/**,src/main/resources/static/css/**"
---

# Front-end (Thymeleaf templates + CSS)

Apply when a request changes how the UI looks or behaves. The FE is server-rendered: no JavaScript framework, no build step, no `static/js`. Behavior comes from controller + template, so a behavior change usually touches `web/` (and its `PagineTest`) as well as the template.

## Templates (`src/main/resources/templates`)

- Every page starts with `<html xmlns:th="http://www.thymeleaf.org" lang="it">` and uses the shared fragments from `fragments.html`:
  `<head th:replace="~{fragments :: head('Titolo')}">`, `<header th:replace="~{fragments :: menu}">`, and `<div th:replace="~{fragments :: avvisi}">` for feedback. Page content goes in `<main>`.
- Feedback is shown only through the `errore` / `messaggio` model or flash attributes rendered by `avvisi`; don't add ad-hoc alert markup.
- Links and form actions use `th:href` / `th:action` with `@{...}` (e.g. `@{/ordini/{id}/spedisci(id=${ordine.id})}`); mutations are `method="post"` forms (no JS, no `PUT`/`DELETE`).
- Show actions conditionally on state with `th:if` (e.g. `ordine.stato.name() == 'CONFERMATO'`). This is only UX: the rule itself must also exist in `model`.
- Format money with `#numbers.formatDecimal(x, 1, 'POINT', 2, 'COMMA')` + `' €'` and dates with `#temporals.format(x, 'dd/MM/yyyy HH:mm')`.
- Keep a natural-language static default in each `th:text` element (the prototype text), as existing templates do.
- All visible text is Italian. Use `th:text`, never `th:utext`, for user-provided data.
- Forms bind to `OrdineForm` with indexed fields (`righe[0].codice`, `righe[0].quantita`); adding a field means updating the form class, `toRichiesta()` and the template together.
- Templates can only use data the controller puts in the model; `open-in-view` is off, so lazy associations won't load in the view.

## CSS (`src/main/resources/static/css/app.css`)

- Single stylesheet, plain CSS, linked via `th:href="@{/css/app.css}"` from the `head` fragment. Don't add frameworks or CDNs.
- Reuse existing classes: `.num` (right-aligned numbers), `.stato` + `.spedito`/`.annullato` (status badge; the modifier is the lowercased `StatoOrdine`, so a new state needs a new modifier), `.avviso` + `.errore`/`.ok`, `.primario`, `.azioni`.
- Match the existing palette (primary `#d6007a`, badge purple `#6c5ce7`, success green `#1e9e6a`).

## Verification

- Thymeleaf reloads without restart (`spring.thymeleaf.cache=false`), but run `.\mvnw.cmd test -Dtest=PagineTest` after changing pages or controllers; update it when visible text, redirects or status codes change.
