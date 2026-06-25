# Procedimento Canônico de Execução de Experimento v1

## 1. Objetivo

Padronizar o funcionamento do experimento de ponta a ponta (criação, geração de artefatos, aprovação e publicação), garantindo rastreabilidade operacional, alinhamento com o código vigente e foco no resultado comercial do sistema.

Este documento consolida:
- a ordem operacional nas telas;
- as etapas do pipeline de experimento;
- as etapas do pipeline Gera Landing;
- a relação entre Worker AI, OpenAI (modo batch), Lead Portal e publicação final;
- observabilidade de entradas/saídas e custos por experimento.

## 2. Escopo funcional

Aplica-se ao fluxo de experimentos no Marketing Hub, especialmente:
- Backend (`backend/ads-service`);
- Worker AI (`ai-worker`);
- Frontend administrativo de Experimentos;
- publicação final de landing no Lead Portal.

## 3. Fluxo macro do experimento (visão operacional)

### 3.1 Criação do experimento
1. O usuário cria o experimento pela tela, preenchendo os campos obrigatórios do formulário.
2. Após salvar, o experimento passa a ter contexto para geração de ativos do pipeline.


### 3.1.1 Regra mandatória — identificação automática de hipótese e experimento

O usuário não deve escolher manualmente nome de hipótese nem nome de experimento. Para reduzir esforço operacional e evitar nomes inconsistentes, o backend deve gerar a identificação no momento da criação usando:

- sigla derivada do nome do nicho, com 3 a 4 caracteres em caixa alta;
- sequência numérica por nicho para hipótese no formato `<SIGLA>-H001`, `<SIGLA>-H002`, ...;
- sequência numérica por hipótese para experimento no formato `<CODIGO-HIPOTESE>-E001`, `<CODIGO-HIPOTESE>-E002`, ...;
- quando não houver nicho associado, usar a sigla operacional `GER` como fallback.

A interface deve informar que o identificador será automático e não deve exigir campo de nome para fechar hipótese ou criar experimento. O nome do experimento deve carregar o código da hipótese para manter rastreabilidade direta entre hipótese e teste.

### 3.2 Execução no detalhe do experimento
1. O usuário entra no detalhe do experimento.
2. A execução acontece por etapas guiadas, com geração e validação progressiva dos artefatos.

## 4. Pipeline de experimento (núcleo inicial)

A sequência canônica de seções do pipeline inclui:
1. `CAMPAIGN_ANGLE` (ângulo da campanha)
2. `AD_COPY` (ad copy)
3. `AD_IMAGE_BRIEFING` (briefing da imagem do anúncio)

Essas seções e suas dependências fazem parte do enum oficial do backend.

### 4.1 Contrato operacional administrativo do pipeline

O pipeline oficial de experimento usa a versão canônica `procedimento-experimento-canon.v1` no backend administrativo de pipelines. A sincronização segura deve considerar estruturais os campos `code`, `module` e `name` do pipeline, além de `code`, `position`, `name`, `required`, `executionModule` e `rootPackage` das etapas oficiais. Os campos `description`, `active` e `openAiModelId` das etapas são configuração operacional e não podem ser sobrescritos pela sincronização sem regra explícita. A etapa deve informar `executionModule` somente quando for executada fora do backend principal; `rootPackage` deve sempre indicar o pacote raiz no backend ou no módulo executor onde a implementação da etapa vive.

Endpoints administrativos vigentes:
- `GET /api/pipelines/metadata` expõe versão canônica, aliases e política de campos;
- `GET /api/pipelines/{id}/diagnostics` compara banco e contrato oficial com causa-raiz e ação recomendada;
- `POST /api/pipelines/{id}/sync` sincroniza de forma idempotente um pipeline existente, sem aceitar payload da tela;
- `POST /api/pipelines/{id}/rebuild-official-stages` permite que a tela, após confirmação explícita do usuário, remova etapas operacionais atuais de um pipeline oficial e recrie somente as etapas do contrato canônico, reaproveitando configurações compatíveis como descrição e modelo OpenAI quando houver mapeamento seguro;
- `POST /api/pipelines/official/{code}/sync` cria ou sincroniza um pipeline oficial ausente pelo código canônico, sem aceitar payload da tela.

A sincronização segura pode criar pipeline/etapas oficiais ausentes e corrigir campos estruturais permitidos, mas deve bloquear divergências destrutivas, como etapa extra sem mapeamento canônico ou duplicidade operacional, para evitar perda de histórico e preservar a causa-raiz para decisão humana. A interface administrativa do frontend não deve oferecer criação manual de pipelines nem de etapas; esses cadastros nascem do contrato canônico e de rotinas do backend, enquanto a tela fica restrita à visualização, diagnóstico, ajuste oficial controlado e edição operacional segura de registros existentes.

### 4.1.1 Blocos operacionais na tela `/pipelines`

Na tela administrativa `/pipelines`, o fluxo de experimento deve ser tratado como **um único pipeline oficial** com código operacional `experiment-pipeline`. As etapas `CAMPAIGN_ANGLE`, `AD_COPY` e `AD_IMAGE_BRIEFING` pertencem ao bloco inicial desse mesmo pipeline administrativo; elas não formam pipeline separado. O GeraLanding também não é pipeline oficial próprio: ele é o bloco/subpipeline operacional de landing dentro do pipeline oficial `experiment-pipeline`, usado para materializar a landing do experimento após o bloco inicial.

Códigos operacionais oficiais a exibir e sincronizar:

| Bloco | Pipeline oficial em `/pipelines` | Etapas operacionais | Obrigatoriedade e acionamento |
|---|---|---|---|
| Núcleo inicial do experimento | `experiment-pipeline` | `campaign-angle`, `ad-copy`, `ad-image-briefing` | etapas obrigatórias; execução por comando operacional do usuário ou rotina explícita; a ordem é obrigatória e cada etapa depende da anterior concluída. |
| GeraLanding dentro do experimento | `experiment-pipeline` | `landing-page-wireframe`, `landing-page-copy`, `landing-page-image-planning`, `landing-page-image-generation`, `landing-page-design-preset`, `landing-page-quality-review`, `landing-page-deliverables` | etapas obrigatórias para preparar landing publicável; o início do bloco é manual/operacional após o bloco inicial estar pronto, e as transições internas previstas como automáticas devem enfileirar a próxima etapa sem novo clique. |

A transição entre o bloco inicial e o GeraLanding ocorre quando `AD_IMAGE_BRIEFING` conclui com sucesso e o experimento já possui ângulo, copy e briefing de imagem suficientes para gerar a landing. A partir desse ponto, o usuário ou a rotina operacional autorizada inicia `landing-page-wireframe`; depois disso, o backend mantém o encadeamento automático documentado neste cânone para copy, planejamento de imagem, geração de imagem e preset de design. Aprovação/publicação da landing e decisões de liberação para campanha continuam sendo passos manuais de controle operacional, pois impactam diretamente o que será exposto ao público e medido como experimento comercial.

