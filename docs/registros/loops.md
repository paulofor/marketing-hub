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
3. Verificar o bloco **O que resolveu efetivamente no histórico** para não voltar a uma solução já superada.
4. Atualizar ou criar teste de contrato que prove que o loop foi fechado.
5. Atualizar cânone, Swagger, tela ou Worker AI quando o contrato entre módulos mudar.
6. Registrar no documento de tema correspondente o que foi feito e, quando necessário, atualizar este arquivo.

## Como ler este documento

Cada loop possui dois tipos de informação:

- **Correção efetiva**: aquilo que, no histórico real do projeto, reduziu ou encerrou o ciclo de retrabalho.
- **Prevenção futura**: regra mínima para evitar que o mesmo tipo de loop volte com outro nome, outro endpoint ou outra etapa.

Quando houver divergência entre tentativa antiga e correção efetiva, a correção efetiva prevalece.

## Classificação

- **CRÍTICO**: envolve gasto real, publicação externa, campanha, Meta Ads, landing pública, submissão ou dados comerciais.
- **ALTO**: bloqueia geração de landing, pipeline, OpenAI, qualidade comercial ou publicação.
- **MÉDIO**: causa retrabalho arquitetural, ruído visual, testes quebrados ou divergência documental.
- **BAIXO**: melhoria de governança sem impacto operacional imediato.

## Índice dos loops identificados

| Loop | Severidade | Status inicial | Tema | Correção efetiva principal |
| --- | --- | --- | --- | --- |
| `LOOP-FB-PUBLICATION` | CRÍTICO | Aberto/recorrente | Publicação Facebook Ads | contrato enxuto + protocolo `jobid` + validação com payload final |
| `LOOP-GL-PUBLICATION-LEADPORTAL` | CRÍTICO | Estabilizado com risco | GeraLanding → Lead Portal | separação `html_geralanding` puro vs `landing_page_html` publicável |
| `LOOP-OPENAI-SCHEMA-CONTRACT` | ALTO | Recorrente | Prompts, schemas e Structured Outputs | prompt + schema + parser + consumer test por etapa |
| `LOOP-GL-ARCHITECTURE-STAGES` | ALTO | Parcialmente estabilizado | Arquitetura por etapas | padrão por etapa backend e `openai.core.<etapa>` no Worker AI |
| `LOOP-GL-AUTOMATION-CHAIN` | ALTO | Recorrente | Encadeamento automático de etapas | orquestração no backend após callback de sucesso |
| `LOOP-QUALITY-REVIEW-VISION` | ALTO | Parcialmente estabilizado | Quality Review visual | screenshot renderizado mobile-first + auditoria por hash |
| `LOOP-LANDING-ANALYTICS-FUNNEL` | CRÍTICO | Recorrente | Analytics, funil e submissão | contrato Lead Portal → backend → evento bruto + evento normalizado + UI |
| `LOOP-PIPELINE-ADMIN-CONTRACT` | MÉDIO | Estabilizado com risco | Tela `/pipelines` e contrato persistente | registry oficial + sincronizador + definição/configuração separadas |
| `LOOP-HYPOTHESIS-PIPELINE` | ALTO | Em formação | Pipeline Dor → Resultado → Mecanismo → Prova → Oferta | etapas completas + pré-requisitos + finalização separada + lease |
| `LOOP-ARTIFACT-CONTAMINATION` | ALTO | Estabilizado com risco | Metadado técnico em artefato final | separação auditoria vs artefato publicável + whitelist de DTO final |
| `LOOP-COST-MODEL-AUDIT` | MÉDIO | Em observação | Custos OpenAI e modelo por etapa | preço vindo do catálogo backend + modelo efetivo auditado por etapa |
| `LOOP-LOW-TICKET-SALES-PAGE-BYPASS` | CRÍTICO | Fechado em 2026-07-01 | Low-ticket/GeraSalesPage | campanha bloqueada sem etapa final do pipeline concluída |
| `LOOP-GERASALESPAGE-VISUAL-TRANSFORMATION` | ALTO | Fechado em 2026-07-07 | GeraSalesPage | prompts v5 + quality review + auditoria bloqueiam pagina sem cenas visuais |

