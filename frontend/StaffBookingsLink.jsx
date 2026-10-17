import React from 'react';
import BookingNotification from './BookingNotification.jsx';

export default function StaffBookingsLink({ pending, active, onOpen }) {
  const requests = pending.items.filter(booking => booking.status === 'REQUESTED').length;
  const cancellations = pending.unreadCancellations?.length || 0;
  const count = pending.error || pending.loading ? null : requests + cancellations;
  return <button className="nav-tab staff-nav" aria-current={active ? 'page' : undefined}
    aria-label={'Manage Bookings' + (pending.error ? ', requests unavailable' : count > 0 ? ', ' + requests + ' pending requests' + (cancellations ? ', ' + cancellations + ' guest cancellations' : '') : '')}
    onClick={onOpen}>
    Manage Bookings
    <BookingNotification count={count} />
    {pending.error && <span className="booking-notification unavailable" aria-hidden="true">!</span>}
  </button>;
}
