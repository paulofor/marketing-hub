# Registros de loops operacionais — Experimentos

> Documento auxiliar de prevenção de recorrência.
>
> Objetivo: registrar pontos em que o Marketing Hub entrou ou pode entrar em ciclos repetidos de correção, retrabalho ou diagnóstico incompleto.
>
> Fonte inicial: análise do histórico `docs/registros/experimentos.md` em 2026-06-17.
>
> Uso obrigatório recomendado: antes de corrigir problema em GeraLanding, Facebook Ads, Lead Portal, OpenAI/schema, pipelines administrativos ou pipeline de hipótese, verificar se a solicitação reabre algum loop listado aqui.

## Regra operacional de uso

Antes de implementar uma correção em tema com histórico de loop:

1. Identificar se o problema pertence a algum `LOOP-*` deste documento.
2. Se pertencer, corrigir a causa-raiz sistêmica, não apenas o sintoma atual.
3. Atualizar ou criar teste de contrato que prove que o loop foi fechado.
4. Atualizar cânone, Swagger, tela ou Worker AI quando o contrato entre módulos mudar.
5. Registrar no documento de tema correspondente o que foi feito e, quando necessário, atualizar este arquivo.

## Classificação

- **CRÍTICO**: envolve gasto real, publicação externa, campanha, Meta Ads, landing pública, submissão ou dados comerciais.
- **ALTO**: bloqueia geração de landing, pipeline, OpenAI, qualidade comercial ou publicação.
- **MÉDIO**: causa retrabalho arquitetural, ruído visual, testes quebrados ou divergência documental.
- **BAIXO**: melhoria de governança sem impacto operacional imediato.

## Índice dos loops identificados

| Loop | Severidade | Status inicial | Tema |
| --- | --- | --- | --- |
| `LOOP-FB-PUBLICATION` | CRÍTICO | Aberto/recorrente | Publicação Facebook Ads |
| `LOOP-GL-PUBLICATION-LEADPORTAL` | CRÍTICO | Estabilizado com risco | GeraLanding → Lead Portal |
| `LOOP-OPENAI-SCHEMA-CONTRACT` | ALTO | Recorrente | Prompts, schemas e Structured Outputs |
| `LOOP-GL-ARCHITECTURE-STAGES` | ALTO | Parcialmente estabilizado | Arquitetura por etapas |
| `LOOP-GL-AUTOMATION-CHAIN` | ALTO | Recorrente | Encadeamento automático de etapas |
| `LOOP-QUALITY-REVIEW-VISION` | ALTO | Parcialmente estabilizado | Quality Review visual |
| `LOOP-LANDING-ANALYTICS-FUNNEL` | CRÍTICO | Recorrente | Analytics, funil e submissão |
| `LOOP-PIPELINE-ADMIN-CONTRACT` | MÉDIO | Estabilizado com risco | Tela `/pipelines` e contrato persistente |
| `LOOP-HYPOTHESIS-PIPELINE` | ALTO | Em formação | Pipeline Dor → Resultado → Mecanismo → Prova → Oferta |
| `LOOP-ARTIFACT-CONTAMINATION` | ALTO | Estabilizado com risco | Metadado técnico em artefato final |
| `LOOP-COST-MODEL-AUDIT` | MÉDIO | Em observação | Custos OpenAI e modelo por etapa |

---

## LOOP-FB-PUBLICATION — Publicação Facebook Ads

- **Severidade**: CRÍTICO.
- **Status**: aberto/recorrente.
- **Sintomas recorrentes**:
  - experimento volta várias vezes para a fila de publicação;
  - campanha duplicada na Meta;
  - publicação marcada como `FAILED` antes de criar campanha/ad set/anúncio;
  - erro da Meta muda a cada tentativa;
  - público aprovado existe na UI, mas o Worker não consegue materializar targeting;
  - payload enviado à Meta diverge do contrato canônico.
- **Causa-raiz sistêmica provável**:
  - publicação dependia de contratos grandes ou genéricos demais;
  - o mesmo endpoint era usado para listar fila e resolver targeting de experimento específico;
  - validações prévias não usavam exatamente o mesmo payload final que seria enviado para a Meta;
  - fluxo de retry não limpava completamente estados anteriores;
  - rastreabilidade por job só foi introduzida depois de vários erros.