### 4.1.2 Regra mandatória — entrada comercial com isca e produto

Todo experimento de captação por isca digital deve testar uma única hipótese comercial de entrada, composta por **uma dor principal**, **uma isca digital principal**, **um produto low-ticket de entrada**, **uma promessa principal** e **um CTA principal**. O objetivo é impedir que anúncio, landing, formulário, entrega e próxima oferta validem mensagens diferentes e tornem o resultado inconclusivo.

Regras obrigatórias:
- o experimento deve escolher apenas uma dor de entrada; quando houver várias dores plausíveis, deve prevalecer a mais urgente, concreta, reconhecível rapidamente pelo público e diretamente ligada à intenção comercial do nicho;
- a isca digital deve ser simples, concreta e imediatamente aplicável, como mensagens prontas, checklist curto, roteiro, template ou mini-kit de vitória rápida; não deve ser apresentada como “prévia”, “diagnóstico”, “sistema completo” ou promessa ampla demais para o primeiro contato;
- o produto low-ticket de entrada deve ser explicitado junto da isca, com transformação percebida como muito necessária, redução clara de dor/esforço, aumento de facilidade e sensação de alívio, controle e felicidade, mantendo plausibilidade e sem prometer resultado absoluto;
- quando a hipótese envolver MEI, autônomo, trabalhador por conta própria, dono-operador ou negócio local, a geração da oferta deve considerar explicitamente a frente de presença digital, atendimento e aquisição prática como possível embalagem de valor: Perfil da Empresa no Google, Instagram, WhatsApp, descrições comerciais, respostas prontas, pedido de avaliações, follow-up, agenda e rotina simples de conteúdo;
- nessa frente, a IA deve ser posicionada como redução de esforço operacional para criar e organizar ativos comerciais aplicáveis, nunca como promessa genérica de “usar IA” nem como automação total do negócio;
- a promessa do anúncio, do botão, do formulário e da entrega deve repetir a mesma ideia central da isca digital. Exemplo canônico de CTA: “Receber as 3 mensagens”;
- a copy não deve misturar múltiplas iscas, múltiplas dores ou múltiplos CTAs no mesmo experimento;
- a landing deve funcionar como confirmação da isca digital e validação de interesse, não como página de venda completa nem como explicação extensa do produto final;
- experimentos que capturam lead por isca digital devem ser configurados para objetivo de campanha **Leads** e otimização compatível com geração de lead/formulário, não para objetivo **Tráfego** nem otimização por clique;
- antes da publicação, o conjunto de ativos deve ser considerado inconsistente se a dor, a isca, a promessa ou o CTA divergirem entre anúncio, botão, formulário, landing e entrega.

A interpretação do resultado do experimento deve partir dessa unidade: se não houver leads, a dor ou a promessa/isca de entrada devem ser revisadas; se houver leads baratos, mas sem avanço comercial, a oferta, prova ou qualificação devem ser melhoradas.

### 4.1.3 Regra transitória — validação inicial por primeiro envio

Enquanto o sistema ainda estiver na fase inicial sem volume recorrente de formulários enviados, o primeiro envio válido do formulário deve ser tratado como sinal comercial suficiente para colocar o experimento em `STANDBY` operacional e pausar imediatamente a exposição paga no Meta Ads. Essa regra não valida estatisticamente o produto nem autoriza escala; ela apenas evita gasto adicional enquanto o time confirma manualmente a qualidade do lead, a coerência da promessa, a entrega mínima prometida e a necessidade de gerar ou ajustar o produto de entrada.

Regras obrigatórias:
- o evento considerado deve ser um envio real e válido do formulário, com dados mínimos úteis e sem duplicidade operacional evidente;
- ao detectar o primeiro envio válido, o backend deve registrar a decisão do experimento como `STANDBY` ou estado equivalente de pausa operacional auditável;
- a campanha, conjunto de anúncios ou anúncio correspondente no Meta Ads deve ser desativado/pausado pelo fluxo oficial de integração, evitando novos gastos até decisão humana ou regra posterior de retomada;
- o lead não pode ficar sem resposta: deve receber a entrega mínima prometida, uma comunicação de recebimento ou um encaminhamento claro compatível com a promessa feita na landing;
- o status `STANDBY` por primeiro envio não substitui a validação estatística de 30+ envios qualificados, nem a regra de reprovação por 100 acessos sem envio;
- a tela administrativa deve deixar claro que o experimento parou por sinal inicial, não por validação completa.

### 4.1.4 Regra mandatória — execução auditável por `ExperimentRun`

O `Experiment` continua representando uma pergunta comercial atômica: uma dor principal, uma promessa principal, uma oferta principal, uma rota comercial, uma variável primária e uma métrica primária. A tentativa operacional de colocar essa pergunta no mercado deve ser representada por `ExperimentRun`, sem alterar o significado comercial do experimento.

Regras obrigatórias:
- correção técnica que não muda a pergunta comercial principal cria novo `ExperimentRun` do mesmo experimento;
- mudança de dor, promessa, isca, rota de captura, produto, oferta, preço, CTA principal, público estratégico, métrica primária ou pergunta de negócio cria novo `Experiment`;
- `ExperimentRun` deve possuir modo explícito `TEST` ou `PRODUCTION`;
- eventos, passos, gates e evidências de `TEST` não podem alimentar métricas comerciais de `PRODUCTION`;
- status operacional do run e validade comercial da evidência são dimensões independentes;
- run tecnicamente inválido continua visível para aprendizado operacional, mas não pode reprovar hipótese, materialização comercial ou mercado;
- somente run com validade `COMMERCIALLY_VALID` pode alimentar comparação, declaração de vencedor, decisão de escala, reprovação comercial ou recomendação de próximo teste baseada em resultado.

Validades canônicas iniciais:
- `NOT_EVALUATED`: evidência ainda não avaliada ou legado migrado sem reclassificação segura;
- `TECHNICALLY_INVALID`: publicação, destino, formulário, integração, targeting técnico ou exposição falharam;
- `MEASUREMENT_INVALID`: eventos, custos, conversões, deduplicação ou janela comercial não são confiáveis;
- `STRATEGICALLY_INVALID`: desenho experimental, variável primária, estratégia, público ou insumo upstream impedem conclusão de mercado;
- `INSUFFICIENT_DATA`: execução e mensuração são válidas, mas a amostra ou janela ainda não sustentam decisão;
- `COMMERCIALLY_VALID`: execução, exposição, estratégia e mensuração permitem aprendizado confiável, mesmo quando o resultado for negativo ou inconclusivo.

Compatibilidade obrigatória com legado:
- status existentes de `Experiment` permanecem legíveis durante a migração;
- `INVALIDATED` legado não deve ser convertido automaticamente em falha de hipótese comercial;
- experimentos legados ativos/concluídos devem receber run legado com validade inicial `NOT_EVALUATED` quando a persistência de runs for implementada;
- a regra transitória de primeiro envio válido em `STANDBY` permanece como política de parada do modo inicial, mas deve ser registrada no run e não substitui validação comercial completa.

