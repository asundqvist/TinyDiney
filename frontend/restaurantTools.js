export function restaurantPayload(values) {
  const payload = {
    name: String(values.name || '').trim(), city: String(values.city || '').trim(),
    cuisine: String(values.cuisine || '').trim(), seats: Number(values.seats),
  };
  if (!payload.name || !payload.city || !payload.cuisine || !Number.isSafeInteger(payload.seats) || payload.seats < 1 || payload.seats > 2147483647) {
    throw new Error('Enter a name, city, cuisine and a positive whole number of seats.');
  }
  return payload;
}

export async function saveRestaurant(request, restaurant, values) {
  const body = JSON.stringify(restaurantPayload(values));
  return request(restaurant?.id != null ? '/restaurants/' + restaurant.id : '/restaurants', {
    method: restaurant?.id != null ? 'PUT' : 'POST', body,
  });
}
