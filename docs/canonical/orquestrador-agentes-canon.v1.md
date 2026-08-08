# Orquestrador de Agentes v1

## Objetivo

O Orquestrador coordena os pareceres do Estrategista de Experimentos, do Operador de Crescimento e
do Especialista em Aprovação de Anúncios. Ele é um mecanismo determinístico do backend, não um
agente opinativo, e não substitui a fonte de verdade de cada domínio.

## Responsabilidades

- reconciliar cada caso pelo par imutável `commercialPlanId + experimentId`;
- consultar somente execuções e avaliações persistidas;
- normalizar cada contribuição como `REQUIRED`, `IN_PROGRESS`, `COMPLETED` ou `BLOCKED`;
- congelar identificadores e estados em evidência JSON auditável;
- impedir duplicidade por chave única;
- bloquear divergência entre o experimento atual e o snapshot recebido pelo Operador;
- indicar quando o conjunto está pronto para decisão humana.

## Limites de autoridade

O Orquestrador não executa modelos, não cria opinião, não altera preço ou orçamento, não inicia ou
retoma campanha, não publica anúncio e não aplica automaticamente recomendações. Mesmo com os três
gates concluídos, o estado final é `READY_FOR_HUMAN_DECISION`.

## Divisão de responsabilidade

- Estrategista: compara formatos e recomenda o próximo aprendizado de portfólio.
- Operador: diagnostica o experimento corrente a partir de eventos reais.
- Especialista em Anúncios: avalia criativo, copy, oferta, público, CTA e destino.
- Orquestrador: verifica identidade, dependências, conflitos e prontidão.
- Humano: autoriza gasto, publicação, preço e decisões comerciais sensíveis.

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

## Evolução

A v1 apenas reconcilia fontes persistidas. Filas, retries e agendamentos continuam nos módulos
executores responsáveis. Uma evolução só poderá disparar solicitações de trabalho por contratos
idempotentes explícitos, preservando a decisão de avanço no backend e os limites de autoridade.
