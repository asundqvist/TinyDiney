export const PAGE_SIZE = 6;

export function paginate(items, requestedPage = 0, pageSize = PAGE_SIZE) {
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const page = Math.max(0, Math.min(totalPages - 1, requestedPage));
  return { items: items.slice(page * pageSize, (page + 1) * pageSize), page, totalPages, total: items.length };
}

export function filterBookingReferences(bookings, query) {
  const search = query.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (!search) return bookings;
  return bookings.filter(booking => (booking.reference || '').toUpperCase().replace(/[^A-Z0-9]/g, '').includes(search));
}
