# TinyDiney database

Local development uses MySQL 8.4 through Docker Compose. The API runs separately through Maven or IntelliJ. Tests use isolated H2 databases.

## Configure and start

Copy `.env.example` to `.env` only if no local file exists. Set different passwords for the application and root:

```text
MYSQL_PORT=3306
DB_USERNAME=tinydiny
DB_PASSWORD=replace-with-your-local-password
MYSQL_ROOT_PASSWORD=replace-with-a-different-root-password
DEMO_DATA_ENABLED=true
```

The legacy local username and Compose project name remain `tinydiny`; the application is branded TinyDiney.

With Docker running Linux containers:

```powershell
docker compose up -d --wait --wait-timeout 180
docker compose ps
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

In IntelliJ, set the active profile to `mysql` and the working directory to the repository root. This profile imports `.env` as properties, so use plain `KEY=value` entries without surrounding quotes.

## Connection settings

| Setting | Local value |
| --- | --- |
| Host | `127.0.0.1` |
| Port | `MYSQL_PORT`, normally `3306` |
| Database | `restaurant` |
| User/password | `DB_USERNAME` / `DB_PASSWORD` |

The port is bound to localhost only. Change `MYSQL_PORT` if 3306 is occupied.

The `mysql` profile accepts a full `DB_URL` override. Otherwise it builds the JDBC address from `DB_HOST`, `DB_PORT` (falling back to `MYSQL_PORT`), `DB_NAME`, `DB_SSL_MODE` and `DB_ALLOW_PUBLIC_KEY_RETRIEVAL`. Defaults are intended for local development, with TLS disabled and connection timezone Europe/Stockholm.

The `prod` profile requires explicit database and Auth0 settings; see [DEPLOYMENT.md](DEPLOYMENT.md). Without a database profile or overrides, the API uses in-memory H2, which loses its contents when the process ends.

## Model and persistence

- `restaurant`: name, city, cuisine, seats and an optional unique demo key.
- `bookings`: restaurant association, contact information, time, party size, status, owner subject, reference, notification markers and revision/version fields.

The owner comes from the validated JWT subject, not the entered email. Legacy bookings without owners remain visible to staff and are not automatically assigned to accounts.

References are random and unique. Startup initialization backfills missing references. The JPA version field protects concurrent changes; the separate revision contributes to ETag.

Hibernate manages the schema through `ddl-auto: update`. There are no versioned SQL migrations.

## Demo data

The shared catalogue in `src/main/resources/demo/restaurants.json` defines 12 fictional restaurants:

| Restaurant | City | Signature dish |
| --- | --- | --- |
| Maple & Mini | Stockholm | Berry Pancakes |
| Mamma Mia's Meatballs | Stockholm | Meatballs & Macaroni |
| Peas & Please | Uppsala | Fish & Chips with Peas |
| Pocket Pizza Palace | Gothenburg | Mini Margherita |
| Puddington | Uppsala | Black Pudding & Lingonberries |
| Little Burger Barn | Gothenburg | Mini Cheeseburger |
| Noodle Nest | Stockholm | Tiny Tomato Pasta |
| The Happy Hen | Uppsala | Golden Chicken Bites |
| Little Green Spoon | Gothenburg | Creamy Pea Pasta |
| Sunny Side Minis | Stockholm | Mini Berry Stack |
| Little Slice House | Uppsala | Little Cheese Pizza |
| Tiny Bun Club | Gothenburg | Little Classic Burger |

Initialization runs with `mysql` or `prod` and `DEMO_DATA_ENABLED=true`, excluding tests. Empty databases receive all 12 restaurants. Existing databases can receive the six version-2 additions. Stable demo keys prevent duplicates and retain presentation after admin edits. Known legacy names are migrated without changing restaurant IDs or bookings.

Setting `DEMO_DATA_ENABLED=false` stops initialization without removing records. Dishes, prices and descriptions are local presentation data rather than menu records stored in MySQL.

## Stop, restart and troubleshoot

```powershell
docker compose stop
docker compose up -d --wait --wait-timeout 180
docker compose logs --tail 80 mysql
```

The named volume retains data across restarts. `docker compose down` retains that volume; adding `-v` deletes it and its data. Use the latter only when deliberately resetting a disposable local database.

Changing `.env` passwords does not update accounts in an already initialized volume. H2 data is not migrated automatically to MySQL.

The API's health endpoint checks application responsiveness, not database readiness.
