export const rolesClaim = 'https://restaurant-api/roles';
export const requestedScopes = 'openid profile email restaurants:read restaurants:write bookings:read bookings:write bookings:manage';

// These claims control navigation only. The API validates the JWT and authorizes every operation.
export function readAccessRoles(token, claimName = rolesClaim) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return [];
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64), character => character.charCodeAt(0));
    const payload = JSON.parse(new TextDecoder().decode(bytes));
    const roles = payload[claimName];
    return Array.isArray(roles) ? roles.filter(role => typeof role === 'string').map(role => role.toUpperCase()) : [];
  } catch { return []; }
}

export function canManageBookings(roles) {
  return roles.includes('STAFF') || roles.includes('ADMIN');
}

export function canManageRestaurants(roles) {
  return roles.includes('ADMIN');
}

export function requiredApiScope(path, method = 'GET') {
  const resource = path.split('?')[0];
  if (/^\/bookings(?:\/|$)/.test(resource)) {
    if (method === 'GET') return 'bookings:read';
    return /\/(confirm|seat)$/.test(resource) ? 'bookings:manage' : 'bookings:write';
  }
  if (/^\/restaurants(?:\/|$)/.test(resource)) return method === 'GET' ? 'restaurants:read' : 'restaurants:write';
  return null;
}

export function apiAccessError(status, path, method = 'GET', token = '') {
  let message = 'Your session needs to be renewed. Please sign out and sign in again.';
  if (status === 403) {
    const scope = requiredApiScope(path, method);
    let granted = [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (typeof payload.scope === 'string') granted = payload.scope.split(/\s+/);
    } catch { /* The server remains the authority for access decisions. */ }
    message = scope && !granted.includes(scope)
      ? 'Your current session is missing the ' + scope + ' permission. Ask an administrator to check your Auth0 role permissions and the application API access, then sign out and sign in again.'
      : 'Access denied by the API. Check your assigned role and role claim, then sign out and sign in again.';
  }
  return Object.assign(new Error(message), { status });
}
