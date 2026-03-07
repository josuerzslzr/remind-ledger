# ADR-002: Frontend Framework

## Status

Accepted

## Context

RemindLedger needs a web frontend for reminder management and in-app notification
display. The frontend must support server-side rendering, file-based routing, and
a clean developer experience compatible with a TypeScript-first fullstack stack.

## Decision

**Next.js (App Router) + TypeScript.**

App Router (introduced in Next.js 13, now the recommended default) is used over
the legacy Pages Router. TypeScript is the default and expected standard for new
Next.js projects.

## Alternatives considered


| Option                   | Reason rejected                                                                                                                            |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Plain React (CRA / Vite) | No routing, SSR, or file-based structure out of the box. Next.js is the standard production choice for React apps.                         |
| Next.js Pages Router     | Soft-deprecated in favour of App Router; not the current standard for new projects.                                                        |
| Remix                    | Direct Next.js competitor; smaller ecosystem and community. Next.js is the dominant choice for React-based fullstack in the German market. |
| Angular                  | Opinionated full framework with higher setup overhead; React/Next.js ecosystem is more prevalent for new web projects.                     |


## Consequences

- App Router uses React Server Components (RSC); components run on the server by default unless marked `"use client"`.
- Data fetching in Server Components uses native `fetch()` with built-in caching — no client-side library needed for server-rendered data.
- Client-side state management (TanStack Query, Zustand) and styling (Tailwind CSS) are deferred; added gradually during development once the need is felt organically.
- CSS Modules (built into Next.js) used for styling in Phase 1.

