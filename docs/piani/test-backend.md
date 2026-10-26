# Plan: full backend tests for magazzino-ordini

Repo: `C:\Users\S226461\git\corso\magazzino-ordini`. Write tests only; no production code, pom, or dependency changes (no JaCoCo).
Status: **implemented**. `.\mvnw.cmd test` → 90 tests, 0 failures (was 36).

## Current coverage
- `model/OrdineTest` (22) covers the discount/shipping rules, merging, cancel, and ship.
- `model/ProdottoTest` (5) covers preleva and validation.
- `service/OrdineServiceTest` (4) covers only `crea`.
- `web/PagineTest` (5) covers products, form create, stock error, 404, and the detail totals.

## Gaps → tests to add

### 1. Model (plain JUnit, no Spring): extend existing classes
- `ProdottoTest`:
  - `riassortisci` adds pieces.
  - The constructor rejects blank/null code or name, negative stock, and negative scorta minima.
  - Price zero is allowed.
  - The `ScortaInsufficienteException` message includes the code, requested, and available.
- `OrdineTest`:
  - A new order is CONFERMATO and stores the cliente stripped.
  - A cancelled order cannot be shipped, and a shipped order cannot be shipped again.
  - No lines can be added to an ANNULLATO order.
  - Adding a line with insufficient stock throws and adds no line.
  - Quantity 0 is rejected.
  - `getRighe()` is unmodifiable.
  - Cancel returns the merged quantity (5+5) to stock.
  - An empty order has merchandise total 0.00 and shipping 9.90.

### 2. Form (plain JUnit): new `web/OrdineFormTest`
- The new form has 3 empty lines.
- `toRichiesta` skips lines with a blank code or null quantity.
- It maps the code and quantity, and passes the cliente through.

### 3. Repository (`@DataJpaTest`): new `repository/RepositoryTest`
- `ProdottoRepository.findByCodice` finds the product, and returns empty when the code is unknown.
- `findAllByOrderByNomeAsc` returns products sorted by name.
- `OrdineRepository.findAllByOrderByCreatoIlDesc` returns orders newest first; `creatoIl` is passed explicitly, so the order is deterministic.
- Saving an order cascades its lines, and reloading keeps the line price and quantity.
- Note: `DatiIniziali` (`CommandLineRunner`) does not run in a `@DataJpaTest` slice; use `T-` codes anyway.

### 4. Service (`@SpringBootTest` + `@Transactional`)
- `OrdineServiceTest`, extended:
  - **Fixed Clock** via a nested `@TestConfiguration` that provides a `@Primary Clock.fixed(...)` bean; `crea` sets `creatoIl` to that instant.
  - `crea` merges duplicate lines and applies the discount.
  - A failure on the second line leaves the stock of the first line unchanged (rollback). The test ends the test-managed transaction with `TestTransaction.end()` so that `crea` rolls back for real, and it uses seed products.
  - `trova` finds the order, or throws `RisorsaNonTrovata` when the id is unknown.
  - `elenco` contains the created orders.
  - `spedisci` changes the state; `spedisci` of a missing id throws `RisorsaNonTrovata`.
  - `annulla` restores stock; `annulla` after spedisci throws `OperazioneNonConsentita`.
  - A blank cliente throws `RichiestaNonValida`.
- New `CatalogoServiceTest`:
  - `elenco` is sorted by name and includes the `T-` products.
  - `trova` finds the product, and throws `RisorsaNonTrovata` when the code is unknown.
  - `riassortisci` increases stock.
  - `riassortisci` with quantity 0 throws `RichiestaNonValida`.

### 5. Web (MockMvc, seeded catalog, not transactional): extend `PagineTest`
- `/` redirects to `/prodotti`.
- `GET /ordini` returns 200 and renders the list.
- `GET /ordini/nuovo` returns 200 and the form lists products.
- POST without lines re-renders with the "almeno una riga" error.
- POST with an unknown product re-renders with "non trovato" and the response is 200, not 404.
- `POST /ordini/{id}/spedisci` redirects with the flash "Ordine spedito"; a second spedisci gives a flash error.
- `POST /ordini/{id}/annulla` gives the flash "Ordine annullato"; annulla after spedisci gives a flash error.
- `POST /ordini/9999/spedisci` redirects with the flash error "Ordine 9999 non trovato"; the controller catches every `MagazzinoException` on POST, so there is no 404 here.
- `GET /ordini/9999` returns 404 and the `errore` page shows "Ordine 9999 non trovato".
- Each test creates its own order with small quantities of high-stock seed items (`GUA-L`, `MET-5`, `COL-1KG`) so tests stay independent.

## Conventions to respect
- Use Italian method names that describe behaviour, and `assertThrows` with specific `MagazzinoException` subtypes.
- Model tests must not use Spring. Service tests use `T-` products seeded in `@BeforeEach`.
- Do not remove or skip existing tests.
- New test classes: `OrdineFormTest`, `RepositoryTest`, `CatalogoServiceTest`. Everything else extends existing classes.

## Verify
- Run `.\mvnw.cmd test` from the repo root and confirm all tests pass, reporting the real counts from the surefire reports.
