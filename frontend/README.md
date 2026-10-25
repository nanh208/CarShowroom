# ARES storefront

This directory contains the complete Vue frontend transferred from
`CarShowroom-copy/frontend`, including customer and administration pages,
public images, models, styles, and browser tests.

## Run locally

Use Node.js 20 or newer:

```sh
npm ci
npm run dev
```

Vite serves the interface at http://localhost:5173 and forwards `/api`,
`/uploads`, and websocket requests to http://localhost:8080.
For a different backend, copy `.env.example` to `.env` and set `VITE_API_URL`
to its origin. Restart Vite after changing environment variables.

## Production build

```sh
npm run build
npm run preview
```

Deploy `dist/` with a fallback to `index.html` for Vue Router paths.
Serve `/api` and `/uploads` from the backend, rather than the SPA fallback.

## Integration status

The imported frontend expects the API contract of the source project's
Spring backend (including `/api/products`, authentication, checkout and admin
endpoints). The existing backend in this repository still exposes the older
`/api/cars` contract. Importing the interface does not migrate those endpoints;
backend integration remains separate work. The first read-only bridge,
`GET /api/products/{id}`, now returns `product`, `related`, `variants`, and
`flash` from the existing car catalog, with HTTP 404 for unknown IDs. The
legacy model only stores stock availability, so this endpoint exposes
`inStock` and deliberately does not invent a numeric `stock` value. The Vue
purchase flow still needs an inventory contract, and the product list,
reviews, questions and other source-backend endpoints remain to be integrated.
No source backend, database, uploaded
customer files, local secrets or dependency folders are included in this import.