- **Módulos envolvidos**:
  - `backend/ads-service`;
  - `facebook-ads-worker`;
  - Swagger Facebook Ads;
  - cânone de publicação Facebook Ads;
  - tela de experimento e tela `/facebook-campaigns`.
- **Contratos sensíveis**:
  - fila de experimentos prontos;
  - pacote enxuto de targeting;
  - criativos prontos;
  - upload de imagem por bytes;
  - reach estimate;
  - registro de campanha;
  - protocolo `jobid`.
- **Fechamento mínimo do loop**:
  - etapa de dry-run antes de criar qualquer objeto na Meta;
  - payload final de campaign/adset/creative/ad validado e registrado antes do envio;
  - `publicationJobId` obrigatório em todos os passos;
  - teste cobrindo retry de experimento `FAILED` com targeting aprovado;
  - teste garantindo que ausência de `users_lower_bound`/`users_upper_bound` em `reachestimate` é alerta, não falha automática;
  - teste garantindo orçamento por Ad Set e `is_adset_budget_sharing_enabled=false` em campanha sem orçamento.
- **Regra preventiva**:
  - nunca corrigir publicação Facebook apenas pelo erro atual da Meta; comparar payload esperado, payload enviado, resposta, estado do experimento, campanha persistida e job steps.

## LOOP-GL-PUBLICATION-LEADPORTAL — GeraLanding → Lead Portal

- **Severidade**: CRÍTICO.
- **Status**: estabilizado com risco de regressão.
- **Sintomas recorrentes**:
  - aprovação/publicação da landing quebra por rota legada;
  - `customFormHtml` rejeitado;
  - HTML final contém metadado técnico;
  - backend e Lead Portal discordam sobre contrato do payload;
  - frontend habilita botão sem usar a mesma fonte de verdade do backend;
  - landing publicada sem submissão, tracking ou URL standalone correta.
- **Causa-raiz sistêmica provável**:
  - confusão entre HTML fonte, HTML provisório, HTML puro, HTML publicável e HTML salvo no Lead Portal;
  - coexistência de endpoints legados e novos;
  - injeção de pixel, tracking, analytics e submissão em momentos diferentes do fluxo.
- **Campos sensíveis**:
  - `html_geralanding`: HTML/CSS puro gerado pelo GeraLanding;
  - `landing_page_html`: HTML publicável, enriquecido com scripts/pixel/tracking/submissão;
  - `follow_up_action_url`: destino oficial de campanha;
  - `customFormHtml`: contrato enviado ao Lead Portal.
- **Fechamento mínimo do loop**:
  - teste ponta a ponta de aprovação: `html_geralanding` puro → injeções idempotentes → publicação Lead Portal → `follow_up_action_url` salvo;
  - bloqueio explícito de `legacyPreviewHtml`, `renderMode` e comentários técnicos no contrato final;
  - Swagger do Lead Portal e Swagger GeraLanding sincronizados.
- **Regra preventiva**:
  - não alterar publicação de landing sem declarar qual artefato está sendo lido, qual está sendo enriquecido e qual está sendo publicado.

## LOOP-OPENAI-SCHEMA-CONTRACT — Prompts, schemas e Structured Outputs

- **Severidade**: ALTO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - OpenAI retorna JSON com formato diferente do parser;
  - payload vem em Markdown code fence;
  - JSON duplicado/concatenado;
  - JSON escapado dentro de string;
  - schema aceito localmente, mas rejeitado pela Responses API;
  - frontend considera variações disponíveis e backend/worker não encontra candidatos;
  - totalizadores zerados apesar de artefato salvo.
- **Causa-raiz sistêmica provável**:
  - prompt, schema, parser backend, Worker AI e frontend não evoluíam como um único contrato versionado.
- **Etapas mais afetadas**:
  - `landing-page-wireframe`;
  - `landing-page-copy`;
  - `landing-page-image-planning`;
  - `landing-page-design-preset`;
  - `campaign-angle`;
  - `ad-copy`;
  - `ad-image-briefing`.
- **Fechamento mínimo do loop**:
  - teste de compatibilidade do schema com Structured Outputs estrito;
  - golden JSON por etapa;
  - teste do consumer backend processando o golden JSON;
  - teste do Worker AI montando request final com `service_tier=flex`;
  - teste de frontend somente para aquilo que o backend também consegue extrair.
