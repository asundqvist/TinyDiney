import React, { useState } from 'react';
import { saveRestaurant } from './restaurantTools.js';

export function RestaurantEditor({ restaurant, request, onSaved, onCancel }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true); setError('');
    try { onSaved(await saveRestaurant(request, restaurant, values)); }
    catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  return <section className="restaurant-editor pending-card" aria-labelledby="restaurant-editor-title">
    <h2 id="restaurant-editor-title">{restaurant ? 'Edit restaurant' : 'Add restaurant'}</h2>
    {error && <p className="notice" role="alert">{error}</p>}
    <form onSubmit={submit}>
      <fieldset disabled={busy}><label>Name<input name="name" required maxLength={255} defaultValue={restaurant?.name || ''} /></label>
      <div className="form-row"><label>City<input name="city" required maxLength={255} defaultValue={restaurant?.city || ''} /></label><label>Cuisine<input name="cuisine" required maxLength={255} defaultValue={restaurant?.cuisine || ''} /></label></div>
      <label>Number of seats<input name="seats" type="number" min="1" max="2147483647" step="1" required defaultValue={restaurant?.seats || ''} /></label></fieldset>
      <div className="restaurant-editor-actions"><button className="button primary" disabled={busy}>{busy ? 'Saving...' : restaurant ? 'Save changes' : 'Add restaurant'}</button><button type="button" className="button outline" disabled={busy} onClick={onCancel}>Cancel</button></div>
    </form>
  </section>;
}

export default function AdminRestaurants({ restaurants, loading, error, request, onSaved, retry }) {
  const [editing, setEditing] = useState(null);
  const [adding, setAdding] = useState(false);
  const [notice, setNotice] = useState('');
  function saved(restaurant) {
    onSaved(restaurant);
    setNotice(restaurant.name + (adding ? ' has been added.' : ' has been updated.'));
    setAdding(false); setEditing(null);
  }
  return <section className="staff-section wrap" aria-labelledby="admin-title">
    <div className="section-top"><div><p className="eyebrow">ADMIN WORKSPACE</p><h1 id="admin-title">Manage restaurants</h1><p className="muted">Add restaurants and update their name, city, cuisine and seating capacity.</p></div><button className="button primary" disabled={adding || editing !== null} onClick={() => { setAdding(true); setNotice(''); }}>Add restaurant</button></div>
    {notice && <p className="staff-notice" role="status">{notice}</p>}
    {(adding || editing !== null) && <RestaurantEditor key={editing?.id ?? 'new'} restaurant={editing} request={request} onSaved={saved} onCancel={() => { setAdding(false); setEditing(null); }} />}
    {loading ? <p role="status">Loading restaurants...</p> : error ? <div className="notice" role="alert"><p>{error}</p><button className="button outline small" onClick={retry}>Try again</button></div> : !restaurants.length ? <p className="account-empty">No restaurants yet. Add your first restaurant above.</p> : <div className="admin-restaurant-list">{restaurants.map(restaurant => <article className="pending-card" key={restaurant.id}>
      <div className="pending-heading"><div><h2>{restaurant.name}</h2><p className="muted">{restaurant.city} &middot; {restaurant.cuisine} &middot; {restaurant.seats} seats</p></div><button className="button outline small" disabled={adding || editing !== null} aria-label={'Edit ' + restaurant.name} onClick={() => { setEditing(restaurant); setNotice(''); }}>Edit restaurant</button></div>
    </article>)}</div>}
  </section>;
}
