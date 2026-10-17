import React from 'react';

export default function BookingNotification({ count }) {
  if (!count) return null;
  return <span className="booking-notification" aria-hidden="true"><svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>{count > 99 ? '99+' : count}</span>;
}
