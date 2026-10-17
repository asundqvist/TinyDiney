import { useCallback, useEffect, useState } from 'react';

function readSeen(key) {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '{}');
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
  } catch { return {}; }
}

export default function useBookingReadState(prefix, enabled, subject, items, markItemsRead) {
  const [state, setState] = useState({ subject: null, values: {} });
  const key = prefix + encodeURIComponent(subject);
  useEffect(() => {
    setState(enabled && subject ? { subject, values: readSeen(key) } : { subject: null, values: {} });
  }, [enabled, subject, key]);
  const markRead = useCallback(() => {
    if (!enabled || !subject) return;
    setState(previous => {
      const values = markItemsRead(items, previous.subject === subject ? previous.values : {});
      try { localStorage.setItem(key, JSON.stringify(values)); } catch { /* Keep read state in memory. */ }
      return { subject, values };
    });
  }, [enabled, subject, key, items, markItemsRead]);
  return { seen: state.subject === subject ? state.values : {}, markRead };
}
