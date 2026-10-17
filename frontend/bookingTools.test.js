import test from 'node:test';
import assert from 'node:assert/strict';
import { quarterHourTimes, isQuarterHour, localDate, loadStaffBookings, updateStaffBooking, unreadGuestCancellations, markGuestCancellationsRead } from './bookingTools.js';
import { readAccessRoles, canManageBookings, rolesClaim, apiAccessError } from './auth.js';

test('offers quarter-hour choices only between 15:00 and 20:00 inclusive', () => {
  assert.equal(quarterHourTimes.length, 21);
  assert.equal(new Set(quarterHourTimes).size, 21);
  assert.equal(quarterHourTimes[0], '15:00');
  assert.equal(quarterHourTimes.at(-1), '20:00');
  assert.deepEqual(quarterHourTimes.filter(time => time.startsWith('17:')), ['17:00', '17:15', '17:30', '17:45']);
  for (const time of quarterHourTimes) assert.ok(isQuarterHour(time));
  for (const time of ['00:00', '14:45', '20:15', '23:45', '17:05', '17:10', '17:59', '24:00', '', '17:15:30']) assert.equal(isQuarterHour(time), false);
  assert.equal(localDate(new Date(2026, 0, 2, 0, 5)), '2026-01-02');
});

const token = claims => 'header.' + Buffer.from(JSON.stringify(claims)).toString('base64url') + '.signature';

test('staff navigation reads access-token roles and stays hidden for guests and malformed claims', () => {
  for (const role of ['STAFF', 'ADMIN', 'staff']) {
    assert.ok(canManageBookings(readAccessRoles(token({ [rolesClaim]: [role], name: 'Test user' }))));
  }
  assert.equal(canManageBookings(readAccessRoles(token({ [rolesClaim]: ['GUEST'] }))), false);
  assert.equal(canManageBookings(['ADMIN']), true);
  assert.equal(canManageBookings(['ADMIN', 'STAFF']), true);
  assert.equal(canManageBookings(readAccessRoles(token({ permissions: ['bookings:manage'] }))), false);
  for (const claim of [null, 'STAFF', {}, [false, 42]]) assert.deepEqual(readAccessRoles(token({ [rolesClaim]: claim })), []);
  assert.deepEqual(readAccessRoles('not-a-jwt'), []);
  assert.deepEqual(readAccessRoles('header.invalid.signature'), []);
  assert.deepEqual(readAccessRoles(token({ customRoles: ['STAFF'] }), 'customRoles'), ['STAFF']);
});

test('loads every staff booking status across all pages in date order', async () => {
  const paths = [];
  const result = await loadStaffBookings(async path => {
    paths.push(path);
    return paths.length === 1
      ? { content: [{ id: 1, status: 'CONFIRMED', time: '2026-10-20T12:00:00' }, { id: 2, status: 'REQUESTED', time: '2026-10-20T17:15:00' }], totalPages: 2 }
      : { content: [{ id: 3, status: 'REQUESTED', time: '2026-10-19T18:00:00' }, { id: 4, status: 'CANCELLED', time: '2026-10-19T10:00:00' }], totalPages: 2 };
  });
  assert.deepEqual(result.map(booking => booking.id), [4, 3, 1, 2]);
  assert.deepEqual(paths, ['/bookings?size=100&page=0', '/bookings?size=100&page=1']);
});

test('confirms and declines the selected booking through the correct endpoints', async () => {
  const calls = [];
  const request = async (...args) => { calls.push(args); return { id: 57, status: args[0].endsWith('confirm') ? 'CONFIRMED' : 'CANCELLED' }; };
  const booking = { id: 57, status: 'REQUESTED' };
  assert.equal((await updateStaffBooking(request, booking, 'confirm')).status, 'CONFIRMED');
  assert.equal((await updateStaffBooking(request, booking, 'cancel')).status, 'CANCELLED');
  assert.deepEqual(calls, [['/bookings/57/confirm', { method: 'POST' }], ['/bookings/57/cancel', { method: 'POST' }]]);
  await assert.rejects(updateStaffBooking(request, booking, 'seat'));
  await assert.rejects(updateStaffBooking(request, { ...booking, status: 'SEATED' }, 'cancel'));
  assert.equal(calls.length, 2);
});

test('server conflicts propagate so the workspace can reload changed bookings', async () => {
  const conflict = Object.assign(new Error('Already changed'), { status: 409 });
  await assert.rejects(updateStaffBooking(async () => { throw conflict; }, { id: 57, status: 'REQUESTED' }, 'confirm'), error => error === conflict);
});

test('a forbidden booking list identifies the missing read scope without implying an empty list', () => {
  const readDenied = apiAccessError(403, '/bookings?size=100&page=0', 'GET', token({ scope: 'openid restaurants:read bookings:write' }));
  assert.equal(readDenied.status, 403);
  assert.match(readDenied.message, /missing the bookings:read permission/);
  assert.doesNotMatch(readDenied.message, /missing the bookings:manage/);
  const roleDenied = apiAccessError(403, '/bookings', 'GET', token({ scope: 'bookings:read' }));
  assert.match(roleDenied.message, /assigned role and role claim/);
  assert.match(apiAccessError(403, '/bookings/57/confirm', 'POST', token({ scope: 'bookings:read' })).message, /bookings:manage/);
  assert.equal(apiAccessError(401, '/bookings').status, 401);
});