- **Regra preventiva**:
  - todo ajuste em prompt deve responder: o schema aceita, a OpenAI aceita, o backend consome, a UI interpreta e o relatório consegue auditar?

## LOOP-GL-ARCHITECTURE-STAGES — Arquitetura por etapas do GeraLanding

- **Severidade**: ALTO.
- **Status**: parcialmente estabilizado.
- **Sintomas recorrentes**:
  - mover controller para pacote de etapa quebra teste MVC;
  - service da etapa depende de service transversal;
  - DTO fica em pacote genérico e viola ArchUnit;
  - Worker AI consome endpoint genérico quando o contrato exige endpoint por etapa;
  - classe adaptadora é criada e depois removida por pouca responsabilidade;
  - regra ArchUnit precisa ser ajustada repetidamente.
- **Causa-raiz sistêmica provável**:
  - o padrão por etapa foi descoberto durante a implementação, não aplicado como template fechado desde o início.
- **Template mínimo por etapa backend**:
  - `Backend<Etapa>Controller`;
  - `Backend<Etapa>Service`;
  - subpacotes `pending`, `recebePrompt`, `recebeResposta`, `listStageExecutions`, `detailStageExecution`;
  - endpoints públicos de start/list/detail quando aplicável;
  - endpoints internos `pending`, `recebe-prompt`, `recebe-resposta`;
  - Swagger atualizado;
  - testes de controller e service.
- **Template mínimo por etapa Worker AI**:
  - `openai.core.<etapa>`;
  - scheduler;
  - backend client;
  - prompt builder;
  - validator;
  - handler;
  - properties;
  - configuration;
  - testes de request e callback.
- **Regra preventiva**:
  - não criar etapa nova apenas copiando a etapa anterior; preencher o checklist de arquitetura antes de codificar.

## LOOP-GL-AUTOMATION-CHAIN — Encadeamento automático de etapas

- **Severidade**: ALTO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - etapa conclui e não dispara a próxima;
  - etapa dispara algo de outro fluxo sem intenção do usuário;
  - automação de anúncio conflita com automação do GeraLanding;
  - botão manual continua aparecendo em experimento publicado;
  - reexecução mantém artefatos antigos incompatíveis.
- **Causa-raiz sistêmica provável**:
  - automação era tratada como comportamento local da etapa, não como contrato de estado do pipeline.
- **Encadeamentos sensíveis**:
  - Wireframe → Copy;
  - Copy → Prompt Imagem;
  - Prompt Imagem → Gera Imagem;
  - Gera Imagem → Preset Design;
  - Preset Design → Quality Review;
  - AD_IMAGE_BRIEFING → geração de criativos de anúncio, quando explicitamente solicitado.
- **Fechamento mínimo do loop**:
  - cada etapa declarar `manual`, `auto`, `retry`, `disabled-after-publication`;
  - teste de sucesso cria próxima etapa;
  - teste de falha não cria próxima etapa;
  - teste de reexecução limpa artefatos dependentes quando necessário.
- **Regra preventiva**:
  - antes de ativar automação, declarar qual etapa anterior autoriza, qual próxima etapa nasce e quais campos serão limpos ou preservados.

## LOOP-QUALITY-REVIEW-VISION — Quality Review visual

- **Severidade**: ALTO.
- **Status**: parcialmente estabilizado.
- **Sintomas recorrentes**:
  - revisão avalia imagem solta em vez da landing renderizada;
  - pixel/script é enviado como imagem;
  - screenshot falha por timeout;
  - desktop e mobile têm prioridades confusas;
  - execuções diferentes avaliam evidência igual, mas decisões divergem;
  - prompt textual fica longo demais e compete com a evidência visual.
- **Causa-raiz sistêmica provável**:
  - a evidência visual canônica não estava fechada desde o começo.
- **Contrato recomendado**:
  - fonte única: `html_geralanding`;
  - renderização em browser/headless;
  - screenshot mobile obrigatório;
  - screenshot desktop complementar;
  - hash de HTML, request e screenshots;
  - modelo de visão dedicado;
  - prompt curto e visual;
  - resposta com score, bloqueios, recomendação de publicação e etapa sugerida para regeneração.
