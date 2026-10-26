# test/model

Plain JUnit 5 tests of the domain rules: **no Spring context**, no `@SpringBootTest`, no repositories. They must stay fast.

- Build entities directly (`new Prodotto(...)`, `new Ordine("Rossi", ADESSO)`) in `@BeforeEach`; use a fixed `LocalDateTime` constant instead of the real clock.
- Assert on behavior through public methods and getters (stock via `getDisponibili()`, totals with `BigDecimal` at scale 2, e.g. `new BigDecimal("198.70")`).
- Test exceptions with `assertThrows` on the specific `MagazzinoException` subtype.
- Method names are Italian sentences describing the rule (`loStessoProdottoDueVolteFiniscenellaStessaRiga`). Add a test here for every new or changed business rule before any integration test.
