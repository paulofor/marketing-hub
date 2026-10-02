# Homologação técnica — execução #37 / Capella

Consulta de 02/10/2026: produto #7, tipo cadastrado Quartzo
(`LOW_TICKET_DIGITAL_PRODUCT`), plano #2, cadeia #25, definição #109 v6,
referência `experiment:88`. Ciclo e ficha de execução não estão cadastrados
para esta referência; não foram inferidos. As quatro atividades são
`surfaces`, `transaction`, `measurement` e `financialGuardrails`.
O pai definido é #105 v11, atividade `preflight`; não há execução desse pai
para `experiment:88` na cadeia #25.

## Causas observadas antes da implementação

- #37 está na fila: a raiz #32 de `experiment:94` aguarda autorização humana.
  O isolamento por produto é legítimo; não autoriza aprovar ou ativar #94.
- O run técnico #12 preserva a publicação #31. A publicação vigente #32,
  de 29/09, possui outro HTML e fingerprint. O bloqueio das atividades está correto.
- O GET de preflight #12 dizia `hasBlockers=false` porque agregava somente os
  status históricos. Não verificava a identidade atual já exigida pelo executor.
- O painel ocultava criação/preflight para todo experimento já publicado.
  A proteção contra criação retroativa é válida, mas também impedia renovar
  uma aprovação registrada que ficou vencida. A nova permissão vem do backend,
  por comando único, sem sobrescrever história nem permitir aprovação sem prova.
- O experimento #88 está `INVALIDATED`, com janela de 25–29/09 encerrada.
  Homologação técnica não renova essa janela nem reativa sua campanha.
- O teto do #88 é R$ 125. A revisão financeira #9, parecer Plutus #62,
  permanece vigente em 02/10, mas limita o ciclo a R$ 100. O validador conferia
  somente o máximo R$ 400 do plano comercial #2 e omitia essa comparação.
  A correção exige compatibilidade com ambos, sem modificar orçamento.

## Alternativas e escolha

| Alternativa | Benefício | Risco | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Usar os gates anteriores | Imediato | Aprovar pixels e contrato vencidos | Baixo | Recusada |
| Sobrescrever o run encerrado | Mantém um registro | Apaga a distinção entre operação e nova prova | Baixo | Recusada |
| Verificar vigência na leitura e homologar em outra tentativa | Preserva histórico e identifica a causa | Exige nova prova funcional | Moderado | Adotada |

A melhoria do harness usa os validadores Quartzo/Safira existentes. O contrato
expõe `currentEvidenceBlockReason`; a tela apresenta o bloqueio sem inferir
negócio nem alterar os gates, o experimento, a fila ou autorizações.

Para a comparação financeira foram consideradas: confiar só no máximo do plano
(baixo esforço, aceita risco fora do parecer), reduzir o limite silenciosamente
(baixo esforço, altera uma decisão persistida) e bloquear a incompatibilidade
com causa e ação (esforço moderado, preserva controles e decisão). A terceira
foi adotada. Redução de R$ 125 para R$ 100 foi apresentada ao usuário e depende
de sua resposta; a homologação não autoriza nova veiculação.

## Matriz definida antes dos testes

| Dimensão | Cenários e aceite |
| --- | --- |
| Vigência | Gate PASS antigo bloqueia; publicação atual aprovada libera; identidade ausente bloqueia; contrato de outro tipo preserva comportamento |
| História | GET não grava status/gates nem modifica experimento; tentativa antiga e resultado comercial preservados |
| Renovação | Comando único, outra tentativa com gates pendentes, permissão exclusivamente backend, dois POSTs concorrentes e retry reutilizam a mesma tentativa; ausência de prova ou tentativa independente mais recente bloqueia |
| Integração técnica | Referências completas, versão divergente, predecessoras, idempotência e retorno ao pai continuam protegidos |
| Compra/acesso/entrega | Pagamento simulado aprovado/pendente, briefing, ZIP, e-mail local, download, falhas e duplicidade; nenhuma cobrança, SMTP real ou modelo pago |
| Eventos | Interação/CTA, oferta visível, QA, bots, atribuição e deduplicação; ausência permanece desconhecida |
| Economia | Plano governante, preço, tetos, janela/paradas e parecer reutilizável; nenhuma nova autorização de gasto |
| UI | Backend simulado e tela local: bloqueio em desktop, iPhone 15 Pro e Pixel 7, painel completo e compacto; dados de QA isolados |
| Publicação | PR/revisão/checks, SHA na main, workflows aplicáveis, build e comportamento publicado |