---

## LOOP-GERASALESPAGE-VISUAL-TRANSFORMATION — Pagina de venda sem transformacao visual

- **Severidade**: ALTO.
- **Status**: fechado em 2026-07-07.
- **Sintomas recorrentes ou risco observado**:
  - pagina publicada com promessa textual, mas pobre de imagens;
  - usuario nao consegue sentir o estado depois da transformacao;
  - pagina baseada quase so em texto, cards, icones ou gradientes;
  - preview ou demonstracao do produto nao materializa a transformacao percebida.
- **Causa-raiz sistemica confirmada**:
  - os prompts pediam antes/depois de forma generica, mas o HTML e o quality review nao exigiam quantidade minima de cenas visuais nem bloqueavam pagina pobre de prova visual.
- **Correcao efetiva**:
  - criar templates v5 do GeraSalesPage para visual-plan, HTML, quality review e publication package exigindo hero com cena do depois e 3 a 5 blocos visuais de transformacao;
  - marcar blocos visuais com `data-transform-visual`;
  - bloquear deterministamente a publicacao quando o HTML final tiver menos de 3 cenas visuais.
- **Regra preventiva**:
  - pagina de venda para trafego pago nao pode ser aprovada apenas por texto e clareza de oferta; deve provar visualmente dor, depois, preview/prova e contexto real de uso antes de liberar publicacao.

## LOOP-LOW-TICKET-SALES-PAGE-BYPASS — Low-ticket sem página criada pelo pipeline

- **Severidade**: CRÍTICO.
- **Status**: fechado em 2026-07-01.
- **Sintomas recorrentes ou risco observado**:
  - experimento low-ticket com página publicada por ponte operacional, fora da execução completa do GeraSalesPage;
  - liberação de campanha baseada apenas em `followUpActionUrl` preenchido;
  - aprendizado de qualidade da página ficando fora do pipeline;
  - risco de tráfego pago apontar para artefato não auditado pela etapa de quality review/publication package.
- **Causa-raiz sistêmica confirmada**:
  - a prontidão de campanha validava destino de venda como URL, não como artefato concluído pelo pipeline responsável;
  - o GeraSalesPage usava Flex, que reduzia custo, mas aumentava indisponibilidade em artefato comercial crítico.
- **Correção efetiva**:
  - bloquear prontidão e liberação de campanha para `LOW_TICKET_PRODUCT` sem `sales-page-publication-package=CONCLUIDO`;
  - adicionar rebuild canônico para substituir execuções antigas por `SUBSTITUIDO` e recriar a página pelo pipeline;
  - usar `service_tier=default` no GeraSalesPage v1 por padrão;
  - calcular custo OpenAI conforme o service tier usado.
