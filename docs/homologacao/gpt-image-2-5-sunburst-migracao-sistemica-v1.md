# Homologação — migração sistêmica para GPT Image 2.5 Sunburst

Data: 2026-09-14

## Objetivo

Migrar todas as novas gerações de imagem cujo modelo é controlável pelo Marketing Hub para `gpt-image-2.5-sunburst`, preservar registros históricos, impedir downgrade em configurações e callbacks e manter auditoria de modelo, qualidade e custo.

## Alternativas comparadas

| Alternativa | Benefício | Risco | Esforço | Aderência |
|---|---|---|---|---|
| Sunburst em todas as novas gerações | Um contrato e maior capacidade visual | Maior latência que a variante rápida | Médio | Máxima; escolhida |
| Flare em todas as novas gerações | Menor latência | Não é o modelo mais capaz pedido | Baixo | Parcial |
| Sunburst final e Flare em rascunhos | Pode acelerar iterações | Dois baselines e promoção indevida de rascunho | Alto | Boa, mas prematura sem experimento |

Decisão: Sunburst com qualidade `high` como padrão. `xhigh` e `max` permanecem opções explícitas do catálogo, sem elevar custo por padrão antes de existir evidência comercial comparativa.

## Matriz ponta a ponta definida antes dos testes

| Área | Caminho feliz | Validação e falha | Integração/observabilidade | Segregação |
|---|---|---|---|---|
| Catálogo e banco | Sunburst, cinco qualidades e 15 preços | Reaplicação e rollback preservam modelos históricos | Liquibase MySQL 5.7 e include relativo | Banco efêmero exclusivo |
| Backend | Novos jobs e Responses usam Sunburst/high | Modelo antigo é rejeitado em seleção e callback | Modelo real persiste no job/asset e custo usa `usage` | Mocks e dados sem geração externa |
| AI Worker | Geração, edição, criativos, Lead Portal e GeraLanding enviam Sunburst/high | Configuração/job antigo é promovido; payload legado incompatível é removido | Request/response, job e modelo permanecem correlacionados | APIs OpenAI simuladas localmente |
| Íris/Têmis | Criação e edição enviam o contrato novo | Health bloqueia configuração antiga e callback antigo não promove asset | Auditoria redige binário, preserva hash, usage e custo | Servidor HTTP local |
| FEO, pagamentos e vídeo | Todos os pontos controláveis usam o modelo novo | Qualidade legada cai para `high`; vídeo separa orquestrador e ferramenta | Contratos e status informam o campo correto | Test doubles, sem chamada paga |
| Frontend | Resultado e histórico exibem o modelo canônico | Falha parcial continua visível sem ocultar resultado válido | Desktop, iPhone 15 Pro e Pixel 7 | APIs interceptadas no navegador |
| Configuração e CI | Defaults ativos apontam para Sunburst | Scanner falha se um modelo aposentado retornar | Workflow de contrato cobre módulos afetados | Somente leitura de arquivos versionados |

Métricas de aceite: zero configuração ativa com modelo visual aposentado; 100% dos payloads novos com modelo explícito; qualidade padrão `high`; callbacks antigos bloqueados; uma aplicação, reaplicação e rollback MySQL 5.7 íntegros; suítes relevantes e três perfis de navegador sem falhas.

## Escopo revisado

- Backend, catálogo, pipeline de imagens de framework, gerador administrativo e estúdio comercial.
- AI Worker: criativos, framework, Lead Portal e etapa de imagens do GeraLanding.
- Íris/Têmis, FEO, Agenda Cheia e preparação visual do executor de vídeo.
- Defaults de Compose, propriedades, exemplos de ambiente, workflow e contratos Swagger.
- Frontend e manuais que apresentavam o modelo anterior.

O gerador nativo do Fashion Chat pelo Codex App Server foi auditado separadamente: o protocolo atual não permite selecionar nem comprovar o modelo visual interno. Ele continua seguindo a capacidade nativa da plataforma e não é contado como execução comprovada em Sunburst. Ativos que exigirem essa comprovação devem passar pelos endpoints explícitos agora protegidos.

## Resultados

Duas rodadas locais completas e consecutivas terminaram sem falhas após a última correção:

- 3.633 testes Java por rodada: backend 2.959, AI Worker 242, Têmis 92, vídeo 223, FEO 13, Lead Portal 59 e pagamentos 45;
- 672 testes do frontend por rodada, além de TypeScript e build Vite de produção;
- aplicação, reaplicação idempotente, rollback isolado e nova aplicação do changeset em MySQL 5.7 efêmero;
- contrato estático contra modelos aposentados, ausência da estimativa financeira, colisão de variáveis e include Liquibase não relativo;
- `Spotless`, empacotamento do backend, `actionlint` do novo workflow e `git diff --check`;
- tela `/ai/image-generator` inspecionada sem overflow em desktop de 1.440 px, iPhone 15 Pro e Pixel 7 em cada rodada.

Total: 4.305 testes automatizados por rodada, 8.610 nas duas rodadas finais. Todas as integrações externas foram substituídas por test doubles; nenhuma imagem paga foi gerada, nenhum dado produtivo foi alterado e não houve PR, deploy ou publicação.

## Fontes oficiais

- [GPT Image 2.5 Sunburst](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst)
- [Guia de geração de imagens](https://developers.openai.com/api/docs/guides/image-generation)
