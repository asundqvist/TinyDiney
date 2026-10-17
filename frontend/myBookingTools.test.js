import test from 'node:test';
import assert from 'node:assert/strict';
import { canCancelMyBooking, loadMyBookings, unreadBookingUpdates, markBookingUpdatesRead } from './myBookingTools.js';

test('personal list loads every page from the owner-filtered endpoint', async () => {
  const paths = [];
  const bookings = await loadMyBookings(async path => {
    paths.push(path);
    return { content: [{ id: paths.length, time: paths.length === 1 ? '2026-10-20T17:00:00' : '2026-10-19T17:00:00', status: 'REQUESTED' }], totalPages: 2 };
  });
  assert.deepEqual(paths, ['/bookings/mine?size=100&page=0', '/bookings/mine?size=100&page=1']);
  assert.deepEqual(bookings.map(booking => booking.id), [2, 1]);
});

test('personal cancellation obeys the offset deadline including the exact one-hour boundary', () => {
  const booking = { status: 'REQUESTED', cancellable: true, cancellationDeadline: '2026-10-20T16:00:00+02:00' };
  const deadline = Date.parse('2026-10-20T14:00:00Z');
  assert.equal(canCancelMyBooking(booking, deadline - 1), true);
  assert.equal(canCancelMyBooking(booking, deadline), true);
  assert.equal(canCancelMyBooking(booking, deadline + 1), false);
  assert.equal(canCancelMyBooking({ ...booking, status: 'CONFIRMED' }, deadline), true);
  assert.equal(canCancelMyBooking({ ...booking, status: 'CANCELLED' }, deadline), false);
  assert.equal(canCancelMyBooking({ ...booking, status: 'SEATED' }, deadline), false);
  assert.equal(canCancelMyBooking({ ...booking, cancellable: false }, deadline - 1000), false);
  assert.equal(canCancelMyBooking({ ...booking, cancellationDeadline: null }, deadline), false);
});

test('guest notifications track staff updates, persist read revisions and ignore self-cancellations', () => {
  const bookings = [
    { id: 1, status: 'REQUESTED', staffUpdateRevision: null },
    { id: 2, status: 'CONFIRMED', staffUpdateRevision: 2 },
    { id: 3, status: 'CANCELLED', staffUpdateRevision: 2 },
    { id: 4, status: 'CANCELLED', staffUpdateRevision: null },
  ];
  assert.deepEqual(unreadBookingUpdates(bookings).map(item => item.id), [2, 3]);
  const seen = markBookingUpdatesRead(bookings);
  assert.deepEqual(unreadBookingUpdates(bookings, seen), []);
  assert.deepEqual(unreadBookingUpdates([{ ...bookings[1], status: 'CANCELLED' }], seen), []);
  assert.deepEqual(unreadBookingUpdates([{ ...bookings[1], status: 'SEATED', staffUpdateRevision: 3 }], seen).map(item => item.id), [2]);
  assert.deepEqual(markBookingUpdatesRead([], seen), seen);
});

test('public render has permanent dark styling and no theme switch; account includes personal bookings', async () => {
  const { createServer } = await import('vite');
  const { default: React } = await import('react');
  const { renderToString } = await import('react-dom/server');
  const { readFile } = await import('node:fs/promises');
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
  try {
    const { default: App } = await server.ssrLoadModule('/frontend/App.jsx');
    const html = renderToString(React.createElement(App, { authConfigured: false }));
    assert.doesNotMatch(html, /Dark mode|theme-toggle|How it works|HOW TinyDiney WORKS|how-section|Where shall we eat/);
    assert.match(html, /Let us help them pick!/);
    assert.match(html, /Featured restaurants/);
    const css = await readFile(new URL('./styles.css', import.meta.url), 'utf8');
    assert.match(css, /color-scheme:dark/);
    assert.doesNotMatch(css, /color-scheme:light|theme-toggle|data-theme/);
    const { default: MyBookings } = await server.ssrLoadModule('/frontend/MyBookings.jsx');
    const account = renderToString(React.createElement(MyBookings, { request() {}, restaurants: [], account: { items: [], unread: [], loading: true, error: '', markRead() {}, refresh() {}, replaceBooking() {} } }));
    for (const content of ['My bookings', 'Pending', 'Confirmed', 'Cancelled', 'one hour', 'Loading your bookings']) assert.ok(account.includes(content));
    const bookingProps = { request() {}, restaurants: [{ id: 1, name: 'Family Kitchen' }], account: {
      items: [{ id: 987654, reference: 'TD-ABCDEFGH-23456789', restaurantId: 1, status: 'REQUESTED', time: '2026-10-20T17:00:00', cancellationDeadline: '2026-10-20T16:00:00+02:00', guests: 2 }],
      unread: [], loading: false, error: '', markRead() {}, refresh() {}, replaceBooking() {},
    } };
    const guestBooking = renderToString(React.createElement(MyBookings, bookingProps));
    assert.match(guestBooking, /Booking reference:/);
    assert.match(guestBooking, /TD-ABCDEFGH-23456789/);
    assert.doesNotMatch(guestBooking, /987654/);
    const { default: AccountPanel } = await server.ssrLoadModule('/frontend/AccountPanel.jsx');
    const staffAccount = renderToString(React.createElement(AccountPanel, { ...bookingProps, isStaff: true, user: { email: 'staff@example.com' } }));
    assert.match(staffAccount, /staff@example.com|Manage bookings/);
    assert.match(staffAccount, /Sign out/);
    assert.doesNotMatch(staffAccount, /My bookings|Book a table|Refresh bookings|TD-ABCDEFGH-23456789/);
    const guestAccount = renderToString(React.createElement(AccountPanel, { ...bookingProps, isStaff: false }));
    assert.match(guestAccount, /My bookings/);
    assert.match(guestAccount, /Book a table/);
    assert.doesNotMatch(guestAccount, /Manage bookings/);
    const { default: RestaurantSlideshow, SLIDE_INTERVAL_MS } = await server.ssrLoadModule('/frontend/RestaurantSlideshow.jsx');
    assert.equal(SLIDE_INTERVAL_MS, 5000);
    const slide = renderToString(React.createElement(RestaurantSlideshow, { restaurants: [{ name: 'Restaurant One', city: 'Stockholm', cuisine: 'Family' }, { name: 'Restaurant Two', city: 'Uppsala' }], onExplore() {} }));
    assert.match(slide, /Restaurant One/);
    assert.doesNotMatch(slide, /Pause slideshow|Next restaurant|Previous restaurant|slide-controls/);
    const { default: BookingNotification } = await server.ssrLoadModule('/frontend/BookingNotification.jsx');
    assert.match(renderToString(React.createElement(BookingNotification, { count: 2 })), /booking-notification/);
    assert.equal(renderToString(React.createElement(BookingNotification, { count: 0 })), '');
    const favicon = await readFile(new URL('../public/favicon.svg', import.meta.url), 'utf8');
    assert.match(favicon, /#101013/);
    assert.match(favicon, /#c9b4ed/);
  } finally { await server.close(); }
});
