# TinyDiney

TinyDiney is a restaurant booking platform for families with children. It brings together fictional restaurants with child-friendly signature dishes, from pancakes and meatballs to fish and chips.

The English-language React frontend has a dark theme with pastel purple accents. A secured Spring Boot REST API handles restaurants and reservations.

## Features

- Browse 12 demo restaurants with search, city and food-category filters, six cards per page and an automatic slideshow.
- Sign in through Auth0 and request a table in 15-minute intervals between 15:00 and 20:00.
- View personal bookings and staff updates, and cancel pending or confirmed reservations until one hour beforehand.
- Manage bookings as STAFF or ADMIN, with status filters, reference search, five records per page and guest-cancellation notifications.
- Add and edit restaurants as ADMIN.
- Identify reservations by a randomly generated booking reference.

Restaurant names, dishes and prices are fictional. Reservations are for seats, not food orders. Confirmation notifications are simulated through logging; no email is sent.

## Architecture and stack

```text
React / Vite frontend
    |-- Auth0: user login and access tokens
    |
    | HTTPS + Bearer access token
    v
Spring Boot API
    |-- JWT, scopes, roles and ownership checks
    |-- Booking lifecycle and simulated notification service
    v
MySQL database
```

| Component | Technology |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1, Spring MVC |
| Persistence | Spring Data JPA, Hibernate, MySQL 8.4 locally |
| Authentication | Auth0, OAuth2 Resource Server, JWT |
| API | DTOs, HATEOAS, ETag, springdoc OpenAPI and Swagger UI |
| Backend tests | JUnit, RestAssured, Mockito, Spring Security Test, H2 |
| Frontend | React 19, Vite 7, Auth0 React SDK, local SVG illustrations |
| Deployment | Docker, Caddy, Railway |

## Run locally

Requirements: JDK 25, Maven 3.9+, Node.js 22.12+ or Node.js 24, and Docker with Compose for MySQL.

1. Copy the configuration **only if no local `.env` already exists**:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Set different local database passwords and your Auth0 SPA client ID in `.env`. Follow [AUTH0_SETUP.md](AUTH0_SETUP.md) to configure roles, scopes and the login Action.
3. Start MySQL:

   ```powershell
   docker compose up -d --wait --wait-timeout 180
   ```

4. Start the backend from the repository root:

   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

5. Start the frontend in another terminal:

   ```powershell
   npm.cmd ci
   npm.cmd run dev
   ```

Open [the frontend](http://localhost:5173), [Swagger UI](http://localhost:8080/swagger-ui/index.html) or [health](http://localhost:8080/health). On macOS/Linux, use `npm` instead of `npm.cmd`.

The `mysql` profile imports `.env` from the working directory. Without that profile or database overrides, the API uses in-memory H2 and does not seed demo restaurants. Browsing the public frontend demo does not require login; booking requires a working API and Auth0 configuration.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/health` | Application availability |
| GET | `/restaurants`, `/restaurants/{id}` | Paginated list / details |
| POST / PUT | `/restaurants` / `/restaurants/{id}` | Create / update a restaurant |
| GET | `/bookings` | Paginated bookings; guests see only their own |
| GET | `/bookings/mine` | Own bookings, cancellation eligibility and staff updates |
| GET | `/bookings/{id}` | Booking, ETag and transition links |
| POST | `/bookings` | Request a reservation |
| POST | `/bookings/{id}/confirm` | Confirm a request |
| POST | `/bookings/{id}/seat` | Mark a confirmed reservation as seated |
| POST | `/bookings/{id}/cancel` | Cancel a pending or confirmed reservation |

All restaurant and booking endpoints require an access token with both a permitted role and the required scope. Health, Swagger UI and `/v3/api-docs` are public.

```text
REQUESTED --> CONFIRMED --> SEATED
     |             |
     +-------------+--> CANCELLED
```

Creation returns 201 with a Location header. Unknown or inaccessible booking IDs return 404; illegal transitions return 409. A matching If-None-Match on a booking GET returns 304. Optimistic locking rejects concurrent conflicting updates.

## Demonstrate the API

[requests.http](requests.http) contains ordered IntelliJ HTTP Client requests with response assertions for the lifecycle, pagination, ETag, authorization, validation and CORS.

1. Select `local` in the HTTP Client environment selector. Public values are in [http-client.env.json](http-client.env.json).
2. Create `http-client.private.env.json` locally:

   ```json
   {
     "local": {
       "guestToken": "REPLACE_WITH_GUEST_ACCESS_TOKEN",
       "staffToken": "REPLACE_WITH_STAFF_ACCESS_TOKEN",
       "adminToken": "REPLACE_WITH_ADMIN_ACCESS_TOKEN"
     }
   }
   ```

3. Obtain fresh access tokens from three different user accounts, one per role, by signing in and inspecting the Bearer header on an API request in the browser's network tools. Use this API's access tokens, not ID tokens.
4. Run the numbered requests in order, or run the file. Response handlers retain newly created IDs and the ETag for subsequent requests.

The private file is excluded from Git and Docker build contexts. Do not put tokens in tracked files or publish HTTP Client histories.

The sequence creates one demonstration restaurant and three bookings. Run locally unless you intend to add this data to a deployed database. Re-running creates another set; existing records are not deleted.

For Railway, replace the placeholder backend host in the `railway` environment, confirm `frontendOrigin`, and add a matching `railway` entry with fresh tokens to the private file. The host must point to the API service, not the frontend.

## Tests and build

```powershell
mvn test
npm.cmd test
npm.cmd run build
```

Backend tests use isolated H2 databases and locally signed JWTs, without Auth0 or MySQL. The authorization matrix covers all three roles across 11 protected endpoint variants. Other tests cover ownership, JWT validation, cancellation deadlines, concurrency, pagination and the lifecycle.

Frontend tests cover shared logic and selected rendered components, not a full browser end-to-end journey. The production build is written to `dist/`.

## Documentation

- [Frontend](FRONTEND.md)
- [Authentication and authorization](AUTH0_SETUP.md)
- [Database](DATABASE.md)
- [Railway deployment](DEPLOYMENT.md)
- [API demonstration requests](requests.http)

## Current scope

- Staff manage bookings across all restaurants; accounts are not assigned to individual restaurants.
- Booking controls are hidden for STAFF/ADMIN in the frontend. The API still permits creation if they have `bookings:write`.
- The seats field is stored, but capacity and overlapping reservations are not calculated.
- The frontend loads all API pages for filtering, search and counts. Guest account lists have no visual pagination.
- Restaurant times are stored without an offset and interpreted as Swedish local time. Staff display and booking-form date checks also depend on the browser's local timezone.
- Notification read state is per account and browser, rather than synchronized across devices.
- Schema management uses Hibernate update. Health checks application responsiveness, not database readiness.

The original course README and assignment files are retained separately.
