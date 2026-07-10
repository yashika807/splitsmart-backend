# SplitSmart — Backend

Spring Boot API for [SplitSmart](https://github.com/yashika807/Splitsmart-frontend),
a group expense splitter. Handles expense persistence and proxies natural-language
expense parsing to Google's Gemini API so the API key never has to live in the
browser.

## Tech stack

Java 21, Spring Boot 3.5 (Web, Data JPA), PostgreSQL 18. Talks to Gemini
directly over `java.net.http.HttpClient` — no SDK dependency.

## Architecture

```mermaid
flowchart LR
    User[Browser] -->|HTTPS| FE["React + Vite\n(Netlify)"]
    FE -->|REST / JSON| BE["Spring Boot API\n(Java 21)"]
    BE --> DB[(PostgreSQL 18)]
    BE -->|HTTPS| Gemini["Gemini API\ngemini-2.5-flash-lite"]
```

## Getting started

1. Have PostgreSQL 18 running locally with a `splitsmart` database.
2. Copy the properties template and fill in your own values:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
3. Set the two required env vars (or set them in your IDE's run
   configuration — that's how this project was developed):
   ```bash
   export DB_PASSWORD=your_postgres_password
   export GEMINI_API_KEY=your_gemini_api_key
   ```
4. Run it:
   ```bash
   ./mvnw spring-boot:run
   ```

Starts on `http://localhost:8080`.

### Environment variables

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | Postgres password for the `postgres` user |
| `GEMINI_API_KEY` | API key for the Gemini API ([get one here](https://aistudio.google.com/apikey)) |

`application.properties` itself only contains `${DB_PASSWORD}` /
`${GEMINI_API_KEY}` placeholders — the real values never get committed.

## API reference

Base URL: `http://localhost:8080/api/expenses`

### `GET /api/expenses`

Returns all expenses.

```json
[
  { "id": 1, "name": "Rahul", "amount": 500.0, "context": "Trip" }
]
```

### `POST /api/expenses`

Creates an expense. `context` is optional and defaults to `"Trip"`.

```json
// Request
{ "name": "Rahul", "amount": 500, "context": "Family" }

// Response — 200, the created expense with its id
{ "id": 4, "name": "Rahul", "amount": 500.0, "context": "Family" }
```

### `DELETE /api/expenses/{id}`

Deletes an expense by id. No response body.

### `POST /api/expenses/parse`

Sends free text to Gemini and gets back structured expenses. Used by the
AI input box on the frontend.

```json
// Request
{ "text": "Rahul paid 500 for dinner, Mini spent 800 on the cab" }

// Response — 200
[
  { "name": "Rahul", "amount": 500.0 },
  { "name": "Mini", "amount": 800.0 }
]
```

Returns a 500 with an error body if Gemini's response doesn't parse as the
expected JSON shape (this does happen — Gemini occasionally wraps output in
markdown fences or misformats a number).

## What I learned

- **Keeping secrets server-side isn't automatic — it's a design decision.**
  The AI parsing feature originally called Gemini directly from the browser,
  which meant the API key had to ship in the frontend bundle. Moving it here,
  behind `POST /api/expenses/parse`, was the actual fix — not just hiding the
  key better, but making sure it's structurally impossible for the client to
  need it.
- **`spring.jpa.hibernate.ddl-auto=update` is a fast way to iterate on a
  schema in development** — adding the `context` field to `Expense` required
  no manual migration, Hibernate just added the column. Fine for a solo
  learning project; I wouldn't run this in a real production system, where
  I'd want versioned migrations (Flyway/Liquibase) instead.
- **LLM APIs need defensive parsing, not just a happy-path deserializer.**
  `GeminiService` strips markdown fences and checks for a `candidates` field
  before trusting the response — Gemini doesn't always return clean JSON on
  the first try.
- **`java.net.http.HttpClient` is enough for simple outbound API calls.**
  Didn't need a REST client library or SDK just to POST JSON to Gemini and
  read the response back.

## Roadmap

- [x] Expense CRUD
- [x] Gemini-backed natural language parsing endpoint
- [x] Trip/Family context field on expenses
- [x] Secrets kept out of source control
- [ ] Deployed anywhere — currently local-only, no Render/Railway/Fly.io setup
- [ ] Real test coverage — only the default Spring Boot smoke test exists
- [ ] Input validation (negative amounts, blank names, oversized text to `/parse`)
- [ ] CORS restricted to the actual frontend origin (currently `@CrossOrigin(origins = "*")`)
- [ ] Database migrations via Flyway instead of `ddl-auto=update`
- [ ] Auth — there's currently no concept of a user, every expense is global
