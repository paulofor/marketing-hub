# AGENTS.md — Contract for Codex agent

## Build & Test
- **Backend**
  - Build: `./gradlew build`
  - Tests: `./gradlew test`
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
