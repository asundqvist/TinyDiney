import { loadStaffBookings, unreadGuestCancellations, markGuestCancellationsRead } from './bookingTools.js';
import useBookingFeed from './useBookingFeed.js';
import useBookingReadState from './useBookingReadState.js';

export default function useStaffBookings(enabled, request, subject) {
  const feed = useBookingFeed(enabled, request, subject, loadStaffBookings);
  const { seen, markRead } = useBookingReadState('tinydiney-staff-cancellations:', enabled, subject, feed.items, markGuestCancellationsRead);
  return { ...feed, unreadCancellations: unreadGuestCancellations(feed.items, seen), markCancellationsRead: markRead };
}