### 4.1.5 Feature flags canônicas da evolução de experimentos

A evolução de Experimentos deve ser liberada por feature flags, mantendo o comportamento legado como padrão até validação explícita. Nenhuma flag abaixo autoriza, sozinha, publicação, aumento de orçamento, decisão automática de vencedor ou remoção de fluxo legado.

Flags iniciais:

| Flag | Padrão | Responsabilidade | Rollback |
|---|---:|---|---|
| `experiments.runs.enabled` | `false` | habilita leitura/escrita operacional de `ExperimentRun` após a fase de persistência | voltar a UI e comandos para status legado de `Experiment` |
| `experiments.runs.preflight-enabled` | `false` | habilita gates de readiness/preflight antes da mídia | desativar bloqueios novos e manter diagnósticos apenas informativos |
| `experiments.runs.frontend-enabled` | `false` | exibe card/aba de execução atual e histórico mínimo de runs | ocultar UI nova sem apagar dados persistidos |
| `experiments.comparison.enabled` | `false` | habilita criação e leitura de `ExperimentComparison` | pausar comparações sem pausar experimentos individuais |
| `experiments.decision-ai.enabled` | `false` | habilita pipeline de apoio por IA somente após dados confiáveis | manter Centro de Decisão determinístico |

Rollback obrigatório:
- dados novos devem ser append-only ou compatíveis com leitura legada;
- desativar a flag não deve apagar runs, gates, passos, fixtures nem histórico de decisão;
- enquanto `experiments.runs.enabled=false`, comandos produtivos continuam usando o contrato legado;
- enquanto `experiments.comparison.enabled=false`, a política de primeiro envio válido em `STANDBY` continua aplicável ao modo inicial.

### 4.1.6 Fixtures históricas obrigatórias de regressão

Os experimentos 37, 38, 39 e 40 formam baseline de regressão do processo decisório. O objetivo dessas fixtures não é reproduzir todos os relatórios históricos, mas impedir que o sistema volte a tratar falha técnica, dado contaminado, desenho incompleto ou medição inválida como rejeição simples de mercado.

Fonte versionada das fixtures da Fase 0:

- `docs/implementacao/experimentos/fixtures/experimentos-37-40-regressao.json`

Regras obrigatórias para testes futuros:
- experimento 37 deve bloquear conclusão comercial quando formulário/landing/captura não comprovam evidência válida;
- experimento 38 deve bloquear aprendizado de mercado quando variável, métrica, KPI ou publicação estiverem incompletos;
- experimento 39 deve diferenciar falha de qualidade upstream, reset/liberação, alcance e targeting antes de interpretar mercado;
- experimento 40 deve bloquear materialização automática quando houver baixa correspondência semântica, fonte frágil ou contaminação por solução;
- nenhum desses casos pode declarar `COMMERCIAL_HYPOTHESIS_FAILURE` sem run comercialmente válido e evidências persistidas.


### 4.2 Prompts dessas etapas
Os prompts do pipeline ficam versionados no repositório, em `resources` do Worker AI.

Local canônico vigente:
- pipeline de experimento (núcleo inicial): `ai-worker/src/main/resources/prompts/experiment`;
- pipeline de landing no Worker AI: prompts e schemas devem ser resolvidos por configuração tipada da etapa em `openai.core.<etapa>`; o caminho físico em `resources/prompts/<dominio>` é detalhe de recurso versionado e não define namespace Java do Worker AI.

Regra mandatória para `AD_IMAGE_BRIEFING`: cada briefing de imagem de criativo deve orientar texto sobreposto em formato de pergunta clara, completa e objetiva, capaz de filtrar imediatamente pessoas verdadeiramente do nicho. A pergunta precisa mencionar situação, rotina, cargo, atividade, dor ou resultado específico do nicho; se puder servir para qualquer mercado, deve ser reescrita antes da resposta final.

### 4.2.1 Regra mandatória — oferta low-ticket da hipótese

A etapa `hypothesis-offer` do pipeline de hipótese deve materializar uma oferta low-ticket digital, não uma oferta genérica. O objetivo é entregar um produto de entrada simples, vendável e aplicável rapidamente, que depois possa alimentar página de vendas, isca digital e campanha sem precisar redescobrir a oferta.

Regras obrigatórias:
- a oferta deve transformar Dor, Resultado, Mecanismo e Prova em um produto digital de baixo atrito, com promessa de entrada específica e vitória rápida plausível;
- a resposta deve explicitar posicionamento da oferta, promessa de entrada, ancoragem/faixa de preço low-ticket, entregáveis concretos, pilha de valor, percepção de muito por pouco, formato de produção, ativo de quick win e prontidão para a próxima etapa comercial;
- os entregáveis devem poder virar ativos produzíveis pelo Marketing Hub, como diagnóstico, roteiro, checklist, template, biblioteca, calculadora simples, prompts, plano ou mini-kit; o pacote deve parecer robusto, com vários componentes úteis e complementares por um preço baixo;
- a etapa pode sugerir faixa de preço compatível com low-ticket, mas não deve criar checkout, desconto falso, urgência artificial, campanha de anúncios, página de vendas, headline completa de landing ou estratégia de Facebook;
- a oferta deve preservar limites de plausibilidade: sem renda garantida, sem agenda cheia garantida, sem cura, sem automação total e sem depender de acesso interno ao negócio do cliente;
- quando a solução ficar ampla demais, o prompt deve reduzir o escopo para o menor kit capaz de atacar a causa-raiz prioritária da dor.

### 4.2.2 Regra operacional — fluxo completo da hipótese

A tela de nova hipótese deve oferecer uma ação de fluxo completo para executar Dor → Resultado → Mecanismo → Prova → Oferta sem intervenção manual entre etapas. A orquestração principal deve ficar no backend, pois é ele que conhece a ordem canônica, os pré-requisitos, o status persistido e o callback de conclusão/falha de cada etapa.

Regras obrigatórias:
- o botão de fluxo completo deve iniciar a primeira etapa ainda não concluída e não duplicar execução quando já houver job ativo no nicho;
- ao concluir uma etapa automática com sucesso, o backend deve enfileirar a próxima etapa canônica;
- ao falhar uma etapa automática, o backend deve tentar novamente a mesma etapa até 3 tentativas totais;
- após 3 falhas da mesma etapa, o fluxo deve parar e manter a falha auditável para investigação de causa-raiz;
- a interface deve apenas disparar o fluxo e acompanhar os status; não deve simular a orquestração no navegador.

### 4.2.3 Regra mandatória — passagem enriquecida `nicho-cnae → hipótese`

Quando o nicho tiver sido materializado pelo pipeline `nicho-cnae`, o pipeline de hipótese deve receber, além do registro base de `market_niche`, o perfil enriquecido mais recente do nicho.

