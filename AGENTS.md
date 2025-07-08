# AGENTS.md — Contract for Codex agent

## Build & Test
- **Backend**
  - Build: `cd backend/ads-service && mvn package`
  - Tests: `cd backend/ads-service && mvn test`
- **Success Product Worker**
  - Build: `cd success-product-worker && mvn package`
  - Tests: `cd success-product-worker && mvn test`
  - Requires the `ads-service` module installed locally with `mvn install`.
- **Frontend**
  - Build: `npm run build`
  - Tests: `npm run test`

## Conventions
- Java 21 + Spring Boot 3
- React 18 + Vite + TypeScript
- Zustand for state, TanStack Query for data fetching
- Prettier (frontend) and Spotless (backend) for formatting

## Secrets
- Do **NOT** commit `.env`. Use GitHub Actions secrets for tokens.