- **Fechamento mínimo do loop**:
  - teste garantindo que o request usa screenshots renderizados, não imagens extraídas do HTML;
  - teste de auditoria com hashes;
  - UI exibindo evidências visuais usadas na decisão.
- **Regra preventiva**:
  - nenhuma decisão de Quality Review deve ser analisada sem conferir qual screenshot/hash foi avaliado.

## LOOP-LANDING-ANALYTICS-FUNNEL — Analytics, funil e submissão

- **Severidade**: CRÍTICO.
- **Status**: recorrente.
- **Sintomas recorrentes**:
  - navegador indica envio, mas funil aparece zerado;
  - evento existe no banco, mas resumo não conta;
  - submissão cai em endpoint inexistente no domínio público;
  - reset do funil quebra por FK ou não limpa analytics normalizado;
  - landing antiga carrega script antigo sem debug;
  - `visitorId`, `sessionId`, device e OS entram em momentos diferentes do contrato.
- **Causa-raiz sistêmica provável**:
  - produção, persistência, normalização e consumo de eventos evoluíram separadamente.
- **Contratos sensíveis**:
  - `experiment_funnel_event`;
  - `experiment_landing_analytics_event`;
  - `source=landing-page-analytics`;
  - `visitorId` provável;
  - `sessionId`;
  - `eventType`;
  - `page_view`, `section_view_time`, `ENVIO_FORM`;
  - deduplicação de `page_view` em 3 segundos.
- **Fechamento mínimo do loop**:
  - registry de eventos com fonte, payload, tabela bruta, tabela normalizada e query de resumo;
  - teste: evento enviado pelo endpoint público aparece no funil e na aba Analytics;
  - teste: reset apaga normalizados antes dos brutos;
  - teste: submissão pública soma `ENVIO_FORM` sem duplicar.
- **Regra preventiva**:
  - todo novo evento de landing só está pronto quando aparecer na UI que o usuário usa para decisão.

## LOOP-PIPELINE-ADMIN-CONTRACT — Tela `/pipelines` e contrato persistente

- **Severidade**: MÉDIO.
- **Status**: estabilizado com risco.
- **Sintomas recorrentes**:
  - tela permite criar/editar estrutura que deveria ser canônica;
  - etapa oficial ausente no banco;
  - etapa extra quebra diagnóstico;
  - Liquibase falha por posição duplicada;
  - definição e configuração operacional ficam misturadas;
  - modelo OpenAI configurado não aparece na etapa operacional.
- **Causa-raiz sistêmica provável**:
  - CRUD livre foi usado para dados que são contrato oficial de execução.
- **Fechamento mínimo do loop**:
  - tela só edita configuração operacional;
  - definição oficial vem do registry/cânone/sincronizador;
  - rebuild destrutivo exige confirmação explícita;
  - changelogs de posição usam faixa temporária para evitar unique conflict;
  - endpoint de metadados mostra implementação real por etapa.
- **Regra preventiva**:
  - pipeline oficial não é cadastro livre; é contrato sincronizado com campos operacionais editáveis.

## LOOP-HYPOTHESIS-PIPELINE — Pipeline de hipótese

- **Severidade**: ALTO.
- **Status**: em formação.
- **Sintomas recorrentes**:
  - etapa aparece fora de ordem;
  - Oferta executa sem Prova;
  - Worker AI não possui etapa correspondente;
  - job fica preso em `INICIADO`, `PROCESSANDO` ou `AGUARDANDO_RETORNO_OPENAI`;
  - fechamento da hipótese fica dentro da etapa Dor;
  - campo de banco não comporta resposta completa;
  - custo e relatório auditável entram depois da execução.
- **Causa-raiz sistêmica provável**:
  - o pipeline foi crescendo etapa por etapa, sem matriz inicial completa do fluxo Dor → Resultado → Mecanismo → Prova → Oferta → Fechamento.
- **Fechamento mínimo do loop**:
  - cada etapa declarar pré-requisito, próximo passo, campo final, prompt, schema, worker, endpoint e relatório;
  - Oferta exige Prova concluída tanto no start quanto no pending;
  - finalização da hipótese fica em service próprio;
  - lease operacional para jobs presos;
  - `raw_response`, request, prompt e custo persistidos por etapa.