Regras obrigatórias:
- o backend deve incluir no contrato pendente da hipótese os sinais não-ofertivos do perfil enriquecido: rotina, dores, resultados desejados, oportunidades de mecanismo, evidências, fontes, persona operacional, padrões de linguagem, gatilhos comerciais, objeções e scores de qualidade/confiança;
- o Worker AI deve colocar esses sinais no bloco de contexto das etapas Dor, Resultado, Mecanismo, Prova e Oferta, preservando-os como insumo estratégico;
- o pipeline de hipótese deve usar esses sinais para aumentar especificidade, linguagem real, plausibilidade do mecanismo, redução de objeções e percepção de valor da oferta low-ticket;
- a passagem enriquecida não autoriza o `nicho-cnae` a criar promessa, oferta, preço, checkout, landing ou campanha. O `nicho-cnae` permanece responsável por realidade operacional e evidências; a hipótese permanece responsável por transformar essa realidade em Dor → Resultado → Mecanismo → Prova → Oferta.

### 4.2.4 Regra mandatória — não repetir hipóteses no mesmo nicho

Quando uma nova hipótese for solicitada para um nicho, o backend deve obter as demais hipóteses já geradas para esse mesmo `market_niche_id` e incluí-las no contrato pendente entregue ao Worker AI.

Regras obrigatórias:
- o contrato pendente deve enviar um resumo das hipóteses anteriores do nicho, incluindo no mínimo código/título, dor, promessa, persona, mecanismo, entrega e status quando existirem;
- o Worker AI deve inserir esse resumo no prompt da etapa Dor como histórico de diferenciação;
- a nova hipótese não deve repetir a mesma dor de entrada, persona, promessa implícita ou mecanismo potencial já usado no mesmo nicho;
- se o histórico estiver vazio, o prompt deve declarar explicitamente que não há hipótese anterior registrada para o nicho.

### 4.3 Regra de diferenciação de ângulo após experimento reprovado

Ao gerar a etapa `CAMPAIGN_ANGLE`, o prompt deve receber o histórico dos experimentos reprovados por 100 acessos sem envio da mesma hipótese, quando existir. A reprovação de um experimento reprova aquela materialização de mercado (público, criativo, landing, isca e formulário), mas não reprova automaticamente a hipótese estratégica.

Regras obrigatórias:
- o backend deve acrescentar ao prompt somente experimentos da mesma hipótese reprovados pela regra estatística de 100 acessos sem envio de formulário (`FORM_ZERO_CONVERSION_RULE_OF_THREE`), excluindo o experimento atual; experimentos `FAILED`, `USER_STOPPED` ou apenas `INCONCLUSIVE` não entram nesse histórico;
- o modelo deve preservar a hipótese estratégica, mas criar uma rota comercial radicalmente diferente dos experimentos reprovados por 100 acessos sem envio;
- a diferença radical deve trocar ao menos uma alavanca principal: dor de entrada, resultado imediato prometido, isca digital/prova inicial, framing visual ou CTA;
- o novo ângulo não deve reaproveitar headline, promessa central, CTA, mecanismo de entrada ou mensagem de landing semelhantes aos experimentos reprovados por 100 acessos sem envio;
- a landing deve ser tratada como isca digital e validação de interesse no valor da hipótese, não como validação integral do produto final.


### 4.4 Contrato mínimo do artefato `campaignAngle`

O retorno da etapa `CAMPAIGN_ANGLE` deve ser um JSON com o bloco `campaignAngle` preenchido por campos estratégicos detalhados e não vazios. O contrato vigente exige, no mínimo: `visualAngle`, `hook`, `mechanismSummary`, `primaryCTA`, `cta`, `landingMatchLine`, `audienceFilterLine`, `objections`, `messageMatch` e `differentiationRationale`.

Os blocos de dor, resultado, prova e oferta já são tratados em outras etapas do pipeline e não pertencem mais ao contrato final do `campaignAngle`. O campo `funnelStage` também não pertence ao contrato final e não deve ser solicitado nem persistido. O schema deve descrever claramente o papel comercial de cada campo do ângulo, e respostas estruturalmente válidas, mas com strings vazias ou placeholders nesses campos, devem ser rejeitadas para impedir que o pipeline avance com ângulo sem utilidade comercial.

## 5. Pipeline Gera Landing (núcleo da landing)

No fluxo atual, a geração da landing segue as etapas administrativas oficiais do bloco GeraLanding dentro de `experiment-pipeline`:
1. gerar wireframe (`LANDING_PAGE_WIREFRAME`), que deve enfileirar automaticamente o Gera Copy após conclusão bem-sucedida;
2. gerar copy (`LANDING_PAGE_COPY`);
3. gerar planejamento/prompt de imagens (`LANDING_PAGE_IMAGE_PLANNING`), que deve enfileirar automaticamente o Gera Imagem após conclusão bem-sucedida;
4. gerar imagens (`LANDING_PAGE_IMAGE_GENERATION`), materializando `experiment.landing_page_image_assets` com as URLs finais;
5. gerar preset de design (`LANDING_PAGE_DESIGN_PRESET`), que deve ser enfileirado automaticamente após a conclusão bem-sucedida do Gera Imagem;
6. revisar qualidade visual/comercial (`LANDING_PAGE_QUALITY_REVIEW`);
7. gerar entregáveis finais da landing (`LANDING_PAGE_DELIVERABLES`, alias operacional legado `landing-page-html`).

### 5.1 Observação obrigatória — HTML provisório por etapa
Durante o pipeline de Gera Landing, existe produção incremental/provisória de conteúdo para permitir evolução etapa a etapa. No estágio de design preset é consolidada a base visual e ocorre a etapa usada para ingestão do pixel no fluxo atual. Ao concluir o Gera WireFrame com sucesso e persistir `experiment.landing_page_wireframe`, o backend deve criar automaticamente uma execução `landing-page-copy` com `promptTemplateId` operacional `auto/wireframe`. Ao concluir o Gera Prompt Imagem com sucesso e persistir `experiment.landing_page_image_planning`, o backend deve criar automaticamente uma execução `landing-page-image-generation` com `promptTemplateId` operacional `auto/image-planning`. Ao concluir o Gera Imagem com sucesso e persistir `experiment.landing_page_image_assets`, o backend deve criar automaticamente uma execução `landing-page-design-preset` com `promptTemplateId` operacional `auto/image-generation`, mantendo a continuidade do fluxo sem exigir novo clique do usuário.

### 5.1.1 Insumos MOIS para orientar GeraLanding

As etapas `landing-page-wireframe`, `landing-page-copy`, `landing-page-image-planning` e `landing-page-design-preset` podem receber, no contrato `pending`, o campo `geralandingReferenceInsights` com referências estruturadas da Biblioteca MOIS de páginas já analisadas e bem pontuadas. Esse insumo existe para transferir padrões abstratos de páginas vencedoras para o pipeline de geração, como estrutura comercial, função da copy, papel das imagens e direção visual.

