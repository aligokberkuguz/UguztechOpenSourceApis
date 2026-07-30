# UguztechOpenSourceApis

A collection of small, dependency-light, open source helper APIs meant to be dropped into other projects. No Spring Boot — plain **POJOs** on top of [Javalin](https://javalin.io), a lightweight Java web framework. The goal is fast startup, small fat-jars, and zero magic.

Each API lives in its own set of Maven modules so it can be built, tested and deployed independently.

## Table of Contents

- [Available APIs](#available-apis)
- [Architecture](#architecture)
- [Data Storage](#data-storage)
- [Getting Started (local development)](#getting-started-local-development)
- [Environment Variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Deployment](#deployment)
- [API Reference — URL Shortener](#api-reference--url-shortener)
- [Error Responses](#error-responses)
- [Interactive Docs (Swagger UI)](#interactive-docs-swagger-ui)
- [CORS](#cors)
- [Running Tests](#running-tests)
- [Adding a New API to this Repo](#adding-a-new-api-to-this-repo)
- [Contributing](#contributing)
- [License](#license)

## Available APIs

| API | Modules | Status |
| --- | --- | --- |
| URL Shortener | `url-shortener-core`, `url-shortener-web` | ✅ Ready |

More helper APIs will be added as separate module groups over time.

## Architecture

The repo is a single **multi-module Maven project**. Each API is split into:

- **`<api>-core`** — pure business logic (models, services, storage). No web framework dependency, no I/O framework. Fully unit-testable in isolation.
- **`<api>-web`** — the HTTP layer for that API (Javalin controllers, DTOs, the `Main` entry point, `pom.xml` with the Shade plugin to build a runnable fat-jar).
- **`web-common`** — shared web-layer utilities used by every `*-web` module: CORS configuration, Jackson `ObjectMapper` factory (Java 8 time support), RFC 7807 error handling (`ErrorHandler`, `ProblemDetail`), env/config helpers.

```
UguztechOpenSourceApis/
├── pom.xml                  (parent POM — shared dependency management)
├── web-common/              (shared web utilities)
├── url-shortener-core/      (business logic + unit tests)
└── url-shortener-web/       (Javalin HTTP layer + integration tests + Main.java)
```

This separation means the `-core` module of any API can be reused as a plain Java library (e.g. embedded in another app) without pulling in Javalin at all.

## Data Storage

Short URLs are stored **in memory only** — there is no database and no file persistence:

- Storage is a `ConcurrentHashMap` under the hood (`InMemoryUrlStore`), safe for concurrent access from multiple request threads.
- All shortened URLs are lost when the application restarts.
- Entries past their `ttlMinutes` are actively purged by a background scheduler that runs once a minute, so memory doesn't grow unbounded from stale, expired entries.

This is intentional for a lightweight helper API. If you need durable storage, implement the `UrlStore` interface with a persistent backend (e.g. Redis, Postgres) and wire it into `Main.java` in place of `InMemoryUrlStore` — the rest of the application doesn't need to change.

## Getting Started (local development)

**Prerequisites:** JDK 21, Maven 3.9+.

Day-to-day development is **not** meant to happen inside Docker — it's meant to be as simple as running a `main` method from your IDE.

1. Clone the repo and open it in IntelliJ (or any IDE) as a Maven project.
2. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
3. Run `com.uguztech.urlshortener.Main` directly from your IDE (or `mvn -pl url-shortener-web exec:java`, if you prefer the CLI).
4. The API starts on `http://localhost:7070`.

The `.env` file is only a **local convenience** — it is git-ignored and never read inside Docker/production images. See [Environment Variables](#environment-variables) below for how this works across environments.

## Environment Variables

All configuration is read through environment variables (12-factor style). Locally, a `.env` file (loaded via [java-dotenv](https://github.com/cdimascio/java-dotenv)) is used as a fallback so you don't have to export variables manually. In Docker/production, real environment variables (or `docker-compose.yml`'s `environment:` block) are used instead — **the code never changes between environments.**

| Variable | Description | Default |
| --- | --- | --- |
| `BASE_URL` | Base URL prefixed to generated short codes (`shortUrl` field in responses). Set this to your public domain in production. | `http://localhost:7070` |
| `ALLOWED_ORIGINS` | Comma-separated list of origins allowed by CORS, or `*` for all. | `*` |

A ready-to-copy template is provided in [`.env.example`](.env.example).

## Running with Docker

Docker is intended for **pre-production verification** — a quick way to confirm the app behaves the same in a clean, containerized environment as it does in your IDE, and it's also how you'd deploy it in production. There is no separate "Docker dev mode": the same image is used for the final check locally and for shipping to production.

```bash
# builds the fat-jar inside a Maven+JDK image, then copies it into a slim JRE image
docker compose up --build
```

This will:
1. Build every module (`mvn clean package`) inside a `maven:3.9-eclipse-temurin-21` build stage.
2. Copy only the resulting `url-shortener-web.jar` into an `eclipse-temurin:21-jre-alpine` runtime stage (small final image, no build tools).
3. Expose the API on `http://localhost:7070`.

Environment variables for the container come from your root `.env` file automatically (docker-compose reads it for variable substitution), or you can override them:

```bash
BASE_URL=https://short.example.com ALLOWED_ORIGINS=https://example.com docker compose up --build
```

Stop the stack with:
```bash
docker compose down
```

## Deployment

Production runs behind [Caddy](https://caddyserver.com) (see the `caddy` service in `docker-compose.yml` and the root `Caddyfile`), fronted by Cloudflare in proxy mode. TLS is terminated by Caddy using **Cloudflare Origin Certificates** rather than Caddy's automatic Let's Encrypt HTTPS.

Origin Certificates must be generated manually from the Cloudflare dashboard (SSL/TLS → Origin Server) and placed **outside version control** — e.g. in a `certs/` directory at the repo root on the VPS (already git-ignored), mounted read-only into the `caddy` container via `./certs:/certs:ro`. Never commit certificate or key files to this repository.

## API Reference — URL Shortener

### `POST /api/v1/shorten`

Creates a short code for a URL, with an optional TTL.

```bash
curl -X POST http://localhost:7070/api/v1/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com", "ttlMinutes": 60}'
```

```json
{
  "code": "000001",
  "shortUrl": "http://localhost:7070/000001",
  "originalUrl": "https://github.com",
  "createdAt": "2026-07-27T14:03:32.949537Z",
  "expiresAt": "2026-07-27T15:03:32.949537Z"
}
```

Validation rules (returns `400 Bad Request` — see [Error Responses](#error-responses) for the response format):
- `url` must be present, non-blank, and an absolute `http`/`https` URL.
- `ttlMinutes`, if provided, must be a positive number.

### `GET /{code}`

Redirects (`302`) to the original URL. Returns `404 Not Found` (see [Error Responses](#error-responses)) if the code doesn't exist or has expired.

```bash
curl -i http://localhost:7070/000001
```

## Error Responses

All error responses (validation failures, not-found, unexpected server errors) follow the [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `application/problem+json` format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "url must not be blank",
  "instance": "/api/v1/shorten"
}
```

| Field | Description |
| --- | --- |
| `type` | A URI identifying the problem type. Currently always `about:blank` (no dedicated problem-type pages yet). |
| `title` | Short, human-readable summary of the HTTP status (e.g. `Bad Request`, `Not Found`). |
| `status` | The HTTP status code, duplicated in the body for convenience. |
| `detail` | Specific, request-level explanation (e.g. which validation rule failed). |
| `instance` | The request path that produced the error. |

This format is provided by the shared `ErrorHandler` utility in `web-common`, so every API in this repo — current and future — returns errors in the same shape automatically.

## Interactive Docs (Swagger UI)

Every `*-web` module exposes an OpenAPI-generated Swagger UI, so you (and anyone integrating with the API) can explore and try requests without writing any client code:

- Swagger UI: `http://localhost:7070/swagger`
- Raw OpenAPI spec: `http://localhost:7070/openapi`

This is generated at compile time from `@OpenApi` annotations on the controller methods — no manually maintained spec file to keep in sync.

## CORS

CORS is configured via `ALLOWED_ORIGINS` (see [Environment Variables](#environment-variables)). By default it allows all origins (`*`), which is convenient for trying the API out, but you should restrict it to your own domain(s) in production.

## Running Tests

```bash
mvn test
```

Runs all unit tests (business logic in `*-core` modules) and integration tests (real HTTP requests against the Javalin app via `javalin-testtools` in `*-web` modules).

## Adding a New API to this Repo

Each new helper API should follow the same pattern as `url-shortener-*`:

1. Create `<name>-core` — pure business logic, no web dependencies, thoroughly unit tested.
2. Create `<name>-web` — Javalin controllers + DTOs + `Main.java`, depending on `<name>-core` and `web-common`. Add the Shade plugin config (copy from `url-shortener-web/pom.xml`) so it produces a runnable fat-jar. In `Main.java`, call `ErrorHandler.register(app)` right after `app.start(...)` so errors follow the shared RFC 7807 format automatically.
3. Add `@OpenApi` annotations to controller methods for Swagger documentation.
4. Register the new modules in the root `pom.xml`'s `<modules>` section.
5. Document the new API's endpoints in this README.

## Contributing

Issues and pull requests are welcome. Please keep new APIs framework-light (POJO + Javalin) and make sure `mvn test` passes before submitting a PR.

## License

MIT — see [LICENSE](LICENSE) for the full text. Copyright (c) 2026 Uguztech.