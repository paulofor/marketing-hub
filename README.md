# Marketing Hub

Plataforma modular orientada a artefatos para descobrir necessidades reais de mercado, estruturar hipóteses, transformar conhecimento em produtos digitais e validar comercialmente essas soluções com apoio de IA.

## Missão

O Marketing Hub existe para identificar necessidades reais de mercado e transformá-las em produtos digitais que gerem melhoria concreta na vida das pessoas e sejam comercialmente viáveis.

## Linha principal do sistema

Framework central do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

Esse eixo orienta descoberta, modelagem, geração de artefatos, construção de ofertas e validação comercial.

## Visão geral da arquitetura

O projeto é composto por múltiplos módulos, com responsabilidades separadas e contratos explícitos.

Princípios gerais:

- o **backend** é a fonte de verdade dos contratos de domínio que ele possui;
- o **frontend** e os demais módulos consomem contratos, não reinventam regra de negócio;
- workers e serviços auxiliares existem para executar fluxos, integrações e automações;
- o sistema evolui como **workflow orientado a artefatos**, com governança, lineage, versionamento e documentação;
- o banco de dados é acessado pelo backend responsável; outros módulos devem preferir APIs e contratos do sistema.

## Módulos principais

### Núcleo da plataforma

- `backend/ads-service`  
  Backend principal do Marketing Hub. Centraliza domínio, contratos, APIs administrativas e integração entre módulos.

- `frontend`  
  Interface principal do sistema.

- `ai-worker`  
  Worker de IA para geração, transformação e automação baseada em modelos.

### Módulos de pesquisa, descoberta e inteligência

- `oprm`  
  Apoia a compreensão de rotina, dores e oportunidades reais do cliente ou ocupação.

- `mds`  
  Apoia a descoberta de mecanismos plausíveis e sua tradução em mecanismos de produto.

- `market-research-service`  
  Serviço de apoio à pesquisa de mercado.

- `mcp-server`  
  Serviço auxiliar para consultas e operações suportadas via MCP.

### Módulos de operação, entrega e ativos

- `facebook-ads-worker`  
  Integração com publicidade e operações ligadas à Meta.

- `video-management-service`  
  Gestão de fluxos e operações relacionadas a vídeo.

- `email-service`  
  Renderização/disparo de e-mails e suporte a templates do ecossistema.

- `lead-portal`  
  Experiência do lead.

- `lead-portal-payments-service`  
  Serviço de pagamentos do portal de leads.

- `institutional-site`  
  Site institucional e páginas corporativas.

- `image-watermark-service`  
  Serviço de marca d’água.

- `image-zipper-service`  
  Serviço de empacotamento de imagens.

## Estrutura do repositório

```text
.github/                     workflows e automações
backend/                     backend principal
frontend/                    aplicação web principal
ai-worker/                   worker de IA
oprm/                        módulo de rotina/persona/ocupação
mds/                         módulo de mechanism discovery
market-research-service/     pesquisa de mercado
facebook-ads-worker/         integrações com anúncios
email-service/               envio e renderização de e-mails
lead-portal/                 portal do lead
lead-portal-payments-service/ pagamentos
video-management-service/    gestão de vídeo
institutional-site/          site institucional
deploy/                      arquivos de deploy em containers
docs/                        documentação e cânones
AGENTS.md                    contrato operacional principal
docker-compose.yml           compose raiz para serviços auxiliares
