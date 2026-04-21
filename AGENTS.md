# AGNETS.md — Contrato operacional

## 1. Fontes de verdade

- **Regras**: docs/canonical/system-governance-canon.v2.md sempre leia e atualize se necessário.
- **Artefatos**: docs/canonical/modelo-canonico-artefatos-pipeline-experimento.md sempre leia e atualize se necessário.
- **Modelo de dados**: `docs/modelo-dados-experimento.md`. Alterou entidades ou relacionamentos? Atualize o documento imediatamente.
- **Liquibase / MySQL 5.7**: `docs/database/liquibase-mysql57.md`. Use sempre `databaseChangeLog` em YAML, `preConditions` com `dbms:mysql`, `splitStatements: true`, `stripComments: true` e valide mentalmente o SQL.

## 2. Convenções de engenharia

- **Servidor MCP** : Chame o endpoint MCP https://mcpserverdigi.shop/mcp via JSON-RPC
- **Tecnologias padrão**: Java 21 + Spring Boot 3, React 18 + Vite + TypeScript, Zustand para state, TanStack Query para dados. Formatação: Spotless (backend) e Prettier (frontend).
- **Banco**: MySQL 5.7. Somente o backend acessa o banco; demais módulos conversam via APIs do backend. Prefira filtros no SQL ao invés de pós-processar em memória.
- **Modelo único**: entidades residem no backend. Os demais módulos acessam o banco de dados pelo backend.
- **Fluxo entre containers**: nada de chamadas diretas entre serviços (frontend, workers, lead-portal etc). Todo tráfego passa pelo backend principal; apenas o backend fala com o banco.
- **Novos endpoints**: verifique se o contrato já existe; caso contrário, defina-o no backend, atualize a documentação e adicione testes.
- **Manual do usuário**: todos os links devem usar `target="_blank"`.
- **Frontend**: sempre que alterar o frontend crie os métodos do backend para suportar. Tanto back quanto o front estão sendo executados no mesmo host


## 3. Módulos e responsabilidades

- **MarketingHub Backend / Frontend**: camada administrativa e UI principal do sistema.
- **Facebook Ads Worker**: integração com a API da Meta para campanhas e públicos.
- **Worker AI**: integrações com modelos OpenAI para geração e otimização de ativos.
- **Lead Portal (backend/frontend)**: experiência dedicada aos leads após anúncios.
- **Lead Portal Payments Service**: pagamentos via Mercado Pago.
- **Email Service**: envio transacional integrado ao Amazon SES.
- **Image Watermark Service**: gera marcas-d'água para prévias.
- **Image Zipper Service**: monta e distribui pacotes de produtos/amostras.
- **MCP Server**: servidor de mcp, fica na pasta /mcp-server
- **OPRM** : responsavel por obter a rotina de uma determinada ocupação.

Documente qualquer alteração cross-módulo no cânone correspondente e sincronize contratos antes de integrar.

## 4. Dominios

- **oportunidadebrasil.shop** : apontando para 191.252.120.96

## 5. Segurança e secrets

- Nunca commite `.env` ou credenciais. Use GitHub Actions secrets.
- Revise variáveis sensíveis nos pipelines antes de publicar artefatos.
