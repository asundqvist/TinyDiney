import { bookingDateFormat as dateFormat } from './bookingTools.js';
import React, { useEffect, useRef, useState } from 'react';
import { canCancelMyBooking } from './myBookingTools.js';

const statuses = [['REQUESTED', 'Pending'], ['CONFIRMED', 'Confirmed'], ['CANCELLED', 'Cancelled'], ['SEATED', 'Completed']];

export default function MyBookings({ request, restaurants, account }) {
  const { items: bookings, loading, error, markRead, replaceBooking, refresh } = account;
  const initialStatusChosen = useRef(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [status, setStatus] = useState('REQUESTED');
  const [confirming, setConfirming] = useState(null);
  const [busy, setBusy] = useState(null);
  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (loading || error) return;
    if (!initialStatusChosen.current) {
      setStatus(account.unread[0]?.status || 'REQUESTED');
      initialStatusChosen.current = true;
    }
    markRead();
  }, [bookings, loading, error, markRead]);

  async function cancel(booking) {
    if (busy !== null) return;
    if (!canCancelMyBooking(booking)) {
      setActionError('The cancellation deadline has passed. Contact the restaurant for help.');
      setConfirming(null); setNow(Date.now()); return;
    }
    setBusy(booking.id); setActionError(''); setNotice('');
    try {
      const updated = await request('/bookings/' + booking.id + '/cancel', { method: 'POST' });
      replaceBooking(updated);
      setConfirming(null);
      setNotice((booking.reference ? 'Booking ' + booking.reference : 'Your booking') + ' has been cancelled.');
    } catch (failure) {
      setActionError(failure.message);
      if (failure.status === 409 || failure.status === 404) refresh();
    } finally { setBusy(null); }
  }

  const visible = bookings.filter(booking => booking.status === status);
  return <section className="my-bookings" aria-labelledby="my-bookings-title">
    <div className="account-bookings-heading"><h3 id="my-bookings-title">My bookings</h3><button className="detail-link" disabled={loading || busy !== null} onClick={() => { setActionError(''); setNotice(''); setConfirming(null); refresh(); }}>Refresh bookings</button></div>
    <p className="muted">You can cancel pending or confirmed bookings until one hour before your table time. All times are shown in Swedish local time.</p>
    <div className="filters" role="group" aria-label="Filter my bookings">{statuses.map(([value, label]) => <button key={value} className={value === status ? 'active' : ''} aria-pressed={value === status} onClick={() => { setStatus(value); setConfirming(null); }}>{label}{!loading && !error && ' (' + bookings.filter(booking => booking.status === value).length + ')'}</button>)}</div>
    {notice && <p className="staff-notice" role="status">{notice}</p>}
    {actionError && <p className="notice" role="alert">{actionError}</p>}
    {loading ? <p role="status">Loading your bookings…</p> : error ? <p className="notice" role="alert">{error}</p> : !visible.length ? <p className="account-empty">No {statuses.find(([value]) => value === status)[1].toLowerCase()} bookings yet.</p> : <div className="pending-list">{visible.map(booking => <article className="pending-card account-booking" key={booking.id}>
      <div className="pending-heading"><h4>{restaurants.find(restaurant => restaurant.id === booking.restaurantId)?.name || 'Restaurant #' + booking.restaurantId}</h4></div>
      {booking.reference && <p className="booking-reference">Booking reference: <strong>{booking.reference}</strong></p>}
      <p className="account-booking-time"><time dateTime={booking.time}>{dateFormat.format(new Date(new Date(booking.cancellationDeadline).getTime() + 3600000))}</time> · {booking.guests} {booking.guests === 1 ? 'guest' : 'guests'}</p>
      {['REQUESTED', 'CONFIRMED'].includes(booking.status) && <>
        <p className="form-note">{canCancelMyBooking(booking, now) ? 'Cancel by ' + dateFormat.format(new Date(booking.cancellationDeadline)) + '.' : 'Online cancellation is closed. Contact the restaurant for help.'}</p>
        <div className="pending-actions">{confirming === booking.id && canCancelMyBooking(booking, now) ? <><p>Cancel this booking?</p><button className="button primary small" disabled={busy !== null} onClick={() => cancel(booking)}>{busy === booking.id ? 'Cancelling…' : 'Yes, cancel booking'}</button><button className="button outline small" disabled={busy !== null} onClick={() => setConfirming(null)}>Keep booking</button></> : <button className="button outline small" disabled={busy !== null || !canCancelMyBooking(booking, now)} onClick={() => setConfirming(booking.id)}>Cancel booking</button>}</div>
      </>}
    </article>)}</div>}
  </section>;
}
