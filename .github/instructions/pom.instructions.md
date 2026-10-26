---
applyTo: "pom.xml"
---

# Maven build (`pom.xml`)

- The parent is `spring-boot-starter-parent` 4.1.1 (Java 25 via `java.version`). It manages dependency versions: don't add `<version>` to Spring Boot starters or other managed artifacts.
- Spring Boot 4 uses modular starters; follow the existing naming (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, and `-test` starters such as `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`). Don't reintroduce `spring-boot-starter-web` or `spring-boot-starter-test`.
- Keep dependencies grouped as in the file: web, data, test. Each group has an Italian XML comment; add a comment for a new group.
- Scopes: H2 is `runtime`; test starters are `test`.
- The database is in-memory H2 and Thymeleaf renders the FE; don't add an external DB driver, front-end framework or build plugin unless explicitly requested.
- Build with the wrapper (`.\mvnw.cmd`), not a system Maven. After changing dependencies, run `.\mvnw.cmd test`.
