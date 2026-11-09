# IndoorNavSystems

Indoor navigation and path optimization platform. See `docs/` for the product, architecture, database and API documents, and `docs/AGENTS.md` for the working rules.

## Backend

Requires Java 21, Maven 3.9 or later, and Docker for the PostgreSQL-backed persistence tests.

Run the tests from the repository root:

```bash
mvn test
```

`DatabaseIntegrityTest` starts a clean PostgreSQL 17 container, applies Flyway migrations and
the repeatable demo seeds, then validates the schema and data constraints. Docker must be
running for that test; the remaining unit and web tests do not require a database.

To start the backend you need PostgreSQL and the migrations in `database/migrations`. Copy `.env.example` to `.env`, fill in the values and export them into your shell, then run:

```bash
mvn -pl backend spring-boot:run
```

The `dev` profile is the default. It serves Swagger UI at `/swagger-ui.html`. Administrator sign-in stays disabled until `ADMIN_PASSWORD_HASH` is set.