test('staff workspace distinguishes a failed load from zero requests and displays the shared badge count', async () => {
  const { createServer } = await import('vite');
  const { default: React } = await import('react');
  const { renderToString } = await import('react-dom/server');
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
  try {
    const { default: StaffBookings, StaffBookingCard } = await server.ssrLoadModule('/frontend/StaffBookings.jsx');
    const { default: StaffBookingsLink } = await server.ssrLoadModule('/frontend/StaffBookingsLink.jsx');
    const pending = { items: [], loading: false, error: 'Missing bookings:read', refresh() {}, removeBooking() {} };
    const failed = renderToString(React.createElement(StaffBookings, { pending, restaurants: [], request() {} }));
    assert.match(failed, /Bookings unavailable/);
    assert.doesNotMatch(failed, /0 pending bookings|All caught up/);
    const empty = renderToString(React.createElement(StaffBookings, { pending: { ...pending, error: '' }, restaurants: [], request() {} }));
    assert.match(empty, /0 pending bookings/);
    assert.match(empty, /All caught up/);
    const badge = renderToString(React.createElement(StaffBookingsLink, { pending: { ...pending, error: '', items: [{ id: 1, status: 'REQUESTED' }, { id: 2, status: 'REQUESTED' }, { id: 3, status: 'CONFIRMED' }] } }));
    assert.match(badge, /2 pending requests/);
    assert.match(badge, /booking-notification/);
    const errorBadge = renderToString(React.createElement(StaffBookingsLink, { pending }));
    assert.match(errorBadge, /requests unavailable/);
    assert.doesNotMatch(errorBadge, /0 pending/);
    const cancelled = { id: 5, reference: 'TD-ABCDEFGH-23456789', customerName: 'Alex', restaurantId: 1, status: 'CANCELLED', guestCancelledAfterConfirmation: true, time: '2099-06-01T17:00:00', guests: 2 };
    const cancellationBadge = renderToString(React.createElement(StaffBookingsLink, { pending: { ...pending, error: '', items: [cancelled], unreadCancellations: [cancelled] } }));
    assert.match(cancellationBadge, /1 guest cancellations/);
    assert.match(cancellationBadge, /booking-notification/);
    const alert = renderToString(React.createElement(StaffBookings, { pending: { ...pending, error: '', items: [cancelled], unreadCancellations: [cancelled] }, restaurants: [], request() {} }));
    assert.match(alert, /Guest cancellations after confirmation/);
    assert.match(alert, /TD-ABCDEFGH-23456789/);
    assert.match(alert, /Mark cancellations as read/);
    assert.match(alert, /Confirmed/);
    assert.match(alert, /Seated/);
    const confirmedCard = renderToString(React.createElement(StaffBookingCard, { booking: { ...cancelled, status: 'CONFIRMED', guestCancelledAfterConfirmation: false }, restaurantName: 'Test Kitchen', busy: null, declining: null }));
    assert.match(confirmedCard, /Mark as seated/);
    assert.doesNotMatch(confirmedCard, /Confirm booking/);
    const cancelledCard = renderToString(React.createElement(StaffBookingCard, { booking: cancelled, restaurantName: 'Test Kitchen', busy: null, declining: null }));
    assert.match(cancelledCard, /Cancelled by the guest after confirmation/);
    assert.doesNotMatch(cancelledCard, /Mark as seated|Confirm booking|Cancel booking/);
    const clear = renderToString(React.createElement(StaffBookingsLink, { pending: { ...pending, error: '' } }));
    assert.doesNotMatch(clear, /booking-notification/);
  } finally { await server.close(); }
});

test('confirmed bookings can be seated or cancelled but cannot be confirmed twice', async () => {
  const calls = [];
  const request = async (...args) => { calls.push(args); return { id: 12, status: args[0].endsWith('/seat') ? 'SEATED' : 'CANCELLED' }; };
  const booking = { id: 12, status: 'CONFIRMED' };
  assert.equal((await updateStaffBooking(request, booking, 'seat')).status, 'SEATED');
  assert.equal((await updateStaffBooking(request, booking, 'cancel')).status, 'CANCELLED');
  await assert.rejects(updateStaffBooking(request, booking, 'confirm'));
  await assert.rejects(updateStaffBooking(request, { ...booking, status: 'CANCELLED' }, 'seat'));
  assert.deepEqual(calls, [['/bookings/12/seat', { method: 'POST' }], ['/bookings/12/cancel', { method: 'POST' }]]);
});

test('staff cancellation notifications persist read state and ignore pending cancellations or staff actions', () => {
  const items = [
    { id: 1, reference: 'TD-FIRST', status: 'CANCELLED', guestCancelledAfterConfirmation: true },
    { id: 2, status: 'CANCELLED', guestCancelledAfterConfirmation: false },
    { id: 3, status: 'CONFIRMED' },
    { id: 4, status: 'CANCELLED' },
  ];
  assert.deepEqual(unreadGuestCancellations(items).map(item => item.id), [1]);
  const seen = JSON.parse(JSON.stringify(markGuestCancellationsRead(items)));
  assert.deepEqual(unreadGuestCancellations(items, seen), []);
  assert.deepEqual(markGuestCancellationsRead([], seen), seen);
  // Reusing a numeric database ID after a reset must not hide a new reference.
  assert.equal(unreadGuestCancellations([{ ...items[0], reference: 'TD-NEW' }], seen).length, 1);
  assert.equal(unreadGuestCancellations(items, {}).length, 1);
});
