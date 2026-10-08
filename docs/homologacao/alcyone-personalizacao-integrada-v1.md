# Alcyone — personalização integrada — 08/10/2026

## Conferência da solicitação

O PR #5548 está integrado e publicado em `7e473a32a93ca09f404a51a75a3dedd0511f4254`.
Não repetir essa entrega. A comunicação do produto 11 / experimento 97 / processo 113
continua sem tarefa: meta humana, economia e geração integrada ainda não estão aceitas.
O pedido continua sendo fazer Alcyone avançar e materializar os cinco critérios, incluindo
a avaliação de vídeo; tornar a decisão visível não constitui esse aceite.

O runtime alcyone-private-v3 gera três fixtures constantes. As auditorias 115/116 possuem
imagens autênticas, mas não são consumidas nesse percurso. A autorização no plano 34
permite até USD 10 totais para homologar a geração e os pareceres necessários de Atena
e Plutus desde 05/10/2026, sem mídia, cobrança, escala ou vídeo pago.
Desde essa data: imagens 0,157510; Atena 18/19 0,33287560; Plutus 69/70/71 0,25362840.
Total conhecido estimado USD 0,744014. Atena 17 precede essa autorização, possui custo
desconhecido e continua no histórico; não declarar custo integral histórico igual ao subtotal.

## Alternativas

| Caminho | Benefício | Risco / esforço |
| --- | --- | --- |
| Trocar as fixtures pelo painel já gerado | Reutiliza custo | Continua sem ligar cada entrada à sua própria saída |
| Criar outra fábrica e outro ledger | Liberdade de implementação | Duplica persistência e recuperação existentes |
| Reutilizar auditoria de imagem, fila no backend e worker PDE com uma experiência privada ligada à entrada | Fecha entrada → execução → resultado → recuperação | Escolhido; preservar fixtures, pareceres e autorizações anteriores |

## Matriz definida antes dos testes

| Dimensão | Aceite |
| --- | --- |
| Caminho completo | Tela privada → backend principal → pending/claim → provedor simulado → callback → imagem na tela e retorno sem regenerar |
| Personalização | Outra ocasião/peças tem outra entrada e imagem; não devolver a fixture da primeira |
| Identidade | Outro produto/plano/experimento e credencial divergente são recusados; nenhuma transferência de orçamento |
| Limites | Só homologação sintética, experimento PLANNED, consentimento e autorização específica; sem publicação, pagamento, aquisição ou vídeo |
| Custos | Subtotal desde autorização, custo desconhecido bloqueia nova inferência; tentativa ativa impede concorrência paga |
| Recuperação | Request e response brutos persistidos; perda do callback reaproveita resposta; resultado inválido não é sucesso |
| Integração | Frontend PDE usa seu backend, que transporta apenas contratos PDE do backend principal; worker não acessa banco |
| Observabilidade | Identidades, versão, entrada, status, horários, custo e imagem separados do payload bruto; testes nunca contam como vendas |
| Antes válido | A experiência v3 e suas provas permanecem intactas |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7: formulário, carregamento, erro e recuperação |

Uma imagem recebida e testes determinísticos não substituem parecer independente nem
provam preferência, compra, entrega comercial ou contribuição. A meta de contribuição
não será escolhida pelo sistema. Nenhum teste chama o provedor pago.

## Resultados locais

- MySQL 5.7.44 real, changelogs originais e entidades validadas pelo Hibernate: sete
  tentativas, sete identidades e sete entradas no ledger; estimativa sintética USD 0,283500.
- API principal, gateway PDE, worker Node e ambas as telas reais: dois produtos distintos
  em Chromium desktop, iPhone 15 Pro e Pixel 7; entrada/imagem próprias, recuperação,
  credencial divergente recusada, sem overflow ou erro de página. Zero chamadas externas.
- Callback aceito com retorno perdido: outro worker reenviou o mesmo spool, com somente
  uma chamada ao provedor simulado. Request anterior ao provedor, resposta imutável.
- Custo temporariamente desconhecido preservou a imagem e bloqueou nova inferência.
  Restabelecida a fonte do preço, a mesma resposta foi reconciliada pela tela privada.
  Reiniciar o backend manteve entrada, imagem, status e custo exatamente iguais.
- A chamada visual lenta não interrompe o polling do fluxo MUSA existente; teste do
  processo Node confirma dois polls anteriores com somente uma preparação visual ativa.
- PNG apresentado sem chamada ao provedor é recusado como geração real. A identidade
  publicada inclui também a nova entrada HTML, com regressão de fingerprint.
- A primeira rodada encontrou um defeito no servidor HTTP da fixture: favicon ausente
  fazia escrever headers duas vezes e encerrava a entrega privada. Corrigido antes de
  repetir a validação afetada. Não houve publicação para descobrir esse erro.
- O gateway exigiu nomes explícitos em PathVariable: o módulo PDE não publica nomes de
  argumentos no bytecode. Teste HTTP reproduziu o defeito e passou após a correção.
- Regressões: outro produto, autorização/versão/credencial divergentes, STOP, cap/custo
  desconhecido, reserva interrompida antes da request, payload inválido, falha de aplicação
  com replay, registro perdido e recuperação sem outro modelo. Não há parecer fabricado.
- Backend principal: suíte completa executou 4.422 casos, com uma expectativa desatualizada
  no teste de reserva interrompida. Ajustada ao replay permitido antes da request; todos
  os testes afetados e a arquitetura passaram na validação seletiva posterior, incluindo
  a nova regressão que recusa imagem sem chamada real. Os demais casos não falharam.
  A execução completa usou cache de oito contextos Spring após limite real de memória
  na tentativa anterior; a validação incompleta não foi tratada como aprovação.
- Worker Node: 21 testes passaram. APIs PDE: suíte de 213 casos sem falhas após correção
  do PathVariable explícito. Frontend administrativo: oito testes afetados, tipos e build
  passaram; build Alcyone e fronteira de API também passaram. Nenhum custo sintético
  é registrado como consumo comercial.
- O primeiro CI do PR #5549 detectou um acoplamento antigo no teste de smoke: a fixture
  positiva privada de Vega usava o fingerprint histórico para comparar a fonte atual de
  Alcyone. Reproduzido na sandbox. Entre alterar a história, relaxar o validador produtivo
  ou corrigir apenas a fixture positiva, foi escolhido o terceiro caminho: contrato
  temporário ligado à fonte atual e rejeição histórica mantida. `bash -n`, ShellCheck,
  teste direcionado e homologação transacional completa em Docker passaram localmente,
  incluindo promoção isolada e rollback. Os manifestos históricos ficaram intactos.
  A atestação sucessora Vega v6 atualiza somente o hash desse teste compartilhado,
  com publicação automática desabilitada; não retoma nem altera a experiência de Vega.

Runner: `VISUAL_COMPOSE_PROJECT=<projeto exclusivo autorizado> python3 infra/testing/visual-personalization/run-local.py`.
Artefatos em artifacts/visual-personalization, não versionados nem comerciais. O runner
encerra processos próprios e remove Compose com volumes/orphans. Fixtures 95501/95502
não usam identidades produtivas ou credenciais externas.
