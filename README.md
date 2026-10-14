# Wanna Watch

Trailer-first movie discovery for Kenya. See the business and technical plan in the project doc.

## Run locally

Needs Java 21, Maven, Node 20+, Docker.

```bash
docker compose up -d            # PostgreSQL on :5432
cd backend && mvn spring-boot:run   # API on :8080, runs Flyway migrations
cd frontend && npm install && npm run dev   # site on :5173, proxies /api to :8080
```

Backend tests: `cd backend && mvn test`

## Layout

- `backend/` Spring Boot 3 API (`/api/titles`, `/api/titles/{id}`), Flyway schema in `db/migration`
- `frontend/` React + TypeScript (Vite): search box and trailer feed
- `docker-compose.yml` local PostgreSQL

## Status

Step 1: skeleton, schema, titles API, feed page. The catalogue is empty until Step 2 (data sync).
