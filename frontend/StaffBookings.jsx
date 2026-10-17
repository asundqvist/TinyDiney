import { bookingDateFormat as dateFormat } from './bookingTools.js';
import React, { useState } from 'react';
import Pagination from './Pagination.jsx';
import usePagination from './usePagination.js';
import { filterBookingReferences } from './pagination.js';
import { updateStaffBooking } from './bookingTools.js';

const statuses = [['REQUESTED', 'Pending'], ['CONFIRMED', 'Confirmed'], ['SEATED', 'Seated'], ['CANCELLED', 'Cancelled']];
const labelOf = status => statuses.find(([value]) => value === status)?.[1] || status;

export function StaffBookingCard({ booking, restaurantName, busy, declining, onAct, onDecline }) {
  const pending = booking.status === 'REQUESTED';
  const actionable = pending || booking.status === 'CONFIRMED';
  return <article className="pending-card" aria-labelledby={'booking-' + booking.id}>
    <div className="pending-heading"><div><p className="restaurant-location">{restaurantName}</p><h2 id={'booking-' + booking.id}>{booking.customerName}</h2></div><span className="pending-badge">{labelOf(booking.status)} &middot; #{booking.id}</span></div>
    {booking.reference && <p className="booking-reference">Booking reference: <strong>{booking.reference}</strong></p>}
    {booking.guestCancelledAfterConfirmation && <p className="notice">Cancelled by the guest after confirmation.</p>}
    <dl className="pending-details"><div><dt>Date &amp; time</dt><dd><time dateTime={booking.time}>{dateFormat.format(new Date(booking.time))}</time></dd></div><div><dt>Guests</dt><dd>{booking.guests}</dd></div><div><dt>Email</dt><dd><a href={'mailto:' + booking.customerEmail}>{booking.customerEmail}</a></dd></div></dl>
    {actionable && <div className="pending-actions">{declining === booking.id ? <><p>{pending ? "Decline this family's request?" : 'Cancel this confirmed booking?'}</p><button className="button primary small" disabled={busy !== null} onClick={() => onAct(booking, 'cancel')}>{busy === booking.id ? 'Saving...' : pending ? 'Yes, decline' : 'Yes, cancel booking'}</button><button className="button outline small" disabled={busy !== null} onClick={() => onDecline(null)}>Keep booking</button></> : <><button className="button primary small" disabled={busy !== null} onClick={() => onAct(booking, pending ? 'confirm' : 'seat')}>{busy === booking.id ? 'Saving...' : pending ? 'Confirm booking' : 'Mark as seated'}</button><button className="button outline small" disabled={busy !== null} onClick={() => onDecline(booking.id)}>{pending ? 'Decline request' : 'Cancel booking'}</button></>}</div>}
  </article>;
}