O uso desses dados é auxiliar: o contrato do experimento atual continua sendo a fonte principal de verdade. É proibido copiar texto, marca, URL, promessa específica, identidade visual ou claims de páginas externas. O worker deve usar as referências apenas para inferir padrões reutilizáveis e adequá-los ao produto, nicho, hipótese e oferta do experimento em execução.

### 5.2 Instrumentação obrigatória de funil no assembler do design preset
Para a etapa `LANDING_PAGE_DESIGN_PRESET`, o assembler de HTML provisório deve injetar instrumentação mínima de comportamento para diagnóstico de avanço de funil na landing:
1. disparo de `page_view` no carregamento da página;
2. marcação explícita das seções monitoráveis (`data-track-section` derivado de `data-section-id`/`id`);
3. medição de tempo de visualização por seção usando `IntersectionObserver` com critério de visibilidade (>= 50%);
4. emissão de evento consolidado por seção (`section_view_time`) com `sectionId` e `elapsedMs` sempre que a seção deixa de ficar visível, quando a aba fica oculta e no `beforeunload`.

Regras complementares:
- a instrumentação deve ser idempotente (não pode ser injetada em duplicidade no mesmo HTML);
- o payload publicado no artefato final deve manter apenas campos/eventos previstos em contrato canônico, sem metadado técnico fora do escopo funcional.

### 5.3 Contrato canônico de analytics público da landing

A landing publicada deve emitir eventos públicos de analytics suficientes para diagnosticar intenção comercial, recorrência provável e gargalos de conversão sem transformar a mensuração em identificação determinística de pessoa real. O contrato mínimo dos eventos emitidos pela landing é:

| Campo | Obrigatoriedade | Semântica canônica |
|---|---|---|
| `eventId` | obrigatório em eventos novos | Identificador único do evento gerado no cliente para rastreabilidade e deduplicação operacional. |
| `eventType` | obrigatório | Tipo do evento público. Valores canônicos iniciais: `page_view` e `section_view_time`; novos tipos devem ser documentados antes do uso. |
| `visitorId` | obrigatório em eventos novos; opcional apenas para legado | Identificador first-party persistente do visitante provável no mesmo navegador/dispositivo. Não prova pessoa real, não deve ser tratado como identidade civil, login, e-mail ou usuário determinístico. |
| `sessionId` | obrigatório em eventos novos | Identificador da sessão de navegação atual no navegador/aba. Deve mudar quando a sessão local expirar ou for recriada. |
| `sectionId` | obrigatório para `section_view_time`; ausente ou nulo em eventos sem seção | Seção monitorada da landing, derivada de `data-section-id`, `id` ou contrato equivalente de seção rastreável. |
| `elapsedMs` | obrigatório para `section_view_time` quando representar duração consolidada | Tempo total, em milissegundos, usado por eventos de permanência/visualização de seção. |
| `visibleMs` | opcional, recomendado para eventos de visibilidade | Tempo efetivamente visível, em milissegundos, quando a implementação diferenciar tempo decorrido de tempo visível. |
| `pageUrl` | obrigatório em eventos novos | URL da página onde o evento ocorreu, preservando parâmetros úteis de atribuição quando aplicável. |
| `occurredAt` | obrigatório em eventos novos | Data/hora ISO-8601 gerada no momento da ocorrência observada no cliente; o backend deve manter data de recebimento separada quando necessário. |
| `userAgent` | obrigatório em eventos novos quando disponível no navegador | User-Agent informado pelo navegador para apoio diagnóstico. Não substitui `visitorId` e não deve ser usado para fingerprinting agressivo. |

Semântica operacional obrigatória:
- `visitorId` representa apenas um visitante provável por navegador/dispositivo first-party. Ele pode ser apagado pelo usuário, bloqueado por restrição de storage, reiniciado por troca de navegador/dispositivo e compartilhado por pessoas diferentes no mesmo ambiente. Portanto, a UI e os relatórios devem comunicar “visitante provável”, nunca “mesma pessoa comprovada”.
- `sessionId` representa uma sessão de navegação e serve para separar visitas do mesmo `visitorId` em momentos diferentes. Ele não é suficiente, isoladamente, para medir recorrência entre visitas futuras.
- Evento é a observação pontual emitida pela landing (`page_view`, `section_view_time` e futuros eventos documentados). Evento não é sessão nem visitante.
- Visitante recorrente provável existe quando o mesmo `visitorId` aparece em mais de uma sessão (`sessionId` distinto) do mesmo experimento ou quando existem ao menos dois `page_view`s válidos do mesmo `visitorId` separados por intervalo maior que a janela canônica de deduplicação.
- A janela canônica inicial de deduplicação de `page_view` repetido em curto intervalo é de **3 segundos** por experimento, `visitorId`, `sessionId`, `eventType` e `pageUrl`. `page_view`s repetidos dentro dessa janela devem ser tratados como duplicidade operacional para contagem de recorrência e volume, preservando auditoria bruta quando o modelo de dados permitir.
- Eventos legados sem `visitorId` continuam válidos para analytics histórico de sessão e funil, mas não podem ser usados para afirmar recorrência provável de visitante entre sessões. Quando exibidos junto a dados novos, devem ser identificados como legado/sem visitante provável.
- O backend deve aceitar eventos legados sem `visitorId` enquanto houver landings antigas publicadas, mas toda nova publicação deve enviar `visitorId` e `sessionId` conforme este contrato.


### 5.4 Quadro operacional — etapas, assembler de HTML e persistência de HTML provisório

| Etapa | Classe que faz assembler do HTML da etapa | Campo(s) de tabela onde grava HTML provisório |
|---|---|---|
| `LANDING_PAGE_WIREFRAME` | `WireframeProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_COPY` | `CopyProvisionalHtmlAssembler` | `gera_landing_stage_execution.provisional_html` |
| `LANDING_PAGE_IMAGE_PLANNING` | `ImagePlanningProvisionalHtmlAssembler` (usa internamente `CopyProvisionalHtmlAssembler` + `LandingPageImageInjector` apenas para esta etapa). | `gera_landing_stage_execution.provisional_html` (não persiste em `experiment.landing_page_html` nesta etapa). |
| `LANDING_PAGE_DESIGN_PRESET` | `DesignPresetProvisionalHtmlAssembler` + `LandingPageImageInjector.injectImages(...)` | `gera_landing_stage_execution.provisional_html`, `experiment.landing_page_design_preset` (JSON bruto da resposta do modelo) e `experiment.html_geralanding` (HTML consolidado da etapa). `experiment.landing_page_html` só é persistido na aprovação/publicação. |

### 5.5 Regra de isolamento por conjunto (obrigatória)

Cada conjunto de montagem de HTML deve atuar **exclusivamente** na sua etapa canônica, com pacote dedicado dentro de `com.marketinghub.geralanding`:

