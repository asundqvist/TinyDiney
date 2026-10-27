# TinyDiney deployment on Railway

Deploy separate frontend and backend services from this repository, plus a MySQL service. Both application builds use the repository root.

## Services

| Setting | Backend | Frontend |
| --- | --- | --- |
| `RAILWAY_DOCKERFILE_PATH` | `Dockerfile.backend` | `Dockerfile.frontend` |
| Root directory | Repository root | Repository root |
| Port | 8080 | 8080 |
| Health path | `/health` | `/` |
| Build/start commands | Dockerfile defaults | Dockerfile defaults |

Generate a public HTTPS domain for each application service. The frontend needs the API's public URL because its requests originate in the visitor's browser. MySQL can use a private service connection.

The backend image runs Java as a non-root user with the prod profile. Caddy serves the frontend bundle on PORT; Railway terminates HTTPS.

## Backend variables

```text
RAILWAY_DOCKERFILE_PATH=Dockerfile.backend
PORT=8080
SPRING_PROFILES_ACTIVE=prod
FORWARD_HEADERS_STRATEGY=framework
AUTH0_ISSUER_URI=https://YOUR_TENANT.eu.auth0.com/
AUTH0_AUDIENCE=https://restaurant-api
AUTH0_ROLES_CLAIM=https://restaurant-api/roles
FRONTEND_ORIGIN=https://YOUR_FRONTEND_DOMAIN
DEMO_DATA_ENABLED=true
DB_URL=jdbc:mysql://MYSQL_PRIVATE_HOST:3306/restaurant?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectionTimeZone=Europe/Stockholm
DB_USERNAME=MYSQL_APPLICATION_USER
DB_PASSWORD=MYSQL_APPLICATION_PASSWORD
```

Replace JDBC host, port, database and credentials with the MySQL service's actual values or Railway variable references. Use a JDBC URL beginning with `jdbc:mysql://`, not a plain `mysql://` URL. The example disables database TLS for a private connection; configure it according to your provider for other arrangements.

The prod profile reads environment variables rather than importing local `.env`. The frontend origin has no path or trailing slash. Forwarded headers let generated links reflect the external HTTPS origin.

Demo data can remain enabled for this fictional demonstration. Disabling it stops initialization without deleting records.

## Frontend build variables

```text
RAILWAY_DOCKERFILE_PATH=Dockerfile.frontend
PORT=8080
VITE_API_URL=https://YOUR_BACKEND_DOMAIN
VITE_AUTH0_DOMAIN=YOUR_TENANT.eu.auth0.com
VITE_AUTH0_CLIENT_ID=YOUR_SPA_PUBLIC_CLIENT_ID
VITE_AUTH0_AUDIENCE=https://restaurant-api
VITE_AUTH0_ROLES_CLAIM=https://restaurant-api/roles
```

VITE variables are Docker build arguments embedded in the bundle. Rebuild and redeploy after changes. They must not contain secrets.

Register the actual frontend origin for Auth0 callbacks, logout and web origins; see [AUTH0_SETUP.md](AUTH0_SETUP.md).

## Verification

1. Load the frontend and its JavaScript/styles.
2. Open the backend health, OpenAPI and Swagger endpoints.
3. Confirm that a protected endpoint without a token returns 401.
4. Test guest booking, staff handling and admin restaurant editing.
5. Verify that a guest cannot confirm or access another guest's booking.
6. Restart the application and confirm persistence.

For HTTP Client checks, configure the railway environment's actual backend host, frontend origin and private tokens. The mutation examples create demonstration records.

A Railway response of 404 Application not found calls for checking the service domain and active deployment. Health only proves application responsiveness, not database readiness or Auth0 configuration.

The backend Docker build skips test execution. Run Maven and frontend tests before deployment. Schema management uses Hibernate update; backup and restore settings are managed outside this repository.

Reference: [Railway Dockerfiles and build variables](https://docs.railway.com/builds/dockerfiles).