export default function StaffBookings({ request, restaurants, pending }) {
  const { items: bookings, loading, error: loadError, refresh, replaceBooking, unreadCancellations = [], markCancellationsRead } = pending;
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(null);
  const [declining, setDeclining] = useState(null);
  const [restaurantId, setRestaurantId] = useState('');
  const [status, setStatus] = useState('REQUESTED');
  const [query, setQuery] = useState('');

  async function act(booking, action) {
    if (busy !== null) return;
    setBusy(booking.id); setActionError(''); setNotice('');
    try {
      const updated = await updateStaffBooking(request, booking, action);
      replaceBooking(updated);
      setDeclining(null);
      setNotice('Booking #' + booking.id + (action === 'confirm' ? ' confirmed.' : action === 'seat' ? ' marked as seated.' : booking.status === 'REQUESTED' ? ' declined.' : ' cancelled.'));
    } catch (error) {
      setActionError(error.status === 409 ? 'This booking has already been changed. The booking list has been refreshed.' : error.message);
      if (error.status === 409 || error.status === 404) refresh();
    } finally { setBusy(null); }
  }

  const matching = filterBookingReferences(bookings, query);
  const atRestaurant = matching.filter(booking => !restaurantId || String(booking.restaurantId) === restaurantId);
  const visible = atRestaurant.filter(booking => status === 'ALL' || booking.status === status);
  const bookingPage = usePagination(visible, JSON.stringify([status, restaurantId, query]), 5);
  const changeBookingPage = page => { setDeclining(null); bookingPage.onPageChange(page); };
  const nameOf = id => restaurants.find(restaurant => restaurant.id === id)?.name || 'Restaurant #' + id;
  return <section className="staff-section wrap" aria-labelledby="staff-title">
    <div className="section-top"><div><p className="eyebrow">STAFF WORKSPACE</p><h1 id="staff-title">Manage Bookings</h1><p className="muted">Review requests, manage confirmed tables and mark arriving guests as seated. Updates every 15 seconds while this page is visible.</p></div><button className="button outline" disabled={loading || busy !== null} onClick={() => { setNotice(''); setActionError(''); setDeclining(null); refresh(); }}>Refresh bookings</button></div>
    {!loading && !loadError && unreadCancellations.length > 0 && <div className="notice cancellation-alert" role="status"><strong>Guest cancellations after confirmation ({unreadCancellations.length})</strong><ul>{unreadCancellations.slice(0, 6).map(booking => <li key={booking.id}>{booking.customerName} at {nameOf(booking.restaurantId)} &middot; {dateFormat.format(new Date(booking.time))}{booking.reference ? ' / ' + booking.reference : ''}</li>)}</ul>{unreadCancellations.length > 6 && <p>View cancelled bookings to see all cancellations.</p>}<button className="button outline small" onClick={() => { setStatus('CANCELLED'); setRestaurantId(''); setQuery(''); }}>View cancelled bookings</button><button className="button outline small" onClick={markCancellationsRead}>Mark cancellations as read</button></div>}
    <div className="booking-search"><label>Search by booking reference<input type="search" value={query} placeholder="e.g. TD-ABCDEFGH-23456789" onChange={event => { setQuery(event.target.value); setDeclining(null); if (event.target.value.trim()) setStatus('ALL'); }} /></label></div>
    <div className="filters" role="group" aria-label="Filter staff bookings"><button className={status === 'ALL' ? 'active' : ''} aria-pressed={status === 'ALL'} onClick={() => { setStatus('ALL'); setDeclining(null); }}>All bookings{!loading && !loadError && ' (' + atRestaurant.length + ')'}</button>{statuses.map(([value, label]) => <button key={value} className={value === status ? 'active' : ''} aria-pressed={value === status} onClick={() => { setStatus(value); setDeclining(null); }}>{label}{!loading && !loadError && ' (' + atRestaurant.filter(booking => booking.status === value).length + ')'}</button>)}</div>
    <div className="staff-toolbar"><label>Restaurant<select value={restaurantId} onChange={event => { setRestaurantId(event.target.value); setDeclining(null); }}><option value="">All restaurants</option>{restaurants.map(restaurant => <option key={restaurant.id} value={restaurant.id}>{restaurant.name} &middot; {restaurant.city}</option>)}</select></label><div className="booking-toolbar-actions"><p className="results-count" role="status">{loading ? 'Loading bookings...' : loadError ? 'Bookings unavailable' : visible.length + ' ' + (status === 'ALL' ? 'matching' : labelOf(status).toLowerCase()) + ' ' + (visible.length === 1 ? 'booking' : 'bookings')}</p>{!loading && !loadError && <Pagination {...bookingPage} disabled={busy !== null} label="Booking pages above results" onPageChange={changeBookingPage} />}</div></div>
    {notice && <p className="staff-notice" role="status">{notice}</p>}
    {actionError && <p className="notice" role="alert">{actionError}</p>}
    {loadError && <div className="notice" role="alert"><p>{loadError}</p><p>The bookings could not be loaded. This does not mean there are no bookings.</p></div>}
    {!loading && !loadError && !visible.length && <div className="staff-empty"><h2>All caught up.</h2><p>No {status === 'ALL' ? 'matching' : labelOf(status).toLowerCase()} bookings{restaurantId ? ' for this restaurant' : ''}. New bookings appear automatically while this page is visible.</p></div>}
    {!loading && !loadError && <div className="pending-list">{bookingPage.items.map(booking => <StaffBookingCard key={booking.id} booking={booking} restaurantName={nameOf(booking.restaurantId)} busy={busy} declining={declining} onAct={act} onDecline={setDeclining} />)}</div>}
    {!loading && !loadError && <Pagination {...bookingPage} disabled={busy !== null} label="Booking pages" onPageChange={changeBookingPage} />}
  </section>;
}