- `com.marketinghub.geralanding.wireframe` → etapa `LANDING_PAGE_WIREFRAME`
  - `WireframeProvisionalHtmlAssembler`
  - `WireframeHtmlGenerator`
- `com.marketinghub.geralanding.copy` → etapa `LANDING_PAGE_COPY`
  - `CopyProvisionalHtmlPayloadResolver`
  - `CopyProvisionalHtmlProcessor`
  - `CopyProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.presetdesign.provisorio` → HTML provisório da etapa `LANDING_PAGE_DESIGN_PRESET`
  - `DesignPresetProvisionalHtmlProcessor`
  - `DesignPresetProvisionalHtmlAssembler`
- `com.marketinghub.geralanding.imageplanning` → etapa `LANDING_PAGE_IMAGE_PLANNING`
  - `ImagePlanningProvisionalHtmlAssembler`

Regras:
1. Um conjunto de etapa não pode consolidar regras de outra etapa.
2. Enriquecimentos transversais (ex.: injeção de URLs finais de imagem) devem ocorrer em serviço auxiliar dedicado e orquestrados pelo serviço da etapa, sem transferir a responsabilidade de etapa entre processadores.
3. A etapa de geração de imagens deve persistir o manifesto consolidado `experiment.landing_page_image_assets`; a etapa de preset design deve consumir esse manifesto para substituir placeholders/URLs provisórias por URLs finais antes de persistir o HTML.

### 5.6 Quality Review visual — fonte canônica do prompt

A etapa `landing-page-quality-review` deve avaliar o artefato final publicável com base somente em:

1. `experiment.html_geralanding`, exposto ao Worker AI como `htmlGeraLanding`;
2. screenshots renderizados a partir desse mesmo HTML.

O prompt textual do Quality Review não deve receber `experiment.landing_page_html` como fallback legado, nem JSONs intermediários de wireframe, copy, image planning, image generation ou design preset. A causa-raiz apontada pelo Quality Review deve ser inferida apenas a partir do HTML final e da evidência visual renderizada, preservando o foco no artefato que será publicado e evitando falhas quando `landing_page_html` ainda estiver nulo antes da aprovação/publicação.

### 5.7 Contrato operacional de `landingPageHtml`

A etapa `LANDING_PAGE_HTML` / `landingPageHtml` deve gerar um documento HTML final completo, publicável e livre de metadados técnicos internos. A resposta do gerador deve ser HTML puro; envelopes como `{ "landingPageHtml": { "htmlDocument": "..." } }` são quebra de contrato da etapa.

O HTML final da landing deve implementar, no mínimo:
- listener de `submit` no formulário alvo;
- `event.preventDefault()`;
- gate de validação com `checkValidity()` e `reportValidity()`;
- envio assíncrono com `fetch(form.action, ...)`;
- payload usando `new FormData(form)`;
- controle de loading no botão de submit, desabilitando durante a requisição e restaurando depois;
- feedback inline de sucesso/erro ao usuário;
- acabamento visual do feedback como banner/card coerente com `landingPageDesignPreset`, incluindo fundo semântico, borda sutil, espaçamento, contraste AA e leitura clara no mobile.

A geração deve preservar a paridade entre variantes públicas quando houver geração dupla de landing: a mesma entrada canônica e as mesmas validações críticas devem ser aplicadas às variantes `deterministic` e `ai`, mantendo comparabilidade comercial entre os outputs.

### 5.8 Worker AI — divisão equivalente por etapa (obrigatória)

No `ai-worker`, a mesma divisão por etapa deve ser mantida para evitar acoplamento entre execução,
prompt e schema. A arquitetura canônica vigente para qualquer etapa de landing no Worker AI é o núcleo
`com.marketinghub.worker.openai.core.<etapa>`. Não existe modelo canônico ativo em namespace Java
específico de GeraLanding dentro do Worker AI.

- Cada etapa deve residir em `com.marketinghub.worker.openai.core.<etapa>` e usar `StageWorker`,
  `StageBackendPort`, `StagePromptBuilder`, `StageResponseValidator`, `StageResponseHandler` e
  `OpenAiClientPort`.
- Scheduler, propriedades, adapters de backend, prompt builder, validador e handler devem ser beans
  declarados pela configuração explícita da própria etapa, sem depender de services genéricos do antigo
  fluxo de landing.
- O antigo namespace Java de landing do Worker AI é legado, não deve receber novas classes e não
  deve ser usado como referência arquitetural; qualquer remanescente deve ser tratado como débito de
  migração para `openai.core.<etapa>`.
- Cada etapa do `openai.core` deve resolver seu próprio prompt/schema por configuração tipada e por
  contratos de input/output próprios.

Regra operacional: a seleção de schema/prompt por etapa no worker deve ocorrer por definição de etapa
explícita, sem ifs ad-hoc espalhados no serviço de execução. Etapas novas ou remanescentes devem entrar
diretamente no padrão `openai.core.<etapa>`.

## 6. Geração e aprovação de anúncios

Após os artefatos de base:
1. gera anúncios com IA;
2. usuário realiza aprovação operacional dos anúncios.

## 7. Aba Landing: visualização, aprovação e URL de campanha

Com a landing gerada:
1. o usuário acessa a aba **Landing** no experimento;
2. faz visualização e aprovação/publicação;
3. nesse ponto é consolidada a URL final usada na campanha.

### 7.1 Bloqueio de alterações após liberação/publicação

Depois que o experimento for liberado para publicação/execução, a interface administrativa deve tratar o experimento como operacionalmente bloqueado para alterações. O bloqueio começa quando `facebook_release_requested_at` estiver preenchido ou quando o status já representar execução ou pós-execução (`RUNNING`, `PAUSED`, `USER_STOPPED`, `VALIDATED`, `INVALIDATED`, `INCONCLUSIVE` ou `FINISHED`).

Enquanto bloqueado, o frontend deve manter disponíveis as visões de acompanhamento e auditoria (Overview, Funil, Analytics, Chamadas Meta, Jobs e prévias), mas deve desabilitar comandos e campos que alterem artefatos ou configuração do experimento, incluindo edição do experimento, reset de campanhas, geração/regeneração de landing, aprovação/publicação de landing, geração/aprovação/exclusão de criativos, seleção de público e registros manuais no funil.

A regra existe para impedir divergência entre o que já foi publicado/executado no Meta/Lead Portal e os artefatos canônicos usados para mensuração comercial do experimento. Correções após publicação devem seguir fluxo operacional explícito (ex.: duplicar/republicar novo experimento ou rotina de parada/correção autorizada), não edição silenciosa dos ativos em execução.


## 7.2 Ownership canônico dos criativos usados pelo Facebook

A criação, geração por IA, edição, aprovação e exclusão de criativos pertencem exclusivamente ao módulo Experimentos. Essas ações devem continuar usando contratos do domínio Experimentos, pois o criativo é artefato do experimento e compõe a decisão comercial antes da liberação.