- **Regra preventiva**:
  - nunca liberar campanha low-ticket apenas porque existe URL pública; a URL deve ser resultado auditável do GeraSalesPage v1 concluído.

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
- **O que resolveu efetivamente no histórico**:
  - tornar o `POST /api/facebook-campaigns` idempotente para o mesmo `campaignId` e bloquear nova campanha para o mesmo `experimentId`, evitando duplicidade de campanhas;
  - retirar da fila experimentos que já possuem campanha persistida, para o worker não recolocar o mesmo experimento em publicação;
  - criar endpoint enxuto `GET /api/facebook-adsets/experiments/{experimentId}/targeting-package`, evitando carregar `ExperimentDto` completo com HTML/copy/landing e removendo falso erro por buffer/payload grande;
  - quando `experimentId` é informado, resolver targeting mesmo se o experimento estiver `FAILED`, permitindo retry operacional sem depender do status de fila;
  - exigir pelo menos um público aprovado com `metaId` oficial e bloquear publicação ampla acidental;
  - fazer upload de imagem por bytes para `/adimages` e usar `image_hash`, removendo dependência da Meta baixar URL externa;
  - usar no `reachestimate` o mesmo targeting final do ad set, incluindo `geo_locations.countries=["BR"]`;
  - tratar ausência de limites em `reachestimate` como alerta operacional, não falha automática, e bloquear somente erro explícito ou alcance fora da faixa canônica;
  - registrar passos da publicação com `publicationJobId`/protocolo `jobid`, incluindo payload enviado, resposta recebida, endpoint, status e erro;
  - usar destino standalone vindo de `followUpActionUrl` no worker;
  - alinhar orçamento real por Ad Set, reportando `budgetMode=ADSET`;
  - enviar `is_adset_budget_sharing_enabled=false` em campanhas sem orçamento no nível da campanha.
  - enviar `experimentType` no contrato `/api/facebook-campaigns/experiments-ready` para o worker não degradar `LOW_TICKET_PRODUCT + SALES` para campanha de leads quando existir `freeReward` secundário.
  - exigir `facebookPixelId` para `LOW_TICKET_PRODUCT + SALES` antes de entrar na fila e publicar o ad set com `optimization_goal=OFFSITE_CONVERSIONS`, `promoted_object.pixel_id` e `custom_event_type=PURCHASE`.
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
  - teste garantindo que o contrato do backend expõe `experimentType=LOW_TICKET_PRODUCT` e que o worker publica `campaignObjective=SALES` como `OUTCOME_SALES`.
  - teste garantindo que campanha low-ticket de venda só é considerada pronta com pixel e que o worker usa `OFFSITE_CONVERSIONS` + evento `PURCHASE`.
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
- **O que resolveu efetivamente no histórico**:
  - marcar endpoints legados de aprovação/publicação como obsoletos e forçar uso do endpoint canônico do GeraLanding;
  - alterar o frontend para chamar `POST /api/experiments/{id}/geralanding/landing/approve-and-publish`;
  - simplificar o payload para o Lead Portal usando somente `slug`, `name`, `description` e `customFormHtml`, removendo `legacyPreviewHtml` e `renderMode`;
  - retirar a validação/normalização restritiva de `CustomFormHtmlResolver` no Lead Portal quando ela bloqueava HTML publicável válido;
  - separar definitivamente `html_geralanding` como HTML/CSS puro e `landing_page_html` como HTML publicável enriquecido com scripts, pixel, analytics e submissão;
  - habilitar a aprovação pela fonte de verdade do backend, sem depender exclusivamente da prévia local do frontend;
  - injetar submissão canônica idempotente quando o HTML tem controles mínimos de captura, evitando landing publicada sem envio de formulário;
  - criar endpoint de compatibilidade no Lead Portal para receber submissão pública e encaminhar ao backend principal.
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
- **O que resolveu efetivamente no histórico**:
  - normalizar respostas com Markdown code fence antes do parse;
  - extrair o primeiro objeto JSON balanceado em vez de usar substring ingênua do primeiro `{` ao último `}`;
  - tratar JSON escapado e payload duplicado/concatenado nos consumidores;
  - alinhar o caminho esperado pelo backend, por exemplo `landingPageImagePlanning.images[]` quando o resumo de imagens contava `images` e não `imagePlan`;
  - remover palavras-chave incompatíveis com Structured Outputs estrito, como `allOf`, condicionais e `uniqueItems`, quando a Responses API rejeitou o schema;
  - exigir `additionalProperties: false` nos objetos usados em schemas estritos;
  - mover prompts relevantes para o local versionado correto no `ai-worker/src/main/resources/prompts/...`;
  - criar validações pós-resposta para impedir estilos/classes inexistentes em `definicoes`;
  - atualizar extratores backend e Worker AI para reconhecer JSON direto, aninhado, serializado em texto e encapsulado em Markdown quando a UI já conseguia detectar o conteúdo.
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
- **O que resolveu efetivamente no histórico**:
  - padronizar backend por etapa com `Backend<Etapa>Controller`, `Backend<Etapa>Service` e records em subpacotes por operação;
  - expor endpoints internos específicos por etapa: `pending`, `recebe-prompt` e `recebe-resposta`;
  - remover controllers genéricos/transversais quando eles mantinham acoplamento entre etapas;
  - mover provisórios e assemblers para o pacote da própria etapa, como `presetdesign.provisorio`;
  - ajustar o frontend para consumir endpoints segmentados por etapa, inclusive detalhe com `stageCode`/segmento correto;
  - migrar etapas do Worker AI para `com.marketinghub.worker.openai.core.<etapa>`, reduzindo dependência do pacote legado `worker.geralanding`;
  - desativar/remover implementações legadas quando a etapa passou a operar pelo core OpenAI;
  - usar ArchUnit para proteger dependências por etapa/camada, mas ajustar regras somente quando a arquitetura efetiva já estava clara.
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
- **O que resolveu efetivamente no histórico**:
  - colocar o encadeamento automático no backend, no callback de conclusão bem-sucedida da etapa anterior;
  - registrar `promptTemplateId` com origem automática, como `auto/wireframe`, `auto/copy`, `auto/image-planning` e `auto/image-generation`;
  - criar testes explícitos garantindo que sucesso cria a próxima execução e falha não cria;
  - separar a automação do GeraLanding da automação de criativos de anúncio;
  - bloquear geração automática de imagem de anúncio ao concluir `AD_IMAGE_BRIEFING` quando o usuário está em outro fluxo;
  - limpar imagens/jobs/manifesto ao reexecutar `Gera Prompt Imagem`, evitando que imagens antigas contaminem a próxima execução;
  - ocultar ou desabilitar ações de geração em experimento já enviado/publicado, preservando apenas histórico e consulta.
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
- **O que resolveu efetivamente no histórico**:
  - abandonar avaliação por imagens soltas extraídas do HTML e passar a renderizar o HTML em browser/headless;
  - enviar screenshots renderizados para o modelo de visão, com mobile como evidência prioritária;
  - aceitar desktop como complementar, sem impedir a revisão quando o mobile obrigatório já foi capturado;
  - aumentar timeout de screenshot e voltar ao full-page quando recortes prejudicavam a evidência;
  - usar modelo de visão dedicado e configuração própria de `imageDetail`;
  - reduzir o prompt textual quando os screenshots já representam a evidência principal;
  - calcular e persistir hashes de HTML, prompt/request e screenshots para detectar reuso de evidência e contradição entre avaliações;
  - exibir na tela de detalhe os screenshots e dados de auditoria enviados ao modelo.
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
- **O que resolveu efetivamente no histórico**:
  - injetar script de analytics no Lead Portal para a landing standalone realmente chamar o backend;
  - substituir instrumentação legada quando a landing já publicada tinha script antigo sem debug;
  - criar rota local de compatibilidade no Lead Portal para submissão pública e encaminhamento ao backend principal;
  - contar `landing-page-analytics` no resumo do funil, em vez de considerar apenas fontes legadas;
  - somar submissões públicas vindas de `experiment_funnel_event` na etapa `ENVIO_FORM`, com deduplicação por `submissionId`;
  - criar tabela normalizada `experiment_landing_analytics_event` vinculada ao evento bruto, preservando auditoria e permitindo recorrência por `visitorId`;
  - deduplicar `page_view` por `visitorId`, `sessionId`, `eventType` e `pageUrl` em janela curta;
  - no reset, apagar primeiro eventos normalizados e depois eventos brutos, evitando violação de FK;
  - invalidar também a query da aba Analytics no frontend após zerar contagens;
  - enviar `deviceType`, sistema operacional e tamanho de tela pelo script público para apoiar decisão de layout/mobile.
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
- **O que resolveu efetivamente no histórico**:
  - criar registry oficial de pipelines/etapas no backend;
  - expor diagnóstico de contrato na tela, mostrando divergências entre banco e cânone;
  - bloquear exclusão e alteração estrutural de pipeline oficial;
  - criar sincronizador seguro para etapas oficiais ausentes e correções estruturais não destrutivas;
  - criar rebuild controlado com confirmação para remover etapas operacionais divergentes e recriar somente as canônicas;
  - separar definição persistente (`pipeline_definition`, `pipeline_stage_definition`) de configuração operacional (`pipeline_stage_config`);
  - preservar modelo OpenAI, descrição e status operacional durante sincronização quando houver mapeamento seguro;
  - remover criação manual de pipeline/etapa no frontend;
  - ajustar changelogs de posição usando faixa temporária para evitar conflito de unique key no MySQL 5.7;
  - expor metadados de implementação e modelos por etapa para a tela de experimento/GeraLanding.
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
- **O que resolveu efetivamente no histórico**:
  - completar a sequência com a etapa Prova entre Mecanismo e Oferta;
  - bloquear Oferta sem Prova concluída tanto na criação manual quanto na fila de pendentes;
  - criar Worker AI específico para cada etapa que existia no backend;
  - revalidar pré-requisitos no pending e na marcação de processamento, não apenas na tela;
  - extrair o fechamento da hipótese para `HypothesisPipelineFinalizationService`, fora da etapa Dor;
  - converter coluna insuficiente para armazenar resposta completa, como `success_rule` para `LONGTEXT`;
  - criar lease operacional para jobs presos em `PROCESSANDO` ou `AGUARDANDO_RETORNO_OPENAI`;
  - persistir `raw_response`, prompt, request cru e custo por etapa para relatório auditável;
  - adicionar fluxo completo automático com retry controlado, mantendo a orquestração no backend;
  - passar contexto enriquecido do nicho-cnae para todas as etapas sem criar oferta prematura fora da etapa Oferta.
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
- **O que resolveu efetivamente no histórico**:
  - remover comentários técnicos `<!-- AUTO: ... -->` dos HTMLs provisórios/finais;
  - impedir título técnico como `Wireframe provisório` no HTML final;
  - separar `html_geralanding` como artefato puro de geração e `landing_page_html` como versão publicável instrumentada;
  - formalizar no AGENTS a proibição de contaminar artefato final com metadado técnico;
  - usar whitelist de campos do DTO final antes de enviar payload publicável;
  - tratar auditoria, jobId, prompt, schema, request, resposta e hashes como dados de execução, não como conteúdo do cliente;
  - fazer o Quality Review apontar metadado técnico visível como problema bloqueante.
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
  - request auditado não mostra o `service_tier` efetivo da etapa;
  - Worker AI usa preço hardcoded ou propriedade zerada;
  - modelo configurado em `/pipelines` não chega à execução.
