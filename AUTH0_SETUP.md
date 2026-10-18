# TinyDiney authentication

Auth0 handles login. Spring Boot is an OAuth2 Resource Server that validates access-token signatures, issuer, audience and expiry. The frontend uses the Auth0 React SDK with Authorization Code Flow and PKCE.

## API and roles

Register an Auth0 API with identifier `https://restaurant-api`, RS256 signing and RBAC enabled. Define these permissions:

```text
restaurants:read
restaurants:write
bookings:read
bookings:write
bookings:manage
```

The audience is an API identifier, not the deployed backend URL. Changing it requires matching frontend and backend configuration.

| Role | Permissions |
| --- | --- |
| GUEST | `restaurants:read`, `bookings:read`, `bookings:write` |
| STAFF | GUEST permissions plus `bookings:manage` |
| ADMIN | All five permissions |

New users receive GUEST automatically through the Post Login Action below. Assign STAFF and ADMIN manually to the relevant users; these roles are never granted by the Action.

## Automatic GUEST assignment and role claim

The same Action assigns GUEST to users without any roles and adds the role claim to the access token. It also handles existing roleless accounts on their next login. Users who already have a role, including STAFF or ADMIN, retain their roles.

### Management API application

1. Under **Applications → Applications**, create a separate **Machine to Machine** application, for example `TinyDiney Role Assignment`.
2. Authorize it for the **Auth0 Management API**, with `read:roles` and `update:users` permissions. This is a different API from TinyDiney's `https://restaurant-api` audience.
3. Copy its domain, Client ID and Client Secret into the Action's Secrets as described below. The frontend continues to use its existing SPA client ID.
4. Under **User Management → Roles → GUEST**, copy the **Role ID** beginning with `rol_`. Confirm that the role has the three GUEST permissions listed above.

