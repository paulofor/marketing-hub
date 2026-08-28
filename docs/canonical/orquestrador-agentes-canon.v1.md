# Orquestrador de Agentes v1

## Objetivo

O Orquestrador coordena os contratos e pareceres dos oito agentes definidos em
`matriz-responsabilidades-agentes-canon.v1.md`. Ele é um mecanismo determinístico do backend, não um
agente opinativo, e não substitui a fonte de verdade de cada domínio.

Para planos de primeiras vendas, a experiência canônica é um fluxo único:
`Escolher oferta → preparar experimento → homologar jornada → publicar teste → medir vendas → ajustar ou escalar`.
Os especialistas permanecem invisíveis na operação comum e são apresentados apenas por decisão curta
(`APROVADO`, `BLOQUEADO`, `AJUSTE_NECESSARIO` ou `EM_ANDAMENTO`) acompanhada de próxima ação.

## Responsabilidades

- reconciliar cada caso pelo par imutável `commercialPlanId + experimentId`;
- consultar somente execuções e avaliações persistidas;
- normalizar cada contribuição como `REQUIRED`, `IN_PROGRESS`, `COMPLETED` ou `BLOCKED`;
- congelar identificadores e estados em evidência JSON auditável;
- impedir duplicidade por chave única;
- bloquear divergência entre o experimento atual e o snapshot recebido pelo Operador;
- indicar quando o conjunto está pronto para decisão humana.
- expor uma única etapa corrente e uma única próxima ação calculadas no backend;
- manter o detalhe de pareceres e gates como auditoria avançada, sem exigir sua leitura para operar o plano;
- acionar trabalho especializado somente quando a etapa do fluxo exigir aquela competência.

## Limites de autoridade

O Orquestrador não executa modelos, não cria opinião, não altera preço ou orçamento, não inicia ou
retoma campanha, não publica anúncio e não aplica automaticamente recomendações. Mesmo com os três
gates concluídos, o estado final é `READY_FOR_HUMAN_DECISION`.

## Divisão de responsabilidade

- Argos: entrega evidência factual, sem escolher estratégia.
- Atena: decide mercado, desejo, posicionamento, tese de oferta, portfólio e hipótese.
- Plutus: valida preço como hipótese econômica, margem, CAC, orçamento e risco.
- Dédalo: materializa PDE, landing e comunicação não audiovisual conforme a estratégia aprovada.
- Apolo: materializa roteiro, vídeo, áudio, montagem e legendas.
- Psique: avalia compreensão, desejo, prazer sensorial, esforço, confiança e objeções.
- Têmis: revisa verdade, prova, fidelidade, direitos, compliance e segurança comercial.
- Hermes: opera distribuição, instrumentação, funil e otimização a partir de eventos reais.
- Orquestrador: verifica identidade única, domínio, dependências, gates e prontidão.
- Humano: autoriza gasto, publicação, preço e decisões comerciais sensíveis.

Uma atividade de agente possui exatamente uma `responsibleAgentKey` e um
`responsibilityDomain` compatível. Revisões de Psique e Têmis nunca são coautoradas: o backend exige
as duas atividades independentes quando ambas forem aplicáveis.

## Estados consolidados

- `WAITING_FOR_AGENTS`: existe parecer ausente ou em processamento.
- `BLOCKED`: existe falha, reprovação ou divergência de contexto.
- `READY_FOR_HUMAN_DECISION`: os três pareceres foram concluídos para o mesmo contexto; nenhuma
  ação comercial foi autorizada automaticamente.

## Métricas

- 100% dos casos vinculados ao plano e experimento corretos;
- zero casos duplicados para o mesmo par;
- zero publicação ou gasto originado pelo Orquestrador;
- 100% das reconciliações com evidência persistida;
- bloqueio de toda divergência de experimento.
- tempo entre a criação do plano e o experimento homologado/publicável.

## Evolução

A v1 apenas reconcilia fontes persistidas. Filas, retries e agendamentos continuam nos módulos
executores responsáveis. Uma evolução só poderá disparar solicitações de trabalho por contratos
idempotentes explícitos, preservando a decisão de avanço no backend e os limites de autoridade.