- **Regra preventiva**:
  - não adicionar etapa na tela sem backend, worker, Swagger, lease, custo e pré-requisito equivalente.

## LOOP-ARTIFACT-CONTAMINATION — Metadado técnico em artefato final

- **Severidade**: ALTO.
- **Status**: estabilizado com risco.
- **Sintomas recorrentes**:
  - HTML final recebe comentário `AUTO`;
  - título técnico aparece na landing;
  - payload final inclui campo legado ou de debug;
  - JSON técnico fica serializado dentro de campo textual;
  - Quality Review aponta metadado visível ou aparência provisória.
- **Causa-raiz sistêmica provável**:
  - metadados de execução foram misturados com artefatos publicáveis.
- **Campos/artefatos sensíveis**:
  - HTML final;
  - `html_geralanding`;
  - `landing_page_html`;
  - `customFormHtml`;
  - JSON final de etapa;
  - criativo aprovado;
  - relatório público.
- **Fechamento mínimo do loop**:
  - whitelist de DTO final;
  - teste de ausência de comentários técnicos;
  - separação explícita entre auditoria e artefato final;
  - Quality Review deve apontar contaminação como bloqueio.
- **Regra preventiva**:
  - todo metadado técnico deve ir para tabela/campo de auditoria, nunca para conteúdo publicável.

## LOOP-COST-MODEL-AUDIT — Custos OpenAI e modelo por etapa

- **Severidade**: MÉDIO.
- **Status**: em observação.
- **Sintomas recorrentes**:
  - custo aparece `$0.00` apesar de tokens retornados;
  - modelo da etapa não aparece na tela;
  - request auditado não mostra `service_tier=flex`;
  - Worker AI usa preço hardcoded ou propriedade zerada;
  - modelo configurado em `/pipelines` não chega à execução.
- **Causa-raiz sistêmica provável**:
  - seleção de modelo, modo de preço, catálogo de preço e cálculo de custo ficavam em fontes diferentes.
- **Fechamento mínimo do loop**:
  - modelo por etapa vindo do pipeline/catálogo;
  - fallback default explícito por tipo de artefato;
  - custo calculado via backend/catalogo `openai_model`;
  - request auditável sempre com `service_tier=flex`;
  - UI mostra modelo, modo, preço e custo acumulado.
- **Regra preventiva**:
  - nenhuma etapa OpenAI deve persistir execução sem modelo efetivo, tokens e regra de preço identificável.

---

## Checklist rápido antes de corrigir problema recorrente

Use este checklist quando o problema estiver em algum loop acima:

```md
- O problema reabre qual LOOP-*?
- Qual contrato está divergindo?
- Qual módulo é dono da correção?
- O frontend, backend, worker, Swagger e cânone estão alinhados?
- Existe teste que reproduz a falha atual?
- Existe teste que impede o mesmo loop de voltar?
- O registro operacional foi atualizado no documento do tema?
```

## Sugestão de melhoria para `AGENTS.md`

Adicionar nas convenções de engenharia, logo após “Revisão obrigatória após correção de problema”:

```md
- **Consulta obrigatória de loops conhecidos**: antes de corrigir problema recorrente ou investigar falha em GeraLanding, Facebook Ads, Lead Portal, OpenAI/schema, pipelines administrativos ou pipeline de hipótese, consultar `docs/registros/loops.md`. Se o problema corresponder a um `LOOP-*`, a correção deve fechar a causa-raiz sistêmica, atualizar o teste de contrato que previne recorrência e registrar no documento de tema correspondente.
```

## Registro inicial

## 2026-06-17 00:01:07 UTC-3
- solicitação: criar um arquivo de registro de loops operacionais a partir da análise de `docs/registros/experimentos.md` e revisar o `AGENTS.md` para melhoria preventiva.
- causa-raiz observada: o histórico mostra recorrência de problemas por contratos instáveis entre frontend, backend, workers, OpenAI, Lead Portal e Meta Ads.
- registro do que foi feito: criado este documento com os principais loops, causa-raiz sistêmica, fechamento mínimo e regra preventiva por tema; incluída sugestão objetiva de melhoria para o `AGENTS.md`.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/registros/experimentos.md
