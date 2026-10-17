import { loadAllPages } from './apiTools.js';
import { compareBookings } from './bookingTools.js';

export async function loadMyBookings(request) {
  return (await loadAllPages(request, '/bookings/mine?size=100')).sort(compareBookings);
}

export function canCancelMyBooking(booking, now = Date.now()) {
  const deadline = Date.parse(booking.cancellationDeadline);
  return booking.cancellable === true && ['REQUESTED', 'CONFIRMED'].includes(booking.status)
    && Number.isFinite(deadline) && now <= deadline;
}

export function unreadBookingUpdates(bookings, seen = {}) {
  return bookings.filter(booking => Number.isInteger(booking.staffUpdateRevision) && booking.staffUpdateRevision > 0
    && booking.staffUpdateRevision > (seen[booking.id] || 0));
}

export function markBookingUpdatesRead(bookings, seen = {}) {
  const next = { ...seen };
  for (const booking of bookings) {
    if (Number.isInteger(booking.staffUpdateRevision) && booking.staffUpdateRevision > 0) {
      next[booking.id] = Math.max(next[booking.id] || 0, booking.staffUpdateRevision);
    }
  }
  return next;
}
