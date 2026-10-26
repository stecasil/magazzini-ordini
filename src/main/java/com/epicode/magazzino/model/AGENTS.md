# model

JPA entities and **all business rules**. No Spring beans, no `Clock`, no repository access: this package must stay testable as plain JUnit (`ProdottoTest`, `OrdineTest`).

- Entities have a `protected` no-arg constructor "per JPA" and a public constructor that validates input, throwing `RichiestaNonValidaException`.
- State changes go through behavior methods (`Ordine.aggiungiRiga/spedisci/annulla`, `Prodotto.preleva/riassortisci`), not setters. Collections are exposed read-only (`Collections.unmodifiableList`).
- `Ordine` owns its `RigaOrdine` list (cascade ALL, orphan removal, EAGER). Stock is withdrawn in `aggiungiRiga` and returned in `annulla`; keep that symmetry when changing either.
- `Ordine.totaleMerce()` starts from `BigDecimal.ZERO.setScale(2)`; keep money at scale 2.
- Quantity discount (`RigaOrdine.percentualeSconto()`) is computed from the merged line quantity, never stored.
- Exceptions extend `MagazzinoException` (unchecked, abstract); their message is shown to the user, so write it in Italian and make it readable. Pick the type by meaning: `RichiestaNonValida` (bad input), `OperazioneNonConsentita` (illegal state transition), `ScortaInsufficiente` (stock), `RisorsaNonTrovata` (404).
- `StatoOrdine` is persisted with `EnumType.STRING`; don't reorder or rename values casually.