See [Auth0's Management API setup for Actions](https://support.auth0.com/center/s/article/using-the-management-api-in-actions) and [role assignment permissions](https://auth0.com/docs/manage-users/access-control/configure-core-rbac/rbac-users/assign-roles-to-users).

### Action configuration

Create a custom **Login / Post Login** Action, for example `TinyDiney roles`.

In the Action editor, add the **`auth0` dependency at version 5.x**. The SDK method below requires that version; installing a Maven or frontend dependency does not install it in the Auth0 Action.

Add these entries to the Action's **Secrets**:

| Secret | Value |
| --- | --- |
| `MANAGEMENT_DOMAIN` | Auth0 tenant domain without `https://` or a trailing slash |
| `MANAGEMENT_CLIENT_ID` | Client ID of the separate M2M application |
| `MANAGEMENT_CLIENT_SECRET` | Client Secret of that M2M application |
| `GUEST_ROLE_ID` | GUEST's actual `rol_...` ID, not the role name |

Store the M2M credentials only in the Action's Secrets. They are not needed in frontend variables, the repository or the API's JWT-validation configuration.

Use this Action code:

```javascript
const { ManagementClient } = require('auth0');

exports.onExecutePostLogin = async (event, api) => {
  const audience = 'https://restaurant-api';
  const rolesClaim = 'https://restaurant-api/roles';

  if (event.resource_server?.identifier !== audience) {
    return;
  }

  let roles = event.authorization?.roles ?? [];

  if (roles.length === 0) {
    const management = new ManagementClient({
      domain: event.secrets.MANAGEMENT_DOMAIN,
      clientId: event.secrets.MANAGEMENT_CLIENT_ID,
      clientSecret: event.secrets.MANAGEMENT_CLIENT_SECRET,
    });

    try {
      await management.users.roles.assign(
        event.user.user_id,
        { roles: [event.secrets.GUEST_ROLE_ID] }
      );
    } catch (error) {
      console.log('Could not assign GUEST role:', error.message);
      api.access.deny('Could not configure your account. Please try again.');
      return;
    }

    roles = ['GUEST'];

    // Include requested guest scopes in the first login's token.
    const requestedScopes = event.transaction?.requested_scopes ?? [];
    const guestScopes = [
      'restaurants:read',
      'bookings:read',
      'bookings:write',
    ];

    for (const scope of guestScopes) {
      if (requestedScopes.includes(scope)) {
        api.accessToken.addScope(scope);
      }
    }
  }

  api.accessToken.setCustomClaim(rolesClaim, roles);
};
```

Match the audience, claim name and role name to the API configuration. If GUEST permissions change, update both the role and the first-login `guestScopes` list. The Action explicitly adds only requested guest scopes after successfully assigning the role; subsequent logins use the persisted role and normal RBAC scope calculation. See [Auth0's token modification API](https://auth0.com/docs/actions/reference/post-login/post-login-api-object).

### Deploy and verify

1. Click **Deploy** in the Action editor.
2. Open the Login/Post Login flow under **Actions → Triggers**, or **Flows → Login**, depending on the dashboard layout.
3. Replace the previous Action that only copied roles into the token with this combined Action. Do not leave another Action afterward that overwrites the same role claim from the original event.
4. Save/apply the flow. No Java or React deployment is needed for this Auth0 change.
5. Register a new account in TinyDiney. Check **User Management → Users → the user → Roles** for GUEST, then verify that the user can request a booking immediately.
6. Check that existing STAFF and ADMIN users still have their original access. Sign out and in after changing their roles or permissions.

The running tenant's successful signup and booking flow was manually verified during project setup. Local backend tests do not execute this hosted Action.

## SPA configuration

Use a **Single Page Application**, not a Machine to Machine client. Locally, register `http://localhost:5173` in Allowed Callback URLs, Allowed Logout URLs and Allowed Web Origins. For deployment, also register the actual HTTPS frontend origin.

The application sends `window.location.origin` for callback and logout, without a trailing slash or extra callback path.

The frontend requests:

```text
openid profile email restaurants:read restaurants:write bookings:read bookings:write bookings:manage
```

Allow user-delegated access to these API scopes for the SPA. With RBAC enabled, granted scopes are limited by the user's assigned permissions.

The API requires both the correct scope and role. A `permissions` claim alone does not replace `scope`.

## Environment

| Backend variable | Value |
| --- | --- |
| `AUTH0_ISSUER_URI` | `https://YOUR_TENANT.eu.auth0.com/`, including trailing slash |
| `AUTH0_AUDIENCE` | `https://restaurant-api` |
| `AUTH0_ROLES_CLAIM` | `https://restaurant-api/roles` |
| `FRONTEND_ORIGIN` | Frontend origin without trailing slash |

| Frontend variable | Value |
| --- | --- |
| `VITE_AUTH0_DOMAIN` | Tenant domain without `https://` or trailing slash |
| `VITE_AUTH0_CLIENT_ID` | SPA public client ID |
| `VITE_AUTH0_AUDIENCE` | Same identifier as backend |
| `VITE_AUTH0_ROLES_CLAIM` | Same claim as backend |

The example configuration contains a development tenant domain. Configure your own tenant for a separate installation. JWT validation needs no client secret. Frontend variables are public build values.

## Endpoint authorization

| Method and endpoint | Roles | Scope |
| --- | --- | --- |
| GET `/restaurants`, `/restaurants/{id}` | GUEST, STAFF, ADMIN | `restaurants:read` |
| POST `/restaurants`, PUT `/restaurants/{id}` | ADMIN | `restaurants:write` |
| GET `/bookings`, `/bookings/{id}`, `/bookings/mine` | GUEST, STAFF, ADMIN | `bookings:read` |
| POST `/bookings`, `/bookings/{id}/cancel` | GUEST, STAFF, ADMIN | `bookings:write` |
| POST `/bookings/{id}/confirm`, `/bookings/{id}/seat` | STAFF, ADMIN | `bookings:manage` |

Guests read and cancel only their own bookings; other users' booking IDs return 404. The mine endpoint always filters by the authenticated subject, including for staff/admin.

Guest cancellation is allowed for REQUESTED/CONFIRMED until one hour before the reservation. Staff/admin manage all restaurants and are not subject to the guest deadline.

| Status | Meaning |
| --- | --- |
| 401 | Missing/invalid token, signature, issuer, audience or expiry |
| 403 | Insufficient role or scope |
| 404 | Unknown resource or another guest's booking |
| 409 | Illegal transition, elapsed cancellation deadline or concurrent update |

Health and documentation are public. Swagger UI has an **Authorize** control: enter an API access token without the Bearer prefix. See [README_TinyDiney.md](README_TinyDiney.md#demonstrate-the-api) for HTTP Client tokens. ID tokens and client-credentials tokens do not provide the intended user access-token setup.

## CORS, CSRF and sessions

CORS accepts the configured frontend origin only, with credentials disabled. Authorization, Content-Type and If-None-Match request headers are allowed. Location and ETag are exposed.

The API uses explicit Bearer headers and stateless sessions; CSRF is disabled with a comment in SecurityConfig. CORS is a browser policy, not authentication.

Token caching uses the SDK's in-memory default. Closing a tab does not necessarily end Auth0's separate login session; use Sign out.

## Troubleshooting and tests

- Callback mismatch: check the exact frontend redirect origin.
- Unsupported authorization-code grant: confirm the client is a SPA with Authorization Code enabled.
- 403 after login: check scope, assigned role and custom role claim, then sign out and in.
- CORS rejection: compare the origin's scheme, host and port.
- Frontend configuration change: restart Vite or rebuild the deployed image.
- Signup/login fails before GUEST appears: check **Monitoring → Logs** and the Action output for `Could not assign GUEST role`. Verify the `auth0` 5.x dependency, exact Secret names, M2M Management API authorization and the `rol_...` role ID. If assignment fails, this Action intentionally denies login rather than issuing an incompletely configured account token.

Tests use local RSA keys and H2, covering JWT validation, ownership and all three roles across 11 protected endpoint variants. They do not verify the live tenant.

References: [Auth0 React SDK](https://auth0.com/docs/libraries/auth0-react), [Actions with authorization](https://auth0.com/docs/manage-users/access-control/sample-use-cases-actions-with-authorization), [Spring Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
