# Íris — aceite estratégico antes da materialização

## Evidência e escopo — 04/10/2026

O Processo 4 #113 v11, execução #44, produto #11 (Alcyone, nome público Look para a Ocasião),
usa `experiment:97`, plano comercial #34 v3 e cadeia #26. Não há ciclo nem ficha de execução
vinculados. As quatro atividades permanecem sem comprovação. Atena #17 concluiu sua pesquisa,
mas o contrato V2 contém `INSUFFICIENT_EVIDENCE`; conclusão técnica da pesquisa não é liberação.
Plutus ainda não analisou o plano #34; revisão financeira #12 r2 tem 29 premissas ausentes.
A prova #583 cobre fixtures internas, com `providerCallsAuthorized=0` e
`commercialEvidenceEligible=false`, não geração personalizada comercial.

Backend e worker verificavam disponibilidade/hash do contrato de Atena, sem verificar seu
estado V2. A lacuna omitia o bloqueio estratégico da tela e permitiria chegar ao modelo se os
demais predecessores fossem apresentados como prontos. Os bloqueios financeiros e de Dédalo
já impediam a execução observada; nenhuma chamada adicional ocorreu durante o diagnóstico.

## Alternativas

| Alternativa | Benefício | Risco e esforço | Aderência |
| --- | --- | --- | --- |
| Reforçar somente o prompt | Mudança pequena | Depende de inferência paga para detectar a lacuna | Baixa |
| Bloquear todo contrato incompleto no provedor estratégico | Regra central | Impede outros agentes de consultar pesquisa concluída com lacunas; altera consumidores | Média |
| Validar o aceite no gate de Íris e no preflight do executor | Bloqueio determinístico anterior ao modelo, com causa visível | Mudança pequena em duas fronteiras já existentes | Alta, escolhida |

O V2 precisa de `READY_FOR_OPERATION` no artefato da mesma versão. V3/V4 privados continuam
usando seus gates próprios; não se exige liberação comercial nesses percursos. Ler a pesquisa,
preservar provas e estudar a lacuna continua permitido. A correção não altera Atena, preço,
produto, autorização de mídia nem a tentativa original.

## Matriz local definida antes dos testes

| Caso | Aceite |
| --- | --- |
| V2 aprovado e predecessores prontos | Atividade disponível e entrada aceita pelo worker |
| V2 com evidência insuficiente | Backend mostra a causa e worker não inicia Codex |
| V2 sem estado ou com versão interna divergente | Bloqueio funcional, sem exceção de leitura |
| V2 insuficiente e economia/produto ausentes | Todas as pendências pertinentes são preservadas |
| V3/V4 privados e primeiro experimento | Preparação anterior válida continua disponível |
| Comando e fila | Recusa não cria tarefa; liberação posterior permite a mesma referência |
| Reinício ou tarefa antiga congelada | Worker recusa a entrada insuficiente antes do modelo |
| Tela desktop, iPhone 15 Pro e Pixel 7 | Causa legível, referência preservada e sem requisição paga |
| Observabilidade e métricas | Motivo no relatório/execução; custo não informado não vira zero |
| Isolamento | Identificadores sintéticos nos testes, sem writes no banco operacional, venda ou campanha |

Não há mudança de schema, cobrança, concorrência/reserva, prompt nem pixel do produto.
Os testes de ciclo, fila, callbacks e recuperação já existentes são mantidos. A rodada cobre
unitários dos dois módulos, integração de comando/fila, empacotamento e tela com dependências
locais. Repetir apenas as validações afetadas se houver defeito; não publicar para testar.

## Resultado local

- Antes da correção, as regressões reproduziram sete falhas no gate do backend e seis no
  preflight do worker. Depois, passaram 3.880 testes do backend e 41 do worker; 24 e dois
  casos condicionais, respectivamente, permaneceram dispensados por suas condições originais.
- Integração H2: parecer insuficiente recebe 409, não cria tarefa e mantém fila vazia.
  A mesma referência sintética liberada cria uma tarefa; repetição não a duplica e o executor
  a reserva pelo `pending`. Custo ausente permanece ausente. O ciclo privado anterior passa.
- Spotless e empacotamento dos dois módulos passaram; nove testes do verificador de pacote
  passaram. O JAR preserva as 4.207 classes testadas e 746 recursos externos, com catálogo
  inicializado. `bash -n`, ShellCheck e o validador de arquitetura dos agentes passaram.
- Chromium local: desktop, iPhone 15 Pro e Pixel 7 exibem a causa e o caminho aprovado,
  sem erro JavaScript, overflow ou gravação operacional; as respostas vêm da integração local.
- Dockerfile versionado: imagem construída e worker `UP` com backend simulado; consultas
  ao `pending` oficial, zero POST e comando de modelo desabilitado, sem credenciais injetadas.
  O double foi empacotado após o mount da engine isolada não disponibilizar o arquivo local.
  A topologia temporária é removida depois da validação.

## Limite comercial da entrega

Atividade 4.1 exige estratégia liberada, prova personalizada e economia. 4.2 depende dela e
de revisão independente dos criativos no subprocesso #121 v10. 4.3 depende dela e de destino
aprovado no subprocesso #114 v7. 4.4 integra somente os contratos aprovados, sem autorizar mídia.

A geração real e revisões pagas requerem envelope específico aprovado; teto atual do plano
é R$0. R$24 por pacote e R$600 iniciais são hipóteses históricas, não custos conferidos ou nova
autorização. A homologação limitada deve medir fidelidade às peças, três combinações, tempo,
retomada, tentativas e custo, reutilizando os demais testes válidos. Fontes essenciais de taxas,
tributos, suporte e infraestrutura continuam necessárias à economia integral. Nada aqui
comprova compra, contribuição ou lucro.
