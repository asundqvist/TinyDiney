import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToString } from 'react-dom/server';

test('updated dishes and illustrations match both renamed restaurants and their existing API IDs', async () => {
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
  try {
    const { presentRestaurant, demoRestaurants, loadRestaurantRecords } = await server.ssrLoadModule('/frontend/catalogue.js');
    assert.equal(demoRestaurants.length, 12);
    const pages = [];
    const loaded = await loadRestaurantRecords(async path => {
      pages.push(path);
      return { content: demoRestaurants.slice((pages.length - 1) * 6, pages.length * 6), totalPages: 2 };
    });
    assert.equal(loaded.length, 12);
    assert.deepEqual(pages, ['/restaurants?size=6&sort=id,asc&page=0', '/restaurants?size=6&sort=id,asc&page=1']);
    assert.equal(new Set(demoRestaurants.map(item => item.demoKey)).size, 12);
    const renamed = presentRestaurant({ id: 20, demoKey: 'noodle-nest', name: 'New Pasta Name', city: 'New city', cuisine: 'Updated cuisine' });
    assert.equal(renamed.name, 'New Pasta Name');
    assert.equal(renamed.cuisine, 'Updated cuisine');
    assert.equal(renamed.dish.type, 'pasta');
    const { default: FoodArt } = await server.ssrLoadModule('/frontend/FoodArt.jsx');
    const cases = [
      [{ id: 3, name: 'Peas & Please', city: 'Uppsala' }, 'Peas & Please', 'fish'],
      [{ id: 5, name: 'Golden Cub Diner', city: 'Uppsala' }, 'Puddington', 'black-pudding'],
      [{ id: 6, name: 'Little Harbour', city: 'Gothenburg' }, 'Little Burger Barn', 'burger'],
    ];
    const images = [];
    for (const [stored, name, type] of cases) {
      const presented = presentRestaurant(stored);
      assert.equal(presented.id, stored.id);
      assert.equal(presented.name, name);
      assert.equal(presented.dish.type, type);
      assert.equal(presented.dish.category, 'Classics');
      assert.ok(demoRestaurants.some(item => item.name === name && item.dish.type === type));
      images.push(renderToString(React.createElement(FoodArt, { type })));
    }
    assert.equal(new Set(images).size, 3);
    // Stored names can lag behind catalogue edits until the backend restarts.
    for (const demo of demoRestaurants) {
      for (const name of [demo.name, ...(demo.legacyNames || [])]) {
        const presented = presentRestaurant({ id: 42, name, city: demo.city });
        assert.equal(presented.id, 42);
        assert.equal(presented.name, demo.name);
        assert.equal(presented.dish?.type, demo.dish.type, `Missing illustration for ${name}`);
        assert.ok(renderToString(React.createElement(FoodArt, { type: presented.dish.type })).includes('<svg'));
      }
    }
    assert.equal(presentRestaurant({ name: 'Golden Cub Diner', city: 'Another city' }).dish, null);
  } finally { await server.close(); }
});
