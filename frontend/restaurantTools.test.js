import test from 'node:test';
import assert from 'node:assert/strict';
import { restaurantPayload, saveRestaurant } from './restaurantTools.js';
import { canManageBookings, canManageRestaurants, requestedScopes } from './auth.js';

test('admins manage restaurants separately from staff and guest bookings', () => {
  assert.equal(canManageRestaurants(['ADMIN']), true);
  assert.equal(canManageBookings(['ADMIN']), true);
  assert.equal(canManageBookings(['ADMIN', 'STAFF']), true);
  assert.equal(canManageRestaurants(['STAFF']), false);
  assert.equal(canManageRestaurants(['GUEST']), false);
  assert.ok(requestedScopes.split(' ').includes('restaurants:write'));
});

test('restaurant creation and editing use validated API fields and preserve IDs', async () => {
  const calls = [];
  const request = async (...args) => { calls.push(args); return { id: 7 }; };
  const values = { name: ' New Kitchen ', city: ' Stockholm ', cuisine: ' Family food ', seats: '40', id: 999 };
  const payload = { name: 'New Kitchen', city: 'Stockholm', cuisine: 'Family food', seats: 40 };
  assert.deepEqual(restaurantPayload(values), payload);
  await saveRestaurant(request, null, values);
  await saveRestaurant(request, { id: 7 }, values);
  assert.deepEqual(calls, [['/restaurants', { method: 'POST', body: JSON.stringify(payload) }], ['/restaurants/7', { method: 'PUT', body: JSON.stringify(payload) }]]);
  for (const seats of ['', 0, -1, 1.5, 'invalid', 2147483648]) {
    await assert.rejects(saveRestaurant(request, null, { ...values, seats }));
  }
  await assert.rejects(saveRestaurant(request, null, { ...values, name: '  ' }));
  const denied = Object.assign(new Error('Missing restaurants:write'), { status: 403 });
  await assert.rejects(saveRestaurant(async () => { throw denied; }, null, values), error => error === denied);
  assert.equal(calls.length, 2);
});

test('admin editor uses actual API values and account shows only restaurant management', async () => {
  const { createServer } = await import('vite');
  const { default: React } = await import('react');
  const { renderToString } = await import('react-dom/server');
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
  try {
    const { default: AdminRestaurants, RestaurantEditor } = await server.ssrLoadModule('/frontend/AdminRestaurants.jsx');
    const { default: AccountPanel } = await server.ssrLoadModule('/frontend/AccountPanel.jsx');
    const { loadRestaurantRecords } = await server.ssrLoadModule('/frontend/catalogue.js');
    const restaurant = { id: 7, name: 'New name', city: 'Stockholm', cuisine: 'Updated cuisine', seats: 45 };
    assert.deepEqual(await loadRestaurantRecords(async () => ({ content: [restaurant], totalPages: 1 })), [restaurant]);
    const html = renderToString(React.createElement(AdminRestaurants, { restaurants: [restaurant], loading: false }));
    assert.match(html, /Add restaurant|Edit restaurant/);
    assert.match(html, /Updated cuisine/);
    assert.match(html.replaceAll('<!-- -->', ''), /45 seats/);
    const editor = renderToString(React.createElement(RestaurantEditor, { restaurant }));
    for (const field of ['New name', 'Stockholm', 'Updated cuisine', '45', 'Save changes']) assert.ok(editor.includes(field));
    const account = renderToString(React.createElement(AccountPanel, { isAdmin: true, isStaff: false, user: { email: 'admin@example.com' } }));
    assert.match(account, /Manage restaurants/);
    assert.match(account, /Manage Bookings/);
    assert.match(account, /Sign out/);
    assert.doesNotMatch(account, /My bookings|Book a table/);
    const failed = renderToString(React.createElement(AdminRestaurants, { restaurants: [], error: 'Missing restaurants:write' }));
    assert.match(failed, /Missing restaurants:write/);
    assert.doesNotMatch(failed, /No restaurants yet/);
  } finally { await server.close(); }
});
