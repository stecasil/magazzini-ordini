# service

Transactional use cases that orchestrate `model` + `repository`. They contain no rules of their own beyond input checks (e.g. an order needs at least one line); rules belong in `model`.

- Classes are `@Service` + `@Transactional`; mark pure queries `@Transactional(readOnly = true)`. A failure mid-use-case must leave nothing written.
- Mutating use cases load the entity, call the domain method and return it; no explicit `save` is needed for managed entities (see `spedisci`/`annulla`).
- Constructor injection; take `Clock` and use `LocalDateTime.now(clock)` for timestamps.
- Not-found lookups throw `RisorsaNonTrovataException` with an Italian message.
- Inputs are plain records (`NuovoOrdine` with nested `Riga`), independent of the web form.
- `open-in-view` is off: return entities fully loaded for the template (e.g. `Ordine.righe` is EAGER).
- Tests are `@SpringBootTest` + `@Transactional` integration tests (`OrdineServiceTest`).