Testes aprovados não comprovam vendas, satisfação ou lucro. O preço, oferta,
entrega assistida e direitos vendidos permanecem os cadastrados.

## Resultado da validação local

- Backend completo e regressões finais: 3.809 testes, zero falhas/erros e 24 testes condicionais
  ou já desabilitados na base. As regressões desta correção executaram sem skip.
  Frontend completo: 831 testes aprovados; typecheck e build aprovados.
- Contrato transacional existente: 11 testes aprovados, incluindo HTTP,
  persistência H2, pagamento simulado, ZIP de 24 entradas, e-mail em servidor
  local, recuperação e ausência de duplicação. Nenhum provedor pago foi usado.
- O HTTP real do controller, com validador Quartzo real e persistência simulada,
  gera as respostas usadas na tela local. Desktop, iPhone 15 Pro e Pixel 7
  aprovam estados vencido/atual, sem gravações nem chamadas externas.
  A fixture lê explicitamente UTF-8; isso evita evidência visual com acentos
  corrompidos pelo charset padrão do MockMvc.
- Renovação pelo controller HTTP e banco H2 reais: duas solicitações
  concorrentes retornam a mesma nova tentativa, com gates pendentes e histórico
  preservado. Os modos TEST/PRODUCTION sem permissão e outra tentativa
  independente mais recente recebem conflito sem criar run. A tela usa a
  resposta dessa integração para comprovar a renovação nos três dispositivos.
- Antes da correção, os cenários de vigência e incompatibilidade financeira
  falharam ao restaurar temporariamente o comportamento anterior. Depois, os
  mesmos casos passaram, juntamente com casos atuais e outros identificadores.
- A página pública #32 mostra o SHA de origem esperado. Os oito ativos e seis
  CTAs carregaram nos três dispositivos, sem erro de JavaScript nem overflow.
  Os hashes das capturas inspecionadas estão no registro JSON ao lado.
- Criativos #522/#523 v10, seus assets e a copy dos quatro anúncios da Meta
  preservam 10 posts, 10 stories, textos, R$ 67 e o destino do #88. Ambas as
  campanhas estão efetivamente pausadas; os anúncios estão `CAMPAIGN_PAUSED`.
  O storage devolveu 403 ao cliente Python padrão e 200 com user agent de
  navegador; os pixels foram baixados, inspecionados e vinculados por SHA-256.
  Essa diferença de cliente não foi tratada como defeito da experiência humana.
- HTML, JavaScript e privacidade do pós-compra publicado são idênticos à main.
  Com APIs interceptadas, os três dispositivos comprovaram campos obrigatórios,
  pagamento ausente, falha de envio, recuperação e entrega, sem gravação real.
- O banco permaneceu com 711 eventos, máximo ID 5299, antes e depois do QA;
  não há event IDs duplicados. Eventos automatizados estão qualificados.
  Uso, retorno, oferta efetivamente vista e compra não observados permanecem
  desconhecidos; a prova técnica não fabrica sinais de mercado.
- A Meta confirmou `PAUSED`/`PAUSED`. Não houve ativação, cobrança, novo modelo,
  alteração da oferta ou do orçamento do experimento.

Registro: [evidências estruturadas](capella37-evidencias-v1.json).
Custos históricos da execução, latência comparativa e custo por homologação
concluída ainda não estão disponíveis. Esta entrega mede correção funcional;
não alega economia paga nem aumento de vendas.

Reprodução do contrato integrado, após o build do frontend e com seu preview
local na porta 15173:

```bash
cd backend/ads-service
mvn -Dtest=BackendExperimentRunServiceQuartzoTest,BackendExperimentRunControllerTest \
  -Dpreflight.fixture-output=/tmp/preflight-fixture.json \
  -Dpreflight.renewal.fixture-output=/tmp/preflight-renewal.json test
cd ../../
PREFLIGHT_FIXTURE_RESULT=/tmp/preflight-fixture.json \
  PREFLIGHT_RENEWAL_RESULT=/tmp/preflight-renewal.json \
  PREFLIGHT_EVIDENCE_DIR=/tmp/preflight-evidence \
  node frontend/e2e/experiment-run-current-evidence-responsive.mjs
```

A publicação desta correção não comprova as quatro atividades: seus registros
devem ser conferidos após o deploy. A atividade financeira só pode concluir
com limite compatível e parecer vigente, sem mudar silenciosamente a decisão.
