import { useEffect, useState } from 'react';
import { paginate } from './pagination.js';

export default function usePagination(items, scope, pageSize) {
  const [state, setState] = useState({ scope, page: 0 });
  const result = paginate(items, state.scope === scope ? state.page : 0, pageSize);
  useEffect(() => { setState({ scope, page: 0 }); }, [scope]);
  // Clamp the saved page when an action or poll removes the last item on it.
  useEffect(() => {
    setState(previous => previous.scope === scope && previous.page !== result.page ? { scope, page: result.page } : previous);
  }, [scope, result.page]);
  return { ...result, onPageChange: page => setState({ scope, page }) };
}
