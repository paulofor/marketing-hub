# AGENTS.md — Contract for Codex agent

## Build & Test
- **Backend**
  - Build & publish: `cd backend/ads-service && mvn -s ../settings.xml deploy`
  - Tests: `cd backend/ads-service && mvn -s ../settings.xml test`
- **Success Product Worker**
  - Build: `cd success-product-worker && mvn -s settings.xml package`
  - Tests: `cd success-product-worker && mvn -s settings.xml test`
  - Downloads the `ads-service` artifact from GitHub Packages.
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
