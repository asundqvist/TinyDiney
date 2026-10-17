import test from 'node:test';
import assert from 'node:assert/strict';
import { paginate, filterBookingReferences } from './pagination.js';

test('six items per page without gaps; empty and shrinking lists clamp safely', () => {
  const rows = Array.from({ length: 13 }, (_, id) => ({ id }));
  assert.deepEqual([0, 1, 2].flatMap(page => paginate(rows, page).items.map(item => item.id)), rows.map(item => item.id));
  assert.equal(paginate(rows.slice(0, 12), 0).totalPages, 2);
  assert.equal(paginate(rows, 2).items.length, 1);
  assert.equal(paginate(rows.slice(0, 6), 2).page, 0);
  assert.equal(paginate(rows, -1).page, 0);
  assert.deepEqual(paginate([], 99), { items: [], page: 0, totalPages: 1, total: 0 });
});

test('reference search handles case, partial codes and pasted hyphens across all statuses', () => {
  const rows = [
    { id: 1, reference: 'TD-ABCDEFGH-23456789', status: 'CONFIRMED' },
    { id: 2, reference: 'TD-JKLMNPQR-98765432', status: 'CANCELLED' },
    { id: 3, status: 'REQUESTED' },
  ];
  assert.deepEqual(filterBookingReferences(rows, ' tdabcdefgh23456789 ').map(row => row.id), [1]);
  assert.deepEqual(filterBookingReferences(rows, 'jklm').map(row => row.id), [2]);
  assert.deepEqual(filterBookingReferences(rows, 'notfound'), []);
  assert.equal(filterBookingReferences(rows, '').length, 3);
});

test('booking workspace renders five cards and page controls; hidden pages still contribute to counts', async () => {
  const { createServer } = await import('vite');
  const { default: React } = await import('react');
  const { renderToString } = await import('react-dom/server');
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
  try {
    const { default: StaffBookings } = await server.ssrLoadModule('/frontend/StaffBookings.jsx');
    const { default: Pagination } = await server.ssrLoadModule('/frontend/Pagination.jsx');
    const items = Array.from({ length: 12 }, (_, i) => ({ id: i + 1, status: 'REQUESTED', reference: 'TD-' + (i + 1), customerName: 'Guest ' + (i + 1), restaurantId: 1, customerEmail: 'guest@example.com', time: '2099-06-01T17:00:00', guests: 2 }));
    const html = renderToString(React.createElement(StaffBookings, { restaurants: [], pending: { items, loading: false, error: '', refresh() {}, replaceBooking() {} } }));
    assert.equal((html.match(/<article/g) || []).length, 5);
    assert.match(html, /Guest 5/);
    assert.doesNotMatch(html, /Guest 6/);
    assert.match(html.replaceAll('<!-- -->', ''), /Page 1 of 3/);
    assert.match(html.replaceAll('<!-- -->', ''), /12 pending bookings/);
    assert.match(html, /Search by booking reference/);
    const last = renderToString(React.createElement(Pagination, { page: 1, totalPages: 2, total: 12, label: 'Restaurant pages' }));
    assert.match(last, /Restaurant pages/);
    assert.match(last, /disabled="">Next/);
    assert.equal(renderToString(React.createElement(Pagination, { total: 0 })), '');
  } finally { await server.close(); }
});
