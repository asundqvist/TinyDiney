import React, { useEffect, useState } from 'react';
import FoodArt from './FoodArt.jsx';

export const SLIDE_INTERVAL_MS = 5000;

export default function RestaurantSlideshow({ restaurants, onExplore }) {
  const [index, setIndex] = useState(0);
  const count = restaurants.length;
  const position = count ? index % count : 0;
  const restaurant = restaurants[position];
  useEffect(() => { setIndex(0); }, [restaurants]);
  useEffect(() => {
    if (count < 2) return;
    const timer = setInterval(() => {
      if (document.visibilityState !== 'hidden') setIndex(current => (current + 1) % count);
    }, SLIDE_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [count]);

  if (!restaurant) return <div className="hero-art"><p>No restaurants to feature right now.</p></div>;
  return <section className="hero-art slideshow" aria-label="Featured restaurants" aria-roledescription="carousel">
    <div aria-live="off">
      <button className="slide-feature" aria-label={'Explore ' + restaurant.name} onClick={() => onExplore(restaurant)}>
        {restaurant.dish ? <FoodArt className="hero-plate" type={restaurant.dish.type} /> : <span className="restaurant-placeholder">{restaurant.cuisine}</span>}
        <span className="slide-name">{restaurant.name}</span><span className="slide-caption">{restaurant.dish ? restaurant.dish.name + ' · ' : ''}{restaurant.city}</span>
      </button>
    </div>
  </section>;
}
