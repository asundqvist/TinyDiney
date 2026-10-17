import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

// Shared polling, account isolation and stale-request handling for guest and staff lists.
export default function useBookingFeed(enabled, request, subject, loadBookings) {
  const [state, setState] = useState({ subject: null, items: [], loading: true, error: '' });
  const sequence = useRef(0);
  const controller = useRef(null);
  const inFlight = useRef(false);
  const blocked = useRef(false);

  const invalidate = useCallback(() => {
    ++sequence.current;
    controller.current?.abort();
    inFlight.current = false;
  }, []);

  const refresh = useCallback(async ({ silent = false } = {}) => {
    if (!enabled || !subject || (silent && (inFlight.current || blocked.current))) return;
    invalidate();
    const current = sequence.current;
    const abort = new AbortController();
    controller.current = abort;
    inFlight.current = true;
    if (!silent) setState(previous => ({ ...previous, loading: true }));
    try {
      const items = await loadBookings(path => request(path, { signal: abort.signal }));
      if (current === sequence.current) {
        blocked.current = false;
        setState({ subject, items, loading: false, error: '' });
      }
    } catch (error) {
      if (current === sequence.current && error.name !== 'AbortError') {
        blocked.current = error.status === 401 || error.status === 403;
        setState({ subject, items: [], loading: false, error: error.message });
      }
    } finally { if (current === sequence.current) inFlight.current = false; }
  }, [enabled, request, subject, loadBookings, invalidate]);

  useEffect(() => {
    blocked.current = false;
    if (!enabled || !subject) {
      setState({ subject: null, items: [], loading: false, error: '' });
      return;
    }
    refresh();
    const onVisible = () => { if (document.visibilityState !== 'hidden') refresh({ silent: true }); };
    const timer = setInterval(onVisible, 15000);
    window.addEventListener('focus', onVisible);
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      invalidate();
      clearInterval(timer);
      window.removeEventListener('focus', onVisible);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [enabled, subject, refresh, invalidate]);

  const items = useMemo(() => enabled && state.subject === subject ? state.items : [], [enabled, state, subject]);
  const replaceBooking = useCallback((booking, merge) => {
    invalidate();
    setState(previous => ({ ...previous, loading: false,
      items: previous.items.map(item => item.id === booking.id ? (merge ? merge(item, booking) : booking) : item),
    }));
  }, [invalidate]);

  return { items, refresh, replaceBooking,
    loading: enabled && (state.subject !== subject || state.loading),
    error: enabled && state.subject === subject ? state.error : '',
  };
}
