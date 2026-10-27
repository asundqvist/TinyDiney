# TinyDiney frontend

An English-language React application with a permanent dark theme and pastel purple accents. Vite runs separately from the Spring Boot API.

## Local setup

Install Node.js 22.12+ or Node.js 24, configure `.env` using `.env.example`, then run:

```powershell
npm.cmd ci
npm.cmd run dev
```

Open http://localhost:5173. The server uses a strict port to keep Auth0 redirects and CORS consistent. On macOS/Linux, use `npm` instead of `npm.cmd`.

| Variable | Purpose |
| --- | --- |
| `VITE_API_URL` | API origin; locally `http://localhost:8080` |
| `VITE_AUTH0_DOMAIN` | Tenant domain without `https://` |
| `VITE_AUTH0_CLIENT_ID` | SPA public client ID |
| `VITE_AUTH0_AUDIENCE` | API identifier, normally `https://restaurant-api` |
| `VITE_AUTH0_ROLES_CLAIM` | Normally `https://restaurant-api/roles` |

Restart Vite after local configuration changes. Production variables are embedded during the build, so changes require a new frontend build and deployment. Never put client secrets or database passwords in frontend variables.

See [AUTH0_SETUP.md](AUTH0_SETUP.md) for authentication.

## Views

| View | Behaviour |
| --- | --- |
| Home | Introduction and restaurant slideshow; changes every five seconds while visible |
| Restaurants | Search and city/food filters; six cards per page with controls above and below |
| My account | Personal booking statuses, cancellation, notifications and Sign out at top and bottom |
| Manage Bookings | STAFF/ADMIN workspace; status/restaurant filters, reference search and five cards per page |
| Manage restaurants | ADMIN editor for name, city, cuisine and seats |

Public visitors browse the local demo catalogue. Signed-in users see API records. Stable demo keys link records to local illustrations and signature dishes, with legacy-name matching for older records. Custom restaurants use a generic presentation.

Signature dishes, prices and descriptions come from `src/main/resources/demo/restaurants.json`; the admin editor does not edit these presentation fields. Food illustrations are local SVG components. Google Fonts loads remotely with local font fallbacks.

## Bookings and updates

Guests request tables for 1–10 people in quarter-hour intervals between 15:00 and 20:00 inclusive. Backend validation enforces future dates and the same interval. The resulting reference is shown while the request awaits confirmation.

My account shows Pending, Confirmed, Cancelled and Completed bookings. Guests can cancel their own pending or confirmed bookings until one hour before the reservation. The API enforces ownership and deadlines independently of the interface.

Staff and admin do not see booking creation controls. Staff can confirm, decline, seat or cancel reservations. Admin can also edit restaurants.

Lists refresh every 15 seconds while visible and when focus returns. Manual refresh is available. Stale requests are discarded; automatic retries stop after authorization errors until manual refresh or a new authenticated context.

Guests receive notifications for staff updates. Staff receive alerts when guests cancel confirmed bookings; pending requests also contribute to the navigation count. Read state is stored per account in browser local storage and is not shared across devices. Notification storage does not include tokens or contact information.

## Main files

- `App.jsx`: navigation, dialogs, booking form and authenticated API requests.
- `AccountPanel.jsx`, `MyBookings.jsx`: guest account and bookings.
- `StaffBookings.jsx`, `AdminRestaurants.jsx`: management views.
- `useBookingFeed.js`: shared refresh and stale-response handling.
- `useBookingReadState.js`: per-account notification read state.
- `apiTools.js`: retrieval of every API page.
- `Pagination.jsx`, `usePagination.js`: shared visual pagination.
- `catalogue.js`, `FoodArt.jsx`: restaurant presentation and artwork.
- `main.jsx`: Auth0 provider and return from login.

Catalogue and staff pagination operate on loaded records. Guest lists have status filters but no visual pagination. Restaurant times assume Europe/Stockholm; staff display and booking-form checks also depend on the browser's local timezone.

## Verification

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run preview
```

Preview serves the production bundle on port 5173; stop the development server first. Tests cover shared logic and selected rendered components. Deployment is described in [DEPLOYMENT.md](DEPLOYMENT.md).
