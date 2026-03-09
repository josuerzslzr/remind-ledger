# ADR-002: Frontend Framework, Routing, and Styling

## Status

Accepted

## Context

RemindLedger needs a web frontend for reminder management and in-app notification
display. The application is fully authenticated — there are no public,
SEO-critical pages. The backend is a separate Spring Boot API (ADR-001); the
frontend consumes it via HTTP from the browser.

Given these characteristics, the frontend is a **Single Page Application (SPA)**:
all rendering happens client-side, and the build output is a set of static files
(HTML, JS, CSS) served from a CDN. Server-side rendering adds complexity without
benefit for an authenticated app with no public content.

A routing library and a styling approach are decided alongside the framework
because both shape the project structure and every component from day one.

## Decision

### Framework
**React + Vite + TypeScript.**

Vite is the standard build tool for new React SPAs, replacing the deprecated
Create React App. It provides fast dev-server startup (native ES modules), hot
module replacement, and optimised production builds to static files.

### Routing
**React Router (v6+).**

React Router is the most widely adopted client-side routing library in the React
ecosystem. It provides declarative route definitions, nested layouts, and
loader/action patterns for data-aware routing.

### Styling
**Tailwind CSS.**

Tailwind is the dominant utility-first CSS framework in the React ecosystem.
Vite has first-class PostCSS support, making Tailwind integration zero-friction.
Utility classes are co-located with markup, eliminating the need for separate
stylesheet files and CSS naming conventions.

## Alternatives considered

### Framework

| Option | Reason rejected |
|--------|----------------|
| Next.js (App Router) | Full-stack React framework with SSR and React Server Components. Not selected because RemindLedger is fully authenticated with no public pages — SSR provides no SEO or performance benefit. RSC adds significant complexity (server/client component boundary, hydration mismatches, evolving caching behaviour). The Next.js server would also become a middleman between the browser and the Spring Boot API, adding latency. Kept as a candidate for a future project that benefits from SSR. |
| Next.js (Pages Router) | Soft-deprecated in favour of App Router; not the current standard for new projects. |
| Remix | Direct Next.js competitor; smaller ecosystem and community. Same SSR overhead concern applies. |
| Angular | Opinionated full framework with higher setup overhead; React ecosystem is more prevalent for new web projects. |
| Plain React + Webpack | Webpack requires significantly more configuration than Vite for equivalent functionality. Vite is the modern default. |

### Routing

| Option | Reason rejected |
|--------|----------------|
| TanStack Router | Fully type-safe, file-based routing option, growing adoption. Deferred to Phase 2 — React Router is the more established and widely documented choice, with broader ecosystem support. |

### Styling

| Option | Reason rejected |
|--------|----------------|
| CSS Modules | Each component requires a separate `.module.css` file; design tokens must be managed manually; less ecosystem alignment with popular React component libraries that assume Tailwind. |
| Styled Components / Emotion (CSS-in-JS) | Runtime overhead; declining adoption in the React ecosystem. |
| Plain CSS / Sass | No design-token system or utility classes out of the box; requires more manual effort to keep styling consistent across components. |

## Consequences

- The app builds to static files (`dist/`); no Node.js server runtime is required in production.
- Frontend is hosted on **S3 + CloudFront** instead of an ECS container (see ADR-005), reducing cost and operational complexity.
- All rendering and routing happen client-side; the browser fetches data directly from the Spring Boot API via HTTP.
- SPA routing requires a CloudFront custom error response (404 → `/index.html` with 200 status) so that deep links and browser refreshes work correctly.
- Client-side state management (TanStack Query, Zustand) is deferred; added when the need arises during development.
- Component library (e.g. shadcn/ui, Radix) is deferred to implementation; Tailwind ensures compatibility with the most common options when the time comes.
- Tailwind configuration (`tailwind.config.ts`) defines the project's design tokens (colours, spacing, typography) in one place.
- TanStack Router is deferred to Phase 2; React Router covers Phase 1 routing needs.

