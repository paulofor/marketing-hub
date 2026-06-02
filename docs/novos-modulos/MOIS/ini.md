# MOIS — índice inicial

## Objetivo

Esta pasta consolida a documentação canônica inicial do **MOIS (Market Offer Intelligence Service)** dentro do Marketing Hub.

O MOIS existe para entender **o que já é vendido no mercado**, **como essas ofertas são empacotadas**, **que promessas e provas aparecem com frequência**, **como o funil é estruturado** e **onde existem lacunas de diferenciação**.

Ele não substitui o OPRM nem o MDS:

- **OPRM** aprofunda rotina, dores, restrições e contexto operacional da ocupação/persona.
- **MOIS** observa e estrutura o que o mercado já vende e comunica.
- **MDS** aprofunda mecanismos, evidência e knowledge packs.

## Documentos desta pasta

1. `market_offer_intelligence_service_responsabilidades.md`
   - escopo canônico do módulo
   - responsabilidades, não-responsabilidades e integrações
   - posição do MOIS dentro do Marketing Hub

2. `mois_canonico_artefatos.md`
   - cânone inicial de artefatos do MOIS
   - envelope, lineage, naming e artefatos centrais
   - regras de evolução compatível

3. `mois_reuso_market_research_service.md`
   - define como o MOIS reutiliza o `market-research-service`
   - separa infraestrutura de pesquisa de domínio de ofertas
   - evita duplicação prematura de capacidades já existentes

4. `docs/swagger/openapi_mois_backend_stub.yaml`
   - stub OpenAPI inicial
   - contrato-base para o backend e consumidores internos
   - serve como ponto de partida para implementação pelo Codex

5. `mois_backend_sprint1_execucao.md`
   - nota técnica da execução da Sprint 1 no backend
   - lista endpoints stub e decisões de escopo
   - registra pendências que devem seguir para a Sprint 2

6. `mois_sprint_corretiva_a_execucao.md`
   - execução da fundação do serviço separado `mois/`
   - formaliza diretório, projeto e container próprios do módulo

7. `mois_sprint_corretiva_c_execucao.md`
   - execução da integração backend ↔ MOIS via gateway HTTP
   - consolida backend como façade institucional do contrato

8. `mois_sprint_corretiva_d_execucao.md`
   - hardening final da correção arquitetural pós-Sprint 4
   - remove legado duplicado no backend e registra ownership explícito

## Direção arquitetural resumida

- O **backend/domínio** continua sendo a fonte de verdade das regras e contratos.
- O MOIS deve operar em **workflow orientado a artefatos**.
- O módulo deve ser **genérico**, sem hardcode da oferta atual usada como exemplo em outros chats.
- O MOIS deve produzir artefatos reutilizáveis para hipóteses, ofertas, experimentos, prompts e análises comparativas.
- Toda implementação de código será feita pelo **Codex**, partindo destes documentos como contrato inicial.

## Próximo passo recomendado

Depois desta base documental, o próximo passo é produzir um **plano de implementação em sprints**, com entregáveis explícitos para backend, worker, persistência, contrato e integração com os demais módulos.
