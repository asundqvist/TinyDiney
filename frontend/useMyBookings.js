import { useCallback } from 'react';
import { loadMyBookings, markBookingUpdatesRead, unreadBookingUpdates } from './myBookingTools.js';
import useBookingFeed from './useBookingFeed.js';
import useBookingReadState from './useBookingReadState.js';

export default function useMyBookings(enabled, request, subject) {
  const feed = useBookingFeed(enabled, request, subject, loadMyBookings);
  const { seen, markRead } = useBookingReadState('tinydiney-booking-updates:', enabled, subject, feed.items, markBookingUpdatesRead);
  const replaceBooking = useCallback(booking => {
    // The cancellation response does not contain the guest-specific deadline fields.
    feed.replaceBooking(booking, (current, updated) => ({ ...current, status: updated.status, cancellable: false }));
  }, [feed.replaceBooking]);
  return { ...feed, unread: unreadBookingUpdates(feed.items, seen), markRead, replaceBooking };
}
