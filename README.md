# Rate My Manager - Backend (MVP)

Spring Boot (Java) REST API for the Rate My Manager MVP.

## Endpoints

- `POST /api/managers` – Add manager (JSON: `{ "name": "...", "company": "...", "post": "...", "role": "..." }`)
- `GET /api/managers?q=keyword` – Search managers by name/company
- `GET /api/managers/{id}` – Get manager + average rating + reviews
- `POST /api/reviews` – Add review (JSON: `{ "manager": { "id": 1 }, "rating": 5, "reviewText": "..." }`)

## Local Setup

1. Install Java 17+ and Maven.
2. Start PostgreSQL and create DB `ratemymanager`.
3. Update `src/main/resources/application.properties` with your DB credentials.
4. Run:

```bash
mvn spring-boot:run
```

Open: `http://localhost:8080/api/managers?q=test`

## Notes
- This is an MVP: no authentication, basic validation, trivial profanity check.
- CORS enabled for all origins to make frontend testing easy.
- Deployed later on Render with a free Postgres instance.
