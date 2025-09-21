# AGENTS.md — Contract for Codex agent

> Sempre consulte este arquivo antes de realizar qualquer alteração no repositório.

## Build & Test

- **Backend**
  - Build & publish: `cd backend/ads-service && mvn -s ../settings.xml deploy`
  - Publicar o pacote para ser usado pelo **AI Worker** no GitHub Packages repository.
  - Tests: `cd backend/ads-service && mvn -s ../settings.xml test`
  - **AI Worker**
    - Build: `cd ai-worker && mvn -s settings.xml package`
    - Tests: `cd ai-worker && mvn -s settings.xml test`
    - Downloads the `ads-service` artifact from the `paulofor/ads-service` GitHub Packages repository.
- **Frontend**
  - Build: `npm run build`
  - Tests: `npm run test`

## Conventions

- Na pasta `manual do usuario`, todos os links devem abrir em uma nova aba utilizando `target="_blank"` para manter o usuário no documento.
- DataBase: MySql 5
- Modelo de Dados atualizado: docs/data-model.md
  - Todo o modelo de dados deve permanecer no projeto **backend**. O projeto
    **ai-worker** deve reutilizar esse modelo e não manter uma cópia
    própria.
  - Todos os métodos de consulta e manipulação de banco de dados deve ser códificado
    no projeto **backend** e pode ser utilizado no projeto **ai-worker**
- Sempre prefira fazer filtros na consulta ao banco de dados, evite buscar muitos registros
  para tratamento em memória.
- Java 21 + Spring Boot 3
- React 18 + Vite + TypeScript
- Zustand for state, TanStack Query for data fetching
- Prettier (frontend) and Spotless (backend) for formatting
- Sempre que ocorrer um erro HTTP 404 em chamada ao backend, verificar se a chamada faz sentido;
  caso positivo, o endpoint correspondente deve ser implementado.
- Campos de formulário que acionam serviços do Worker IA devem incluir um tooltip explicativo
- Todo registro de entidade produzido por um processo do **Worker IA** deve possuir os atributos `modelo` e `prompt`, que devem ser preenchidos no momento da criação do registro.

## Secrets

- Do **NOT** commit `.env`. Use GitHub Actions secrets for tokens.
