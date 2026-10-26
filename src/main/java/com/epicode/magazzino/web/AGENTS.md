# web

Thin Thymeleaf controllers: translate HTTP to service calls, no business rules.

- Controllers are `@Controller` (not REST) and return template names from `src/main/resources/templates` or `redirect:` URLs.
- Catch `MagazzinoException` in the controller:
  - POST that redirects: `RedirectAttributes.addFlashAttribute("errore" | "messaggio", ...)`.
  - POST that re-renders a form (`ordine-nuovo`): add `errore` and the data the template needs (`prodotti`) to the `Model`.
- Let `RisorsaNonTrovataException` propagate; `GestoreErrori` (`@ControllerAdvice`) renders the 404 `errore` page.
- Form objects (`OrdineForm`) are mutable beans bound by Spring MVC and convert to a service input via `toRichiesta()`; blank form rows are dropped there.
- Templates reuse `fragments.html` (`head(titolo)`, `menu`, `avvisi`); `avvisi` displays `errore`/`messaggio`.
- UI text is Italian. Tests: `PagineTest` (MockMvc).