- **Causa-raiz sistêmica provável**:
  - seleção de modelo, modo de preço, catálogo de preço e cálculo de custo ficavam em fontes diferentes.
- **O que resolveu efetivamente no histórico**:
  - remover tabela hardcoded de preços do Worker AI;
  - fazer o Worker AI consultar o catálogo do backend em vez de acessar banco diretamente;
  - calcular custo pelo modelo efetivo do request e pelos preços cadastrados em `openai_model`;
  - persistir e exibir `inputTokens`, `outputTokens` e `costUsd` por execução;
  - expor `GET /api/pipelines/geralanding/stage-models` com modelo, preço flex, tipo de artefato e fallback aplicado;
  - mostrar na aba GeraLanding o modelo configurado, custos flex por 1M tokens e custo acumulado;
  - montar request auditável com `service_tier=flex` desde a origem nas etapas em que isso era necessário;
  - no pipeline de hipótese, recalcular custo no backend com base nos tokens/modelo/preços persistidos, sem confiar cegamente no `costUsd` enviado pelo worker.
- **Fechamento mínimo do loop**:
  - modelo por etapa vindo do pipeline/catálogo;
  - fallback default explícito por tipo de artefato;
  - custo calculado via backend/catalogo `openai_model`;
  - request auditável sempre com o `service_tier` efetivo da etapa;
  - exceções ao Flex, como Quality Review em processamento default/standard por indisponibilidade operacional do Flex em requisições multimodais grandes, devem registrar justificativa funcional no fluxo;
  - UI mostra modelo, modo, preço e custo acumulado.
