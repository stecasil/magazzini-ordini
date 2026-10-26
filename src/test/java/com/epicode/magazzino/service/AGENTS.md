# test/service

Integration tests of use cases against the real service, repositories and in-memory H2.

- Annotate with `@SpringBootTest` + `@Transactional`: each test rolls back, so state doesn't leak.
- Seed what the test needs in `@BeforeEach` via `ProdottoRepository`, with `T-` prefixed codes (e.g. `T-VITI`) so they never collide with `DatiIniziali` data.
- Autowire the service under test and the repositories used to verify side effects (e.g. stock after `crea`).
- Cover transactional outcomes too: a failing line (missing product, insufficient stock) must leave stock unchanged.
- Italian behavior-describing method names; `assertThrows` with the specific `MagazzinoException` subtype.
