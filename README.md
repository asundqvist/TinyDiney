# 🍽️ TinyDiney

**A family-friendly restaurant booking platform built with React and Spring Boot.**

TinyDiney lets families explore fictional, child-friendly restaurants and request table reservations. The application includes a React frontend, a secured REST API, role-based access control and a MySQL database.

## Features

- Browse 12 demo restaurants with search, filters and pagination.
- Sign in securely using Auth0.
- Request, view and cancel restaurant reservations.
- Manage bookings as **STAFF** or **ADMIN**.
- Add and edit restaurants as **ADMIN**.
- Track reservations using unique booking references.

*All restaurants, dishes and prices are fictional. Reservations are for tables, not food orders.*

## Tech stack

| Frontend | Backend | Database & security |
|---|---|---|
| React 19, Vite 7 | Java 25, Spring Boot 4.1 | MySQL 8.4, Auth0, JWT |

Additional technologies include Spring Data JPA, Hibernate, Swagger UI, JUnit, RestAssured, Docker, Caddy and Railway.

## Getting started

**Requirements:** JDK 25, Maven 3.9+, Node.js 22.12+ or 24, and Docker Compose.

1. Create a local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Configure database passwords and Auth0 settings in `.env`. See [Auth0 setup](docs/AUTH0_SETUP.md).

3. Start the database:

   ```powershell
   docker compose up -d --wait --wait-timeout 180
   ```

4. Start the backend:

   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

5. Start the frontend in another terminal:

   ```powershell
   npm.cmd ci
   npm.cmd run dev
   ```

**Local URLs:** [Frontend](http://localhost:5173) · [Swagger UI](http://localhost:8080/swagger-ui/index.html) · [Health](http://localhost:8080/health)

On macOS/Linux, use `npm` instead of `npm.cmd`. Configure Auth0 before testing authenticated bookings.

<details>
<summary><strong>API overview</strong></summary>

The API supports restaurant browsing and administration, reservation requests, booking status transitions and cancellations.

Reservation lifecycle:

`REQUESTED → CONFIRMED → SEATED`

Reservations can also be cancelled while eligible.

The API uses JWT authentication, role and scope checks, ETags and optimistic locking.

For detailed endpoints and example requests, see [API documentation and demonstration](docs/README_TinyDiney.md#api).

</details>

<details>
<summary><strong>Tests and build</strong></summary>

```powershell
mvn test
npm.cmd test
npm.cmd run build
```

Backend tests cover authorization, reservation ownership, validation, pagination, cancellation and concurrency. Frontend tests cover selected logic and components.

</details>

## Documentation

- [Full project documentation](docs/README_TinyDiney.md)
- [Frontend](docs/FRONTEND.md)
- [Authentication and authorization](docs/AUTH0_SETUP.md)
- [Database](docs/DATABASE.md)
- [Deployment](docs/DEPLOYMENT.md)
- [API demonstration](requests.http)

## Project scope

TinyDiney is a demonstration project. Restaurant availability is not capacity-checked, booking notifications are simulated, and some account preferences are stored locally in the browser.
