# test/web

MockMvc tests of the Thymeleaf pages (`PagineTest`).

- Use `@SpringBootTest` + `@AutoConfigureMockMvc` (`org.springframework.boot.webmvc.test.autoconfigure`, Spring Boot 4 package) and an autowired `MockMvc`.
- These tests are **not** `@Transactional` and run against the catalog seeded by `DatiIniziali` (e.g. `GUA-L`, `NAS-50` which has zero stock). Don't change seed codes/stock without updating them, and avoid depending on stock that other tests in this class consume.
- Form posts use indexed params: `righe[0].codice`, `righe[0].quantita`, plus `cliente`.
- Assert status/redirect (`redirectedUrlPattern("/ordini/*")`), and rendered text with `content().string(containsString(...))`. Error re-render returns 200; unknown id returns 404.
- Keep business-rule coverage in `model`/`service` tests; here check only HTTP behavior and rendering.
