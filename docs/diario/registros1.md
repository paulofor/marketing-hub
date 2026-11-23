# Diario de registros

## 2026-07-26 - Product Discovery com linguagem real de consumidor

- Pergunta explicita de causa raiz: "por que esse erro aconteceu?". Resposta: os ciclos recentes do Product Discovery concluiam sem falha tecnica, mas com score baixo porque o worker montava consultas agregando tema e publico com termos amplos como "dificuldade", "review" e "como resolver"; isso gerava pesquisa generica, pouco proxima da linguagem real usada por consumidores em dores, reclamacoes, duvidas e arrependimentos.
- Alternativas avaliadas: (1) criar mais ciclos iguais, baixo esforco mas alta chance de repetir score 45; (2) escolher manualmente uma oportunidade e ir para campanha, rapido mas com risco de midia sem evidencia; (3) melhorar o gerador de queries do worker com frases reais de dor e multiplas consultas por recorte, maior aderencia a descoberta comercial e baixo escopo tecnico. Escolhida a alternativa 3.
- Ajuste aplicado em `product-discovery-worker/src/research.js`: `buildSearchQueries` agora combina frases reais por dominio (roupa online/caimento, looks/rotina e estilo/imagem pessoal) com templates genericos em primeira pessoa para PDEs futuros.
- A melhoria preserva a normalizacao de termos como `30+` para `30 anos ou mais` e remove duplicidades antes de consultar o provedor.
- Testes adicionados em `product-discovery-worker/test/research.test.js` validam consultas de arrependimento de compra online, estilo acessivel e looks para rotina real.

## 2026-11-23 — investigação e recuperação de transporte de Dédalo

- Vega 923: banco, logs históricos e container indicam perda de recebimento da reserva,
  sem prova de inferência, consumo ou entrega. PRs 5868/5869 e versões publicadas preservados.
- Preparação local: recibo persistente, início único, confirmação durante preparação,
  fencing de callback, preservação de processo original e telemetria da tarefa exata.
- Homologação e publicação devem seguir a matriz `docs/homologacao/vega-923-reserva-confirmavel-v1.md`.
  Nenhum avanço comercial, aceite, retentativa de 923 ou retomada de 103 é atribuído à correção.

### 23/11/2026 — complemento da homologação de Dédalo (PR #5870)

O CI integral identificou a fixture ProductProcessRecoveryLifecycleTest ainda no contrato
legado. Falha reproduzida localmente e teste migrado para recibo/contexto/início único,
com replay da mesma identidade, recusa do segundo início e callbacks com fencing.
As verificações de histórico, aceite e relatório continuam presentes. Validação local
com H2, recibo e controller aprovada; nenhuma inferência ou escrita no Vega real.

A revisão final do PR #5870 também fechou a substituição de auditoria em REPLAY.
Dois contextos sintéticos reproduziram a aceitação indevida antes da correção; após ela,
retorno original aceito e divergência/ausência recusadas. 176 testes relacionados aprovados
localmente, sem nova inferência, sem alterar arquitetura e sem escrita no Vega real.

### 23/11/2026 — complemento da orientação pública após PR #5870

A aplicação e o executor publicaram `adbdc343a65d91e934770e5f67aabf3b4f570f8b`.
A leitura da tela real revelou código interno na orientação legada, não coberta pela
fixture do resumo. Corrigida a projeção compartilhada sem mutar histórico, com regressão
antes/depois e fixture completa desktop/mobile. A auditoria distingue falta de registro
de prova de não execução. Homologação: 199 testes backend, 92 arquitetura, 35 frontend,
TypeScript e três perfis de navegador. Nenhuma retentativa, aceite, dispensa ou inferência
no Vega; a conferência continua somente leitura. Detalhes na homologação Vega 923.

Complemento #5871: a projeção do processo pai também é coberta pela regressão; as duas
suítes existentes de navegador passaram nos três perfis após atualização das fixtures
para contexto e auditoria adiada. Sem alterações produtivas adicionais ou no Vega real.

A fixture de transporte passou a interromper a conexão HTTP depois de receber a identidade,
antes de entregar o corpo do recibo. Recuperação após reinício aprovada localmente com a
mesma identidade e sem modelo. A melhoria torna a prova da desconexão explícita, sem mudar
o mecanismo publicado ou repetir a tarefa real.
Suíte completa do worker após esse refinamento: 125 aprovados, 1 opcional ignorado,
zero falhas/erros. Evidências preservadas em artifacts/dedalo-claim.
