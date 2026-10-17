import React from 'react';
import MyBookings from './MyBookings.jsx';

export default function AccountPanel({ isStaff, isAdmin, onAdminRestaurants, user, request, restaurants, account, onBook, onStaffBookings, onSignOut }) {
  return <>
    <div className="account-summary">
      <p className="muted">You are signed in as {user?.name || user?.email || 'guest'}.</p>
      <button className="button outline small" onClick={onSignOut}>Sign out</button>
    </div>
    {isAdmin ? <><button className="button primary full" onClick={onAdminRestaurants}>Manage restaurants</button><button className="button outline full" onClick={onStaffBookings}>Manage Bookings</button></> : isStaff ? <button className="button primary full" onClick={onStaffBookings}>Manage bookings</button> : <>
      <MyBookings key={user?.sub} request={request} restaurants={restaurants} account={account} />
      <button className="button primary full" onClick={onBook}>Book a table</button>
    </>}
    <button className="button outline full" onClick={onSignOut}>Sign out</button>
  </>;
}
