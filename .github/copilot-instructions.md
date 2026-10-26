# Copilot instructions

Spring Boot 4.1.1 / Java 25 app (Maven Wrapper) for orders and warehouse stock: Spring MVC + Thymeleaf pages, Spring Data JPA, in-memory H2. Code, identifiers, comments and UI text are in **Italian** — keep that.

## Build and test

```powershell
.\mvnw.cmd test                                   # full suite (must stay green)
.\mvnw.cmd test -Dtest=OrdineTest                 # one test class
.\mvnw.cmd test -Dtest=OrdineTest#methodName      # one test method
.\mvnw.cmd spring-boot:run                        # http://localhost:8080
```

No linter or formatter is configured.

## Architecture

Request flow: **controller (`web`) → service → model/repository**.

- `model/`: JPA entities **and** the business rules (`Ordine`, `RigaOrdine`, `Prodotto`, `StatoOrdine`) plus the exceptions. Rules live here, not in controllers or services.
- `service/`: transactional use cases that orchestrate model + repositories (`OrdineService`, `CatalogoService`). `@Transactional` at class level (`readOnly = true` on queries), so a failing line leaves nothing written. `NuovoOrdine` is the service input record.
- `web/`: thin Thymeleaf controllers, form objects (`OrdineForm`), and `GestoreErrori` (`@ControllerAdvice`) mapping `RisorsaNonTrovataException` to a 404 `errore` page.
- `config/`: injectable `Clock` (`OrologioConfig`) and seed data (`DatiIniziali`). H2 is `create-drop`; the catalog is reseeded on every start. `open-in-view=false`, so load what a template needs inside the service.

Domain exceptions all extend `MagazzinoException` (`ScortaInsufficiente`, `OperazioneNonConsentita`, `RichiestaNonValida`, `RisorsaNonTrovata`).

## Business rules (preserve them)

- Order starts `CONFERMATO`; may only go to `SPEDITO` or `ANNULLATO`. A shipped order cannot be cancelled.
- Each line withdraws stock; if insufficient the order is not created.
- The same product added twice merges into one line.
- Line price is the product price at order time.
- Cancelling returns stock to the warehouse.
- Money is `BigDecimal`, rounded to the cent.
- Quantity discount per line, on the merged quantity: 1–9 → 0%, 10–49 → 5%, ≥50 → 10% (`RigaOrdine.percentualeSconto()`, computed, not stored). `subtotale()` = price × qty minus discount, `HALF_UP`.
- `Ordine.totaleMerce()` = sum of discounted subtotals; `speseSpedizione()` = 9.90 below 150.00, free from 150.00; `totaleDaPagare()` = merce + spedizione. The order list shows `totaleDaPagare()`.

## Conventions

- Inject `Clock` and use `LocalDateTime.now(clock)`; never call `now()` without it, so tests can fix time.
- Constructor injection; no field `@Autowired`.
- Controllers catch `MagazzinoException` themselves: on POST-redirect they use `RedirectAttributes.addFlashAttribute("errore" | "messaggio", ...)`; on form re-render (`ordine-nuovo`) they put `errore` and `prodotti` in the `Model`. Only `RisorsaNonTrovataException` is left to `GestoreErrori` (404).
- Templates share `fragments.html`: `head(titolo)`, `menu`, and `avvisi` (renders the `errore`/`messaggio` attributes). Reuse them in new pages.
- Tests: domain tests (`model/`) are plain JUnit with no Spring; service tests and `web/PagineTest` (MockMvc) are integration tests. Put new rule tests at the domain level first. Integration tests use `@SpringBootTest` + `@Transactional` (rollback per test), service tests create their own products with `T-` prefixed codes in `@BeforeEach` (`PagineTest` instead uses the seeded catalog), and have Italian method names describing behavior (e.g. `creaUnOrdineEScalaLeScorte`).