- **Regra preventiva**:
  - nenhuma etapa OpenAI deve persistir execução sem modelo efetivo, tokens e regra de preço identificável.

## LOOP-EXPERIMENT-COST-RECONCILIATION — Total de custo sem origem auditável

- **Severidade**: ALTO.
- **Status**: fechado em 2026-07-07.
- **Sintomas recorrentes**:
  - `experiment.total_cost` maior que a soma de origem, mídia e despesa;
  - custo técnico em USD aparecendo como se fechasse total em BRL;
  - diferença legada sendo interpretada como custo real de IA;
  - reprocessamento ou sincronização de mídia inflando custo acumulado.
- **Causa-raiz sistêmica provável**:
  - custo total tratado como acumulador persistido e fonte principal de verdade, sem razão idempotente por origem;
  - atualização do custo combinando entidade JPA gerenciada com `increment` SQL direto.
- **O que resolveu efetivamente no histórico**:
  - usar custo rastreável em BRL como total principal da tela;
  - manter `total_cost` como legado e mostrar diferença positiva como custo não reconciliado;
  - separar auditoria técnica em USD de parcelas financeiras em BRL;
  - impedir `incrementTotalCost` SQL quando a entidade já está gerenciada pelo JPA.
- **Fechamento mínimo do loop**:
  - toda tela ou relatório de experimento deve diferenciar custo rastreável, total legado e diferença não reconciliada;
  - custos OpenAI/GeraLanding/GeraSalesPage em USD entram como auditoria técnica, não como parcela BRL sem conversão rastreável;
  - sincronização de mídia deve aplicar apenas delta e ter teste de regressão;
  - atribuição de custo não pode persistir o mesmo delta por dois caminhos na mesma transação.
