# repository

Spring Data JPA interfaces extending `JpaRepository`, one per aggregate (`OrdineRepository`, `ProdottoRepository`).

- Interfaces only; use derived query names (e.g. `findAllByOrderByCreatoIlDesc`, `findByCodice` returning `Optional`).
- No business logic here; callers in `service` turn an empty `Optional` into `RisorsaNonTrovataException`.
- Schema comes from entity annotations (`ddl-auto=create-drop`); there are no migration scripts.
