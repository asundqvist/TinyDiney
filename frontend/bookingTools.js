import { loadAllPages } from './apiTools.js';

export const quarterHourTimes = Array.from({ length: 21 }, (_, index) =>
  String(15 + Math.floor(index / 4)).padStart(2, '0') + ':' + String((index % 4) * 15).padStart(2, '0'));

export function isQuarterHour(time) {
  return /^(?:(?:1[5-9]):(?:00|15|30|45)|20:00)$/.test(time);
}

export function localDate(date = new Date()) {
  return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');
}

export const bookingDateFormat = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Europe/Stockholm', hourCycle: 'h23' });
export const compareBookings = (first, second) => first.time.localeCompare(second.time) || first.id - second.id;

export async function loadStaffBookings(request) {
  return (await loadAllPages(request, '/bookings?size=100')).sort(compareBookings);
}

export async function updateStaffBooking(request, booking, action) {
  const allowed = booking.status === 'REQUESTED' ? ['confirm', 'cancel']
    : booking.status === 'CONFIRMED' ? ['seat', 'cancel'] : [];
  if (!allowed.includes(action)) {
    throw new Error('This action is not available for the current booking status. Refresh the list and try again.');
  }
  return request('/bookings/' + booking.id + '/' + action, { method: 'POST' });
}

export const cancellationKey = booking => booking.reference || String(booking.id);

export function unreadGuestCancellations(bookings, seen = {}) {
  return bookings.filter(booking => booking.status === 'CANCELLED'
    && booking.guestCancelledAfterConfirmation === true && seen[cancellationKey(booking)] !== true);
}

export function markGuestCancellationsRead(bookings, seen = {}) {
  const next = { ...seen };
  for (const booking of unreadGuestCancellations(bookings, seen)) next[cancellationKey(booking)] = true;
  return next;
}
