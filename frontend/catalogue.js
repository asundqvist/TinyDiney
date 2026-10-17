import { loadAllPages } from './apiTools.js';
import examples from '../src/main/resources/demo/restaurants.json';

export const demoRestaurants = examples;

// IDs always come from the API. Names and cities connect local demo presentation to stored records.
export function presentRestaurant(restaurant) {
  const demo = examples.find(item => restaurant.demoKey ? item.demoKey === restaurant.demoKey : item.city === restaurant.city
    && (item.name === restaurant.name || item.legacyNames?.includes(restaurant.name)));
  return { ...restaurant, name: restaurant.demoKey ? restaurant.name : demo?.name || restaurant.name, cuisine: restaurant.cuisine || demo?.cuisine, description: demo?.description || 'Explore this restaurant and request a table for your family.', dish: demo?.dish || null };
}

export function findRestaurant(restaurants, preferred) {
  if (!preferred) return null;
  return restaurants.find(item => preferred.id != null
    ? item.id === preferred.id
    : item.name === preferred.name && item.city === preferred.city) || null;
}

export function filterRestaurants(restaurants, { category, city, query }) {
  const search = query.trim().toLowerCase();
  return restaurants.filter(item => (category === 'All restaurants' || item.dish?.category === category)
    && (city === 'All cities' || item.city === city)
    && (!search || [item.name, item.city, item.cuisine, item.dish?.name].filter(Boolean).join(' ').toLowerCase().includes(search)));
}

export function loadRestaurantRecords(request) {
  return loadAllPages(request, '/restaurants?size=6&sort=id,asc');
}
