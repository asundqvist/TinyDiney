import React, { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import FoodArt from './FoodArt.jsx';
import { demoRestaurants, filterRestaurants, findRestaurant, loadRestaurantRecords, presentRestaurant } from './catalogue.js';
import { quarterHourTimes, isQuarterHour, localDate } from './bookingTools.js';
import { readAccessRoles, canManageBookings, canManageRestaurants, rolesClaim, apiAccessError } from './auth.js';
import Pagination from './Pagination.jsx';
import usePagination from './usePagination.js';
import AdminRestaurants from './AdminRestaurants.jsx';
import StaffBookings from './StaffBookings.jsx';
import useStaffBookings from './useStaffBookings.js';
import StaffBookingsLink from './StaffBookingsLink.jsx';
import AccountPanel from './AccountPanel.jsx';
import useMyBookings from './useMyBookings.js';
import BookingNotification from './BookingNotification.jsx';
import RestaurantSlideshow from './RestaurantSlideshow.jsx';

function scrollToTop() {
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
}

function Modal({ title, onClose, children, className = '' }) {
  const ref = useRef(null);
  const startedOnBackdrop = useRef(false);
  const titleId = useId();
  useEffect(() => { ref.current.showModal(); }, []);
  function isBackdrop(event) {
    if (event.target !== ref.current) return false;
    const bounds = ref.current.getBoundingClientRect();
    return event.clientX < bounds.left || event.clientX > bounds.right
      || event.clientY < bounds.top || event.clientY > bounds.bottom;
  }
  return <dialog className={className} ref={ref} aria-labelledby={titleId} onCancel={onClose}
    onPointerDown={event => { startedOnBackdrop.current = event.button === 0 && isBackdrop(event); }}
    onPointerCancel={() => { startedOnBackdrop.current = false; }}
    onClick={event => {
      const shouldClose = startedOnBackdrop.current && isBackdrop(event);
      startedOnBackdrop.current = false;
      if (shouldClose) onClose();
    }}>
    <div className="modal-head"><h2 id={titleId}>{title}</h2><button className="close-button" onClick={onClose} aria-label="Close dialog">×</button></div>
    {children}
  </dialog>;
}

function BookingForm({ request, user, restaurants, preferred, loading, loadError, retry }) {
  const [restaurantId, setRestaurantId] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null);
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 60000);
    return () => clearInterval(timer);
  }, []);
  const matched = findRestaurant(restaurants, preferred);
  useEffect(() => { setRestaurantId(matched ? String(matched.id) : ''); }, [matched?.id]);

  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (!restaurants.some(item => String(item.id) === values.restaurantId)) { setError('Please choose an available restaurant.'); return; }
    if (!isQuarterHour(time)) { setError('Please choose a quarter-hour time between 15:00 and 20:00.'); return; }
    const bookingTime = date + 'T' + time + ':00';
    if (!Number.isFinite(new Date(bookingTime).getTime()) || new Date(bookingTime) <= new Date()) { setError('Please choose a date and time in the future.'); return; }
    setSending(true); setError('');
    try {
      const booking = await request('/bookings', {
        method: 'POST',
        body: JSON.stringify({ restaurantId: Number(values.restaurantId), customerName: values.customerName, customerEmail: values.customerEmail, guests: Number(values.guests), time: bookingTime }),
      });
      setResult({ ...booking, restaurantName: restaurants.find(item => String(item.id) === values.restaurantId)?.name });
    } catch (e) { setError(e.message); }
    finally { setSending(false); }
  }

  if (result) return <div className="success" role="status"><span className="success-icon">✓</span><h3>Your request is on its way!</h3><p>Your booking at <strong>{result.restaurantName}</strong> is waiting for confirmation from the restaurant.</p>{result.reference && <p>Booking reference: <strong>{result.reference}</strong></p>}</div>;
  return <>
    <p className="muted">{preferred ? 'Request a family table at ' + preferred.name + '.' : 'Choose a restaurant and send your reservation request.'}</p>
    {loading ? <p role="status">Loading restaurants…</p> : loadError ? <div className="notice" role="alert"><p>{loadError}</p><button className="button outline" onClick={retry}>Try again</button></div> : <>
      {preferred && !matched && <p className="notice">{preferred.name} is not currently available to book. Please choose another restaurant below.</p>}
      {!restaurants.length && <p className="notice">No restaurants are available to book yet. Please check back later.</p>}
      {error && <p className="notice" role="alert">{error}</p>}
      {!!restaurants.length && <form onSubmit={submit}>
        <label>Restaurant<select name="restaurantId" required value={restaurantId} onChange={event => setRestaurantId(event.target.value)}><option value="" disabled>Choose a restaurant</option>{restaurants.map(item => <option key={item.id} value={item.id}>{item.name} · {item.city}</option>)}</select></label>
        <label>Your name<input name="customerName" autoComplete="name" defaultValue={user?.name || ''} required maxLength={120} /></label>
        <label>Email<input name="customerEmail" type="email" autoComplete="email" defaultValue={user?.email || ''} required /></label>
        <div className="form-row"><label>Date<input name="date" type="date" min={localDate(now)} required value={date} onChange={event => { setDate(event.target.value); setTime(''); }} /></label><label>Time<select name="time" required value={time} onChange={event => setTime(event.target.value)} disabled={!date}><option value="" disabled>Choose a time</option>{quarterHourTimes.map(slot => <option key={slot} value={slot} disabled={new Date(date + 'T' + slot) <= now}>{slot}</option>)}</select></label></div>
        <p className="field-note">Reservations start every 15 minutes from 15:00 to 20:00.</p>
        <label>Number of guests<select name="guests" defaultValue="4">{Array.from({ length: 10 }, (_, index) => <option key={index} value={index + 1}>{index + 1} {index ? 'guests' : 'guest'}</option>)}</select></label>
        <button className="button primary full" disabled={sending}>{sending ? 'Sending…' : 'Send reservation request'}</button>
        <p className="form-note">Your reservation is only confirmed once the selected restaurant has accepted it.</p>
      </form>}
    </>}
  </>;
}

