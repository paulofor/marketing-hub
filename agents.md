# agents.md — Contrato operacional

> Consulte este arquivo antes de alterar qualquer módulo. Para todas as regras canônicas, a fonte de verdade é `docs/canonical/system-governance-canon.v2.md`.

## 1. Fontes de verdade

- **Governança**: System Governance Canon (precedência, ownership e critérios de novos cânones).
- **Modelo de dados**: `docs/modelo-dados-experimento.md`. Alterou entidades ou relacionamentos? Atualize o documento imediatamente.
- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.
- **DRIs**: mudanças cross-domain precisam de ADR registrado no diretório correspondente.

## 2. Build & Test essenciais

| Módulo | Build | Testes | Observações |
| --- | --- | --- | --- |
| `backend/ads-service` | `cd backend/ads-service && mvn -s ../settings.xml deploy` | `mvn -s ../settings.xml test` | Publica artefato consumido pelo `ai-worker` (GitHub Packages `paulofor/ads-service`). |
| `ai-worker` | `cd ai-worker && mvn -s settings.xml package` | `mvn -s settings.xml test` | Baixa o artefato do `ads-service` publicado. |
| `frontend` (MarketingHub) | `cd frontend && npm install && npm run build` | `npm run test` | Confirme URLs de backend antes de subir novas chamadas. |
| Outros serviços (lead-portal, email-service, workers específicos) | Consulte o README local antes de subir pipelines. |  | Alinhe com o backend antes de tocar contratos compartilhados.

## 3. Convenções de engenharia

- **Tecnologias padrão**: Java 21 + Spring Boot 3, React 18 + Vite + TypeScript, Zustand para state, TanStack Query para dados. Formatação: Spotless (backend) e Prettier (frontend).
- **Banco**: MySQL 5.7. Somente o backend acessa o banco; demais módulos conversam via APIs do backend. Prefira filtros no SQL ao invés de pós-processar em memória.
- **Modelo único**: entidades residem no backend. O `ai-worker` reutiliza o modelo do backend; não mantenha cópias. Toda consulta ou manipulação de dados nasce no backend e é exposta via contrato explícito.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Worker IA**: qualquer campo que acione serviços de IA precisa de tooltip explicando o efeito. Registros criados pelo worker devem persistir `modelo` e `prompt` no momento da criação.
- **Publicação de imagens**: siga o pipeline imutável (validação no frontend, otimização no `CreativeImageOptimizer`, upload via `POST /api/assets`). Novos fluxos devem reutilizar ou estender esse pipeline.

## 4. Módulos e responsabilidades

- **MarketingHub Backend / Frontend**: camada administrativa e UI principal do sistema.
- **Facebook Ads Worker**: integração com a API da Meta para campanhas e públicos.
- **Worker AI**: integrações com modelos OpenAI para geração e otimização de ativos.
- **Lead Portal (backend/frontend)**: experiência dedicada aos leads após anúncios.
- **Lead Portal Payments Service**: pagamentos via Mercado Pago.
- **Email Service**: envio transacional integrado ao Amazon SES.
- **Image Watermark Service**: gera marcas-d'água para prévias.
- **Image Zipper Service**: monta e distribui pacotes de produtos/amostras.

Documente qualquer alteração cross-módulo no cânone correspondente e sincronize contratos antes de integrar.

## 5. Segurança e secrets

- Nunca commite `.env` ou credenciais. Use GitHub Actions secrets.
- Revise variáveis sensíveis nos pipelines antes de publicar artefatos.
