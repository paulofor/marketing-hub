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
- Sempre que crir uma nova chamado ao backend verifique se ela existe. Se não existir entenda se faz sentido e se sim construa no backend o endpoint documentando ele.
  caso positivo, o endpoint correspondente deve ser implementado.
- Campos de formulário que acionam serviços do Worker IA devem incluir um tooltip explicativo
- Todo registro de entidade produzido por um processo do **Worker IA** deve possuir os atributos `modelo` e `prompt`, que devem ser preenchidos no momento da criação do registro.
- Todo changelog `.sql` **DEVE** começar na **primeira linha** com: `--liquibase formatted sql`
- Para scripts específicos de MySQL, use `dbms:mysql` **na mesma linha** do changeset: `--changeset <autor>:<id> dbms:mysql`
- Use precondições para idempotência, por exemplo:
  ```
  --preconditions onFail:MARK_RAN
  --precondition-sql-check expectedResult:0 <SQL que retorna 0 quando deve executar>
  ```
- **Template mínimo sugerido**:
  ```sql
  --liquibase formatted sql
  --changeset repo:<yyyy-mm-dd>-<id> dbms:mysql
  --preconditions onFail:MARK_RAN
  --precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sua_tabela';
  -- seu SQL aqui
  ```
- **Processamento de imagens**
  - Sempre siga o fluxo adotado no Worker IA (`CreativeImageOptimizer`) e na aba **Criativos** da tela de detalhe de experimentos.
    - **Validação de entrada**: antes do upload, garanta que a imagem tenha largura mínima de 600px e forneça feedback claro ao usuário em caso de falha.
    - **Normalização**: remova o canal alfa aplicando fundo branco e limite as dimensões máximas (1024px) antes de outras otimizações.
    - **Otimização incremental**: converta para JPEG e execute uma estratégia iterativa de compressão/escala, reduzindo qualidade (0.85 até 0.45) e tamanho (100% até 50%) até ficar abaixo do orçamento configurado de bytes (padrão 900 KB) antes de enviar ao backend.
  - Novos fluxos de processamento de imagem devem reutilizar esse pipeline ou estender `CreativeImageOptimizer`, mantendo compatibilidade com o backend (`POST /api/assets`).
## Secrets

- Do **NOT** commit `.env`. Use GitHub Actions secrets for tokens.
