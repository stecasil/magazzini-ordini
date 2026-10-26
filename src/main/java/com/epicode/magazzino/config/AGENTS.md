# config

Spring configuration and startup data.

- `OrologioConfig` provides the `Clock` bean; inject it wherever the current time is needed so tests can substitute a fixed clock.
- `DatiIniziali` (`CommandLineRunner`) seeds the product catalog only when the table is empty. H2 is in-memory with `create-drop`, so the catalog is recreated on each start. Seed codes (e.g. `VIT-M6-30`, `NAS-50` with zero stock) are used by the demo; `web` MockMvc tests rely on them, while service tests create their own `T-`-prefixed products.
