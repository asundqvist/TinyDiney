import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';
import App from './App.jsx';
import { requestedScopes } from './auth.js';
import './styles.css';

const authConfigured = Boolean(import.meta.env.VITE_AUTH0_DOMAIN && import.meta.env.VITE_AUTH0_CLIENT_ID);

const application = <App authConfigured={authConfigured} />;

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {authConfigured ? (
      <Auth0Provider
        domain={import.meta.env.VITE_AUTH0_DOMAIN}
        clientId={import.meta.env.VITE_AUTH0_CLIENT_ID}
        authorizationParams={{
          redirect_uri: window.location.origin,
          audience: import.meta.env.VITE_AUTH0_AUDIENCE || 'https://restaurant-api',
          scope: requestedScopes,
          ui_locales: 'en',
        }}
        onRedirectCallback={appState => {
          let fragment = '';
          if (appState?.openBooking) {
            fragment = '#boka';
            if (typeof appState.restaurant?.name === 'string' && typeof appState.restaurant?.city === 'string') {
              fragment += '?' + new URLSearchParams({ restaurant: appState.restaurant.name, city: appState.restaurant.city });
            }
          }
          window.history.replaceState({}, document.title, window.location.pathname + fragment);
        }}
      >
        {application}
      </Auth0Provider>
    ) : application}
  </StrictMode>,
);