O módulo Facebook pode apenas consumir criativos já aprovados para publicação. Para isso, o backend deve expor contratos de leitura exclusivos no módulo Facebook, começando por `GET /api/facebook-campaigns/experiments/{experimentId}/creatives-ready`, que retorna somente criativos `READY` do experimento no formato necessário ao `facebook-ads-worker`. É proibido usar endpoints do módulo Facebook para criar, editar, aprovar ou excluir criativos.

Essa separação evita acoplamento indevido: Experimentos mantém a responsabilidade pelo artefato e Facebook mantém a responsabilidade pela publicação e consumo operacional.

## 8. Regras de integração com OpenAI

### 8.1 Worker AI como camada obrigatória
Toda chamada para OpenAI no contexto deste fluxo é mediada pelo Worker AI. O frontend e demais módulos não devem chamar OpenAI diretamente para essas etapas.

### 8.2 Modo de processamento OpenAI
No fluxo de **Gera Landing**, a integração canônica com OpenAI deve usar **Flex processing** na API de `responses`, definindo `service_tier=flex` em cada requisição do Worker AI.

Para outros fluxos que ainda usam lote assíncrono (ex.: alguns jobs de criativos/imagem), o modo batch continua permitido quando o contrato operacional exigir processamento em arquivo JSONL com polling.

Regras obrigatórias do modo Flex no Gera Landing:
- usar endpoint síncrono `/v1/responses` com `service_tier=flex`;
- configurar timeout de cliente compatível com latência maior do Flex;
- tratar indisponibilidade de capacidade (`429`) como falha de integração com contexto operacional em log.

## 9. Publicação da landing

A publicação do HTML final da landing ocorre no Lead Portal, com integração feita pelo fluxo de backend/worker.

### 9.1 O que acontece depois de clicar em "Aprovar e publicar landing"

Classe responsável no backend: `BackendPublicLandingService` (método `approveEndPublish`, também exposto pelo alias `approve-and-publish`).

Fluxo obrigatório executado após a aprovação:
1. carregar o experimento e resolver o HTML base puro para publicação a partir de `experiment.html_geralanding`; `experiment.landing_page_html` só pode ser usado como fallback legado quando ainda não houver `html_geralanding`;
2. manter `experiment.html_geralanding` como artefato fonte puro: HTML + CSS de apresentação, sem scripts de funil, pixels, tags de analytics, `gtag`, Google Tag Manager, `fbq`, Meta/Facebook Pixel, `data-mh-funnel-tracking`, `data-mh-funnel-controls` ou `data-mh-landing-analytics`;
3. criar uma cópia publicável enriquecida, persistida em `experiment.landing_page_html`, contendo toda a instrumentação necessária para venda e mensuração;
4. quando a landing possuir controles mínimos de captura (`nome`, `email` e botão de envio) e ainda não possuir contrato de submissão, injetar na cópia publicável o envio canônico `lead-portal-submission-engagement.v1` para `/api/public/lead-portal/flows/{slug}/submission`;
5. injetar nessa cópia publicável a instrumentação de tracking comportamental (`data-track-section` + script `data-mh-funnel-tracking`);
6. injetar nessa cópia publicável os controles de funil (`data-mh-funnel-controls`);
7. resolver os pixels configurados para o experimento/nicho e injetar na cópia publicável os snippets de mensuração elegíveis, incluindo Google/gtag/GTM quando contratado e Meta/Facebook Pixel quando houver `facebookPixelId`;
8. publicar o flow no Lead Portal via `PUT /api/flows/{slug}` com payload contendo `slug`, `name`, `description` e `customFormHtml` igual ao HTML publicável enriquecido (`experiment.landing_page_html`);
9. resolver URLs finais de publicação (`iframe` e `standalone`) e persistir no experimento a `follow_up_action_url`.

Regras adicionais:
- a aba Landing do frontend deve usar `experiment.html_geralanding` para prévia limpa do HTML/CSS gerado e `experiment.landing_page_html` para prévia/publicação instrumentada quando a publicação já tiver sido aprovada;
- `experiment.html_geralanding` nunca deve ser sobrescrito com scripts, pixels ou marcadores operacionais de mensuração; se qualquer etapa precisar enriquecer o HTML, deve gerar uma cópia e gravá-la em `experiment.landing_page_html`;
- a injeção de submissão, tracking e pixels deve ser idempotente no HTML publicável (não duplicar quando já existir em `landing_page_html`);
- falhas de contrato na publicação para Lead Portal devem ser tratadas pela exception canônica de violação de contrato do GeraLanding.

## 10. Custos e mensuração por experimento

As solicitações OpenAI do fluxo são mensuradas, registradas e totalizadas no contexto do experimento, permitindo acompanhamento financeiro por geração.

## 11. Transparência operacional para o usuário

A interface de Experimentos deve manter abas/visões que permitam acompanhar, de forma organizada:
- o que foi enviado para geração;
- o que foi recebido;
- status das etapas;
- facilidade de copiar/baixar artefatos.

## 12. Checklist canônico de execução

1. Criar experimento na tela.
2. Entrar no detalhe e executar etapas do pipeline inicial (`campaign angle` → `ad copy` → `ad image briefing`).
3. Executar etapas do Gera Landing (`wireframe` → `copy` → `image planning` → `image generation` → `design preset` → `landing html`).
4. Gerar anúncios com IA.
5. Aprovar anúncios.
6. Ir para aba Landing, revisar/aprovar/publicar landing e confirmar URL final para campanha.
7. Validar custos/telemetria da geração no experimento.

## 13. Fonte de verdade técnica (código)

- Ordem/seções do pipeline no backend: `ExperimentPipelineSection`.
- Clientes OpenAI do pipeline e Gera Landing no Worker AI.
- Prompts versionados no Worker AI em `src/main/resources/prompts/...`.
- Informações tratadas pela OpenAI, incluindo framework de hipótese e atributos do experimento: `docs/canonical/openai-informacoes-tratadas-canon.v1.md`.
- Endpoint de aprovação/publicação de landing no módulo Gera Landing do backend.

## 14. Governança de evolução deste cânone

Quando houver alteração de regra operacional (ordem de etapas, critérios de aprovação, publicação, modo OpenAI, custos ou observabilidade), este documento deve ser atualizado imediatamente junto com:
- o documento canônico de artefatos aplicável;
- testes e contratos do backend/worker afetados.

## 15. Unificação canônica do Gera Landing (vigente em 2026-05-23)

Este documento passa a ser a **única fonte canônica** para definição operacional do Gera Landing e do procedimento de experimento.

- Documento descontinuado: `docs/gera-landing/modelo-canonico-gera-landing.md` (mantido fora de uso e removido da referência ativa).
- Toda nova regra de etapa, contrato, máquina de estados, publicação e auditoria do Gera Landing deve ser registrada somente em `/docs/canonical`.

### 15.1 Ordem canônica Total Gera Landing