export default function App({ authConfigured }) {
  const { isAuthenticated, isLoading, error, user, loginWithRedirect, logout, getAccessTokenSilently } = useAuth0();
  const [modal, setModal] = useState(null);
  const [category, setCategory] = useState('All restaurants');
  const [city, setCity] = useState('All cities');
  const [query, setQuery] = useState('');
  const [loginError, setLoginError] = useState('');
  const [loginPending, setLoginPending] = useState(false);
  const [selectedRestaurant, setSelectedRestaurant] = useState(null);
  const [restaurantRecords, setRestaurantRecords] = useState([]);
  const restaurants = useMemo(() => restaurantRecords.map(presentRestaurant), [restaurantRecords]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [reload, setReload] = useState(0);
  const [view, setView] = useState('home');
  useEffect(() => {
    scrollToTop();
  }, [view]);
  const [access, setAccess] = useState({ subject: null, roles: [] });
  const accessReady = isAuthenticated && access.subject === user?.sub;
  const canStaff = accessReady && canManageBookings(access.roles);

  const canAdmin = accessReady && canManageRestaurants(access.roles);

  const request = useCallback(async (path, options = {}) => {
    const token = await getAccessTokenSilently();
    let response;
    try {
      response = await fetch((import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '') + path, {
        ...options,
        headers: { ...options.headers, Authorization: 'Bearer ' + token, ...(options.body ? { 'Content-Type': 'application/json' } : {}) },
      });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      throw new Error('We could not reach TinyDiney. Please try again in a moment.');
    }
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) throw apiAccessError(response.status, path, options.method || 'GET', token);
      const problem = await response.json().catch(() => ({}));
      const apiError = new Error(problem.detail || 'We could not process your request. Please check your details and try again.');
      apiError.status = response.status;
      throw apiError;
    }
    return response.json();
  }, [getAccessTokenSilently]);
  const pending = useStaffBookings(canStaff, request, user?.sub);
  const canBook = !isAuthenticated || (accessReady && !canStaff);
  const account = useMyBookings(isAuthenticated && accessReady && !canStaff, request, user?.sub);
  const accountNotifications = !canStaff && !account.loading && !account.error ? account.unread.length : 0;

  useEffect(() => {
    if (!isAuthenticated) { setAccess({ subject: null, roles: [] }); setView('home'); return; }
    let active = true;
    getAccessTokenSilently().then(token => {
      if (active) setAccess({ subject: user?.sub, roles: readAccessRoles(token, import.meta.env.VITE_AUTH0_ROLES_CLAIM || rolesClaim) });
    }).catch(() => { if (active) setLoginError('We could not load your account permissions. Please sign out and sign in again.'); });
    return () => { active = false; };
  }, [isAuthenticated, user?.sub, getAccessTokenSilently]);

  useEffect(() => {
    if (!isAuthenticated) { setRestaurantRecords([]); setLoading(false); setLoadError(''); return; }
    let active = true;
    setLoading(true); setLoadError('');
    loadRestaurantRecords(request).then(items => { if (active) { setRestaurantRecords(items); } })
      .catch(e => { if (active) setLoadError(e.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [isAuthenticated, request, reload]);

  useEffect(() => {
    if (accessReady && (window.location.hash === '#boka' || window.location.hash.startsWith('#boka?'))) {
      const params = new URLSearchParams(window.location.hash.split('?')[1] || '');
      setSelectedRestaurant(params.has('restaurant') && params.has('city') ? { name: params.get('restaurant'), city: params.get('city') } : null);
      if (canStaff) { setModal(null); setSelectedRestaurant(null); setView(canAdmin ? 'admin' : 'staff'); }
      else setModal('booking');
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [accessReady, canStaff, canAdmin]);

  useEffect(() => {
    if (canStaff && modal === 'booking') { setModal('login'); setSelectedRestaurant(null); }
  }, [canStaff, modal]);

  async function login() {
    setLoginError(''); setLoginPending(true);
    try { await loginWithRedirect({ appState: { openBooking: modal === 'booking', restaurant: selectedRestaurant ? { name: selectedRestaurant.name, city: selectedRestaurant.city } : null } }); }
    catch { setLoginError('We could not start sign-in. Please try again in a moment.'); setLoginPending(false); }
  }
  const authErrorMessage = error?.message?.includes('not authorized to access resource server')
    ? 'This application does not have access to the restaurant API. Enable User-Delegated Access for the Single Page Application in Auth0 under Applications → APIs → Restaurant API → Application Access.'
    : 'Something went wrong while signing in. Please try again.';
  const close = () => { setModal(null); setSelectedRestaurant(null); setLoginError(''); };
  const openBooking = (restaurant = null) => { if (!canBook) return; setSelectedRestaurant(restaurant); setModal('booking'); };
  const exploreRestaurant = restaurant => { setSelectedRestaurant(restaurant); setModal('restaurant'); };
  const retry = () => setReload(value => value + 1);
  const navigate = (nextView, event) => {
    event?.preventDefault();
    if (nextView === view) scrollToTop();
    setView(nextView);
    window.history.replaceState({}, document.title, window.location.pathname + window.location.search);
  };
  const catalogue = isAuthenticated ? restaurants : demoRestaurants;
  const cities = [...new Set(catalogue.map(item => item.city))].sort();
  const visible = filterRestaurants(catalogue, { category, city, query });
  const restaurantPage = usePagination(visible, JSON.stringify([category, city, query]));
  const changeRestaurantPage = page => {
    restaurantPage.onPageChange(page);
    scrollToTop();
  };

  return <>
    <a className="skip-link" href="#main">Skip to content</a>
    <header className="header wrap">
      <a className="brand" href="#" onClick={event => navigate('home', event)} aria-label="TinyDiney home">TinyDiney<span className="brand-caption">TINY FAMILIES. BIG DECISIONS</span></a>
      <nav aria-label="Main navigation">
        <a href="#restaurants" onClick={event => navigate('restaurants', event)} aria-current={view === 'restaurants' ? 'page' : undefined}>Restaurants</a>
        {canAdmin && <button className="nav-tab" aria-current={view === 'admin' ? 'page' : undefined} onClick={() => navigate('admin')}>Manage restaurants</button>}
        {canStaff && <StaffBookingsLink pending={pending} active={view === 'staff'} onOpen={() => { navigate('staff'); pending.refresh(); }} />}
        <button className="button primary small account-nav" disabled={authConfigured && isLoading} aria-label={isAuthenticated ? 'My account' + (accountNotifications ? ', ' + accountNotifications + ' booking updates' : '') : 'Sign in'} onClick={() => { setModal('login'); if (isAuthenticated && !canStaff) account.refresh(); }}>{isAuthenticated ? 'My account' : 'Sign in'}<BookingNotification count={accountNotifications} /></button>
      </nav>
    </header>
    {(error || loginError) && <div className="wrap notice" role="alert">{loginError || authErrorMessage}</div>}
    <main id="main">
      {canAdmin && view === 'admin' ? <AdminRestaurants restaurants={restaurantRecords} loading={loading} error={loadError} request={request} retry={retry} onSaved={saved => { setRestaurantRecords(previous => previous.some(item => item.id === saved.id) ? previous.map(item => item.id === saved.id ? saved : item) : [...previous, saved]); retry(); }} /> : canStaff && view === 'staff' ? <StaffBookings request={request} restaurants={restaurants} pending={pending} /> : view === 'restaurants' ? <>
      <section className="menu-section wrap" id="restaurants">
        <div className="section-top"><div><h2>What suits your family?</h2></div></div>
        <div className="restaurant-search"><label>Search restaurants<input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Restaurant, cuisine or signature dish" /></label><label>City<select value={city} onChange={event => setCity(event.target.value)}><option>All cities</option>{cities.map(value => <option key={value}>{value}</option>)}</select></label></div>
        <div className="filters" role="group" aria-label="Filter restaurants by food">{['All restaurants', 'Classics', 'Veggie', 'Sweet treats'].map(value => <button key={value} aria-pressed={category === value} className={category === value ? 'active' : ''} onClick={() => setCategory(value)}>{value}</button>)}</div>
        {!isAuthenticated && <p className="catalogue-note">Explore our fictional demo restaurants. Sign in to see which restaurants are available to book.</p>}
        {isAuthenticated && loading ? <p role="status">Loading restaurants…</p> : isAuthenticated && loadError ? <div className="notice" role="alert"><p>{loadError}</p><button className="button outline" onClick={retry}>Try again</button></div> : <>
          <div className="catalogue-toolbar">
            <p className="results-count" role="status">{visible.length} {visible.length === 1 ? 'restaurant' : 'restaurants'} found</p>
            <Pagination {...restaurantPage} label="Restaurant pages above results" onPageChange={changeRestaurantPage} />
          </div>
          {!visible.length && <p className="notice">{catalogue.length ? 'No restaurants match your search. Try another city or favourite.' : 'No restaurants are available yet. Please check back soon.'}</p>}
          <div className="dish-grid">{restaurantPage.items.map(restaurant => <article className={'dish-card ' + (restaurant.dish?.type || '')} key={restaurant.id ?? restaurant.name + restaurant.city}>
            <button className="dish-image" aria-label={'Explore ' + restaurant.name} onClick={() => exploreRestaurant(restaurant)}><span className="dish-tag">{restaurant.cuisine}</span>{restaurant.dish ? <FoodArt type={restaurant.dish.type} /> : <span className="restaurant-placeholder">A new place to discover</span>}</button>
            <div className="dish-copy"><p className="restaurant-location">{restaurant.city}</p><h3 className="restaurant-name">{restaurant.name}</h3><p>{restaurant.description}</p>
              {restaurant.dish && <div className="signature-dish"><span>THE LITTLE DINER'S PICK</span><div><strong>{restaurant.dish.name}</strong><span>{restaurant.dish.price} SEK</span></div></div>}
              <div className="restaurant-actions">{canBook && <button className="button primary small" onClick={() => openBooking(restaurant)}>Book here</button>}<button className="detail-link" onClick={() => exploreRestaurant(restaurant)}>View restaurant</button></div>
            </div>
          </article>)}</div>
          <Pagination {...restaurantPage} label="Restaurant pages" onPageChange={changeRestaurantPage} />
        </>}
        <p className="menu-note" style={{ textAlign: "center"}}>Featured restaurant names, signature dishes and prices are fictional examples. Check ingredients and allergens with the restaurant. Reservations are for seats, not food orders.</p>
      </section>
      </> : <>
      <section className="hero wrap">
        <div className="hero-copy"><h1>Picky children?<br /><span>Let us help them pick!</span></h1><p>We know how tricky it can be sort out dinner for the tiny ones in the family. TinyDiney helps you discover places close to you where your kids will love the food and you can feel relaxed. Browse our restaurants and find the perfect spot for your next family meal!</p><div className="hero-actions"><a className="button primary" href="#restaurants" onClick={event => navigate('restaurants', event)}>Find a restaurant</a></div></div>
        <RestaurantSlideshow restaurants={catalogue.length ? catalogue : demoRestaurants} onExplore={exploreRestaurant} />
      </section>
      </>}
    </main>
    {modal && <Modal className={modal === 'login' && isAuthenticated ? 'account-dialog' : ''} title={modal === 'restaurant' ? selectedRestaurant.name : modal === 'booking' ? 'A table for the whole family' : isAuthenticated ? 'My account' : 'Good to see you!'} onClose={close}>
      {modal === 'restaurant' ? <><p className="muted">{selectedRestaurant.city} · {selectedRestaurant.cuisine}</p><p>{selectedRestaurant.description}</p>{selectedRestaurant.dish && <><div className={'modal-food ' + selectedRestaurant.dish.type}><FoodArt type={selectedRestaurant.dish.type} /></div><p className="eyebrow">SIGNATURE DISH FOR LITTLE DINERS</p><h3>{selectedRestaurant.dish.name}</h3><p>{selectedRestaurant.dish.text}</p><p><strong>{selectedRestaurant.dish.price} SEK</strong> · Sample price</p><p className="notice">{selectedRestaurant.dish.allergens} These are example ingredients. Confirm all allergens with the restaurant.</p></>}{canBook && <><button className="button primary full" onClick={() => openBooking(selectedRestaurant)}>Book at {selectedRestaurant.name}</button><p className="form-note">You are booking seats at this restaurant. Food is ordered at the restaurant.</p></>}</> :
        (authConfigured && isLoading) || (isAuthenticated && !accessReady) ? <p role="status">Checking your session…</p> :
        isAuthenticated ? modal === 'booking' && !canStaff ? <BookingForm request={request} user={user} restaurants={restaurants} preferred={selectedRestaurant} loading={loading} loadError={loadError} retry={retry} /> : <AccountPanel isAdmin={canAdmin} onAdminRestaurants={() => { close(); navigate('admin'); }} isStaff={canStaff} user={user} request={request} restaurants={restaurants} account={account} onBook={() => openBooking()} onStaffBookings={() => { close(); navigate('staff'); pending.refresh(); }} onSignOut={() => logout({ logoutParams: { returnTo: window.location.origin } }).catch(() => setLoginError('Sign-out failed. Please try again.'))} /> : <>
          <p className="muted">{modal === 'booking' ? 'Sign in to request a table' + (selectedRestaurant ? ' at ' + selectedRestaurant.name : ' at a restaurant of your choice') + '.' : 'Sign in to discover restaurants and book your next family meal.'}</p>
          {authConfigured ? <><button className="button primary full" onClick={login} disabled={loginPending}>{loginPending ? 'Opening sign-in…' : 'Continue to sign in'}</button><p className="form-note">Secure sign-in with Auth0.</p></> : <div className="notice"><strong>Sign-in needs to be configured.</strong><p>Add the public Auth0 Client ID to <code>VITE_AUTH0_CLIENT_ID</code> in <code>.env</code> and restart the frontend server. See <code>FRONTEND.md</code>.</p></div>}
        </>}
    </Modal>}
  </>;
}
