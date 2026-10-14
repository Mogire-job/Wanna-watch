# Wanna Watch

A global movie and TV discovery app. Browse popular titles, search and filter by genre or format, and see which streaming services offer each title in your selected country.

## What it does

- Shows movies and series from TMDB's provider-filtered catalogue.
- Supports country-specific availability and provider filters, including Netflix, Prime Video, Apple TV, HBO/Max, and other services returned by TMDB.
- Displays title metadata, ratings, genres, official YouTube trailers, and regional provider options.
- Imports and refreshes the catalogue through an authenticated, manually triggered sync.
- Lets people create accounts with a verified email and sign in; Google sign-in is available when configured.
- Provides a responsive discovery page with reduced-motion support and system-standard typography.

Provider names and catalogue coverage vary by country and by the data currently available from TMDB. Availability links open the regional watch-options page; a listing does not mean the title is included in a subscription.

## Requirements

- Java 25 (LTS) and Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop with its Linux engine running, for the one-command local stack
- A TMDB API Read Access Token to import catalogue and provider data

## Run the full stack

Copy the example environment file, then add your TMDB access token and a private sync token:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set `WANNA_WATCH_TMDB_API_READ_ACCESS_TOKEN` and `WANNA_WATCH_SYNC_TOKEN`. Keep `.env` private; it is ignored by Git. The sample database credentials and sync token settings are for local development only.

## Accounts and sign-in

Email/password registration requires SMTP so the API can send a one-time verification link. Set `WANNA_WATCH_SMTP_HOST`, `WANNA_WATCH_SMTP_PORT`, `WANNA_WATCH_SMTP_USERNAME`, `WANNA_WATCH_SMTP_PASSWORD`, and the SMTP auth/TLS flags in the private `.env`. Passwords require at least 12 characters; unverified accounts cannot sign in. Verification links expire after 24 hours.

Google sign-in is optional. Create a Google OAuth web client, configure `WANNA_WATCH_GOOGLE_CLIENT_ID` and `WANNA_WATCH_GOOGLE_CLIENT_SECRET`, then set `WANNA_WATCH_GOOGLE_SIGN_IN_ENABLED=true`. Add `http://localhost:5174/login/oauth2/code/google` as an authorized redirect URI for local development. For a deployed site, set `WANNA_WATCH_FRONTEND_URL` to its public origin and register `<public-origin>/login/oauth2/code/google`. Google sign-in accepts only Google-verified email addresses.

Use HTTPS, a trusted SMTP provider, and strong database credentials in production. Do not commit OAuth, SMTP, TMDB, or sync secrets.

Start the database, API, and website:

```powershell
docker compose up --build -d
```

- Website: http://localhost:5174
- API health: http://localhost:8081/actuator/health
- PostgreSQL: `localhost:5433`

Flyway creates and updates the database schema when the API starts.

## Import streaming catalogue data

The sync imports popular movies and series discoverable through the streaming providers available in the selected country. It stores metadata, official YouTube trailers, and provider offer types. The default is one page for each provider group; increase `WANNA_WATCH_TMDB_SYNC_PAGES` in `.env` to import more results per provider before syncing.

Trigger a sync for a market using the private token from `.env`:

```powershell
$headers = @{ 'X-Sync-Token' = '<your WANNA_WATCH_SYNC_TOKEN>' }
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/admin/sync?country=US' -Headers $headers
```

Use any supported ISO 3166-1 alpha-2 country code, for example `GB`, `CA`, `AU`, `IN`, or `KE`. Repeat for other markets to populate their provider availability. Sync access is disabled when no `WANNA_WATCH_SYNC_TOKEN` is configured.

## Run the API and website separately

The API loads an optional `../.env` when launched from `backend/`; the website defaults to its dedicated port and API proxy.

```powershell
# Terminal 1: start only PostgreSQL, if Docker Desktop is running
docker compose up -d db

# Terminal 2
Set-Location backend
mvn spring-boot:run

# Terminal 3
Set-Location frontend
npm ci
npm run dev
```

The local API listens on port `8081`, the website on `5174`, and the Compose database is mapped to `5433`. These ports keep this project separate from other local apps. Set `VITE_API_PROXY_TARGET` only if you intentionally run the API on another port.

## API

- `GET /api/titles?q=&type=MOVIE&genre=&provider=&country=US&page=0&size=20` — popularity-ranked, paginated results; page size is capped.
- `GET /api/titles/{id}?country=US` — title detail and watch options for that country.
- `GET /api/genres` — all standard movie and series genre filters.
- `GET /api/providers?country=US` — streaming services currently available in that market's imported catalogue.
- `GET /api/countries` — supported country/region choices.
- `POST /api/admin/sync?country=US` — refresh catalogue and watch availability; requires the `X-Sync-Token` header.
- `POST /api/auth/register` — create an account and send an email verification link.
- `POST /api/auth/verify` — verify a one-time email token.
- `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` — session-based sign-in and account status. Mutating account requests require the CSRF token returned by `GET /api/auth/csrf`.
- `/oauth2/authorization/google` — optional Google sign-in; requires OAuth credentials.

## Tests and production build

```powershell
Set-Location backend
mvn test

Set-Location ../frontend
npm test
npm run build
```

TMDB attribution: “This product uses the TMDB API but is not endorsed or certified by TMDB.” Streaming provider availability is supplied by TMDB and can differ by region or change over time.