1. `landing-page-wireframe`
2. `landing-page-copy`
3. `landing-page-image-planning`
4. `landing-page-image-generation`
5. `landing-page-design-preset`
6. `landing-page-deliverables`

### 15.2 Regra mandatória para HTML provisório da etapa preset design

Para a etapa `landing-page-design-preset`, o HTML provisório **deve** ser gerado pelo `DesignPresetProvisionalHtmlAssembler` e persistido na execução da etapa.

Persistência esperada na conclusão da etapa:
- `gera_landing_stage_execution.provisional_html` recebe o HTML provisório da etapa;
- `experiment.landing_page_design_preset` recebe o JSON de resposta da etapa;
- `experiment.html_geralanding` recebe o HTML consolidado da etapa com imagens resolvidas a partir de `experiment.landing_page_image_assets` quando disponível.

`experiment.landing_page_html` permanece reservado para fluxo de aprovação/publicação final.

### 15.3 Regra de validação de implementação

Qualquer correção de falha onde o preset design não gerar HTML provisório deve validar, no código, estes pontos mínimos:
1. orquestração da etapa `landing-page-design-preset` no `GeraLandingStageExecutionService`;
2. uso explícito do `DesignPresetProvisionalHtmlAssembler` na montagem do HTML;
3. persistência do resultado em `provisional_html` da execução e em `experiment.html_geralanding`;
4. cobertura por teste unitário da etapa garantindo que o HTML é produzido/persistido.



### 15.4 Diagramas arquiteturais por módulo (base ArchUnit) — Gera Landing

Os diagramas canônicos de arquitetura do GeraLanding (backend e worker ai), derivados das regras ArchUnit, ficam centralizados em:

- `docs/canonical/geralanding-arquitetura-canon.v1.md`

> Observação: manter este procedimento como referência de fluxo, o documento acima como referência primária de arquitetura do módulo GeraLanding e `docs/canonical/pipeline-operacional-canon.v1.md` como referência primária para o padrão geral de criação de pipelines, telas e cards.

### 15.5 Regra mandatória — bloqueio de metainstrução na copy final

É obrigatório bloquear a publicação quando qualquer campo textual final da landing contiver metainstrução ou texto técnico operacional (ex.: instruções de montagem, placeholders, notas para operador/IA como "preciso do targetSectionId real").

Critérios mínimos de bloqueio:
1. validar campos de texto final (incluindo `bodySections[*].items[*].texto`) antes da persistência/publicação;
2. lançar erro explícito de contrato com caminho do campo e conteúdo literal rejeitado;
3. tratar a falha como causa-raiz de geração (prompt/mapper/validador), proibindo correção manual ad hoc no payload publicado.

Mensagem padrão recomendada: `IllegalStateException: Copy inválida: vazamento de metainstrução/texto técnico detectado em <campo>=<valor>`.

### 15.6 Regra mandatória — pouco esforço na copy da landing

A etapa `landing-page-copy` deve aplicar o princípio de pouco esforço em todos os campos textuais finais: o usuário não quer trabalhar para entender a comunicação da página. A copy deve ser clara em leitura rápida, respeitar os limites `tamMinimo`/`tamMaximo` definidos pelo wireframe, evitar explicações longas sem necessidade, não multiplicar escolhas de ação e conduzir naturalmente para o CTA ou próximo passo previsto na estrutura do wireframe.

Essa regra não autoriza criar novos blocos, seções, FAQs, CTAs ou metadados: o wireframe permanece a única fonte de verdade estrutural, e o princípio de pouco esforço deve ser aplicado somente dentro dos textos que o wireframe já solicitou.


### 15.7 Regra mandatória — imagens úteis no wireframe da landing

A etapa `landing-page-wireframe` deve planejar imagens por função comercial, não por preenchimento visual obrigatório. Imagens só devem existir quando cumprirem uma função explícita de prova, demonstração do produto, antes/depois, explicação do mecanismo ou redução de objeção.

Critérios mínimos:
1. a página deve conter normalmente entre 2 e 4 imagens úteis, salvo quando o nicho exigir mais prova visual concreta para sustentar a venda;
2. pelo menos uma imagem de produto/entrega continua obrigatória, representando visualmente o que o cliente compra ou recebe;
3. o hero pode conter imagem somente em container controlado, com proporção e altura máximas declaradas, sem bloco full-width desproporcional e sem competir com o CTA principal;
4. cada imagem planejada no wireframe deve trazer metadados visuais mínimos: posição desejada, proporção aproximada, limite de altura no desktop/mobile, papel de layout e relação com o CTA;
5. é proibido exigir imagem em toda seção apenas para cumprir quantidade mínima, pois isso aumenta ruído cognitivo e pode reduzir conversão.


### 15.8 Regra mandatória — padrão universal de qualidade comercial da landing

Toda landing gerada pelo Gera Landing deve ser avaliada como página de venda de produto digital, independentemente do nicho, produto, formato de isca ou experimento específico. Casos concretos, como um experimento isolado, podem servir como evidência de melhoria, mas não podem virar regra rígida do pipeline.

A narrativa comercial mínima da landing deve seguir o eixo:

> **Dor → Resultado → Mecanismo → Prova → Oferta → Ação**

Critérios universais obrigatórios:
1. **Clareza da promessa**: a primeira dobra deve comunicar dor removida e resultado desejado em poucos segundos;
2. **Especificidade do nicho**: o texto deve parecer escrito para o público real do experimento, evitando linguagem genérica que serviria para qualquer mercado;
3. **Mecanismo plausível**: a landing deve explicar por que o produto digital, método, diagnóstico, roteiro, template, plano, biblioteca ou ferramenta consegue gerar o resultado prometido;
4. **Prova concreta**: a página deve conter demonstração, preview, exemplo aplicado, antes/depois, amostra visual ou evidência funcional adequada ao tipo de produto;
5. **Oferta percebida**: os entregáveis devem ser descritos pelo benefício prático que geram, e não apenas pelo formato do arquivo ou material;
6. **CTA orientado ao benefício**: a ação principal deve vender um avanço desejável para o usuário, não apenas uma ação técnica como preencher formulário, baixar PDF ou gerar material;
7. **Hierarquia visual e mobile**: o design deve facilitar leitura rápida, destacar hero/prova/formulário e transmitir confiança suficiente para conversão;
8. **Coerência experimental**: a landing deve estar vinculada a hipótese, variável principal e métrica mensurável sempre que for usada para decisão de publicação ou aprendizado comercial.

Amostras, PDFs, mini-kits, roteiros, diagnósticos e materiais gratuitos são permitidos, mas devem funcionar como prova de valor, redução de risco ou primeiro passo da transformação. A landing não deve centralizar a promessa no formato do material quando o valor real está na melhoria prática que o produto digital entrega.

Antes de avançar para ajustes de prompt, Quality Gate ou publicação, qualquer melhoria de qualidade deve preservar esse padrão como regra universal do Marketing Hub para comercialização de produtos digitais.