- **Regra preventiva**:
  - nunca usar `experiment.total_cost` isolado como explicação financeira principal; sempre reconciliar por origem auditável ou marcar como legado não reconciliado.

---

## Checklist rápido antes de corrigir problema recorrente

Use este checklist quando o problema estiver em algum loop acima:

```md
- O problema reabre qual LOOP-*?
- Qual contrato está divergindo?
- Qual correção efetiva já resolveu esse tipo de loop antes?
- Estou repetindo uma solução antiga que já foi superada?
- Qual módulo é dono da correção?
- O frontend, backend, worker, Swagger e cânone estão alinhados?
- Existe teste que reproduz a falha atual?
- Existe teste que impede o mesmo loop de voltar?
- O registro operacional foi atualizado no documento do tema?
```

## Registros deste documento

## 2026-06-17 00:01:07 UTC-3
- solicitação: criar um arquivo de registro de loops operacionais a partir da análise de `docs/registros/experimentos.md` e revisar o `AGENTS.md` para melhoria preventiva.
- causa-raiz observada: o histórico mostra recorrência de problemas por contratos instáveis entre frontend, backend, workers, OpenAI, Lead Portal e Meta Ads.
- registro do que foi feito: criado este documento com os principais loops, causa-raiz sistêmica, fechamento mínimo e regra preventiva por tema; incluída sugestão objetiva de melhoria para o `AGENTS.md`.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/registros/experimentos.md

## 2026-06-17 00:12:41 UTC-3
- solicitação: melhorar este arquivo identificando, para cada loop, o que resolveu efetivamente o problema no histórico real do projeto.
- causa-raiz observada: a primeira versão registrava sintomas, causas e prevenção, mas ainda não destacava claramente quais correções concretas estabilizaram cada ciclo.
- registro do que foi feito: adicionado o bloco **O que resolveu efetivamente no histórico** em cada `LOOP-*`, diferenciando correção efetiva de prevenção futura.
- documentos lidos para pesquisar e resolver o problema:
  - AGENTS.md
  - docs/registros/experimentos.md
