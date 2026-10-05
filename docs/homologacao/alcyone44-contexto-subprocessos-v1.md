# Alcyone — referência preservada nos subprocessos

## Evidência e limite — 05/10/2026

Tela, API e MCP confirmam Alcyone #11 / Look para a Ocasião, Safira, plano #34 v3,
cadeia #26, processo #113 v11, execução #44 e `experiment:97`, sem ciclo nem ficha
de execução vinculada. As quatro atividades continuam pendentes: Atena #17 está
`INSUFFICIENT_EVIDENCE`, Plutus não possui parecer para #34 e Dédalo comprovou
somente fixtures internas. A revisão financeira #12 r2 mantém 29 premissas ausentes.

A proteção do PR #5496 já está no backend publicado, confirmado pelo MCP em
`cfc8755915baef47fc3054ffd8bb0d55ee6bd72d` e pelo motivo exibido na tela. A falha
antiga de publicação não é mais o impedimento comercial. Nenhuma revisão paga,
campanha ou cobrança foi iniciada nesta retomada.

O cabeçalho retorna links para #121 e #114 apenas com `chainId=26`. O clique real
removeu `sourceReference=experiment:97`; os links das próprias atividades já a
preservam. `ProcessRunNavigation.url` omite a referência e apenas `executionUrl`
a acrescenta. Isso permite que o destino volte a inferir a execução mais recente,
contrariando o contexto exato solicitado. Não foi observada mistura de resultados
do #97 com outro experimento nesta consulta.

## Escolha

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Acrescentar a referência apenas no componente | Ajuste pequeno | Contexto copiado e outros consumidores continuam incompletos | Não |
| Inferir o experimento mais recente no destino | Nenhuma alteração | Perde a identidade de históricos e execuções paralelas | Não |
| Corrigir o construtor comum do backend | Ida, retorno, atividade e contexto usam a mesma identidade | Mudança pequena com regressões de codificação | Escolhida |

## Matriz definida antes dos testes

| Caso | Critério de aceite |
| --- | --- |
| Pai/filho com ciclo | Cadeia, ciclo, referência e versões preservados |
| Sem ciclo e diferentes referências válidas | Experimento, contrato privado e plano comercial codificados integralmente |
| Atividade e reserva da fila | Mesma referência, âncora correta, parâmetro sem duplicação |
| Projeção ainda sem referência | Não inventar identidade nem escrever `null` no link |
| Pai/filho de contexto incompatível | Recusa existente preservada |
| Conclusão, pausa e catálogo atualizado | Versão delegada e histórico preservados |
| Tela local com respostas simuladas | Ida ao subprocesso e retorno mantêm a referência em desktop, iPhone e Pixel |
| Harness/contexto para agentes | Contexto copiado conserva os links canônicos; gates e fila existentes continuam protegidos |
| Isolamento e custos | Sem produção paga, escrita operacional, campanha ou métrica de venda em testes |

## Resultado local

- Quatro regressões falharam no comportamento anterior e passaram com o construtor comum.
  Os oito casos de navegação passam com referência codificada, sem duplicação nem inferência.
- Suíte completa do backend: 3.951 casos, zero falhas/erros; 25 skips preexistentes preservados
  (fixtures ambientais opcionais e um teste literal de HTML já desabilitado). Nenhum teste foi
  removido, enfraquecido ou dispensado pela alteração. Integrações H2 de fila/comando passaram.
- 66 testes existentes do frontend protegem os links e o contexto AIHUB; build concluído.
- Chromium local em desktop, iPhone 15 Pro e Pixel 7 consumiu os links exportados pela classe
  Java real com APIs simuladas. Ida, retorno, âncora e contexto copiado preservam a referência;
  nenhuma chamada de escrita, erro JavaScript ou overflow horizontal foi observado.
- Spotless dos dois Java alterados, comentários de responsabilidade/métodos e diff conferidos.
  O pacote preserva 4.212 classes testadas e 747 recursos externos; o catálogo do JAR inicializa
  513 cartões. Não há mudança de schema, imagem, dependência, prompt de modelo ou segredo.

SHA-256 do resultado local de navegação no navegador:
`894817370e540f7cd58b05b1a942e0fa2d90b213aa70028ca6609a67d77a8fb8`.
SHA-256 dos links exportados pela classe Java:
`151891b930aa8cb2becfe60bb96db023285351b1d67460d133fad3e28e38e56f`.
Esses testes comprovam navegação e segregação, não os quatro objetivos comerciais.

## Situação comercial

4.1 requer prova personalizada e economia antes do aceite estratégico e da comunicação.
4.2 e 4.3 dependem desse contrato e dos pareceres próprios. 4.4 integra somente versões
aprovadas. A correção de navegação não satisfaz esses objetivos nem autoriza gasto.
As condições de entrega, preço cadastrado de R$79 e nome comercial são preservados.

## Fontes públicas reconferidas sem modelo

O leitor versionado `experiment-strategist-worker/src/main/resources/browser/public-research.mjs`
foi executado com Chromium na sandbox em 05/10/2026, em modo somente leitura. As três
páginas responderam HTTP 200; não se repetiu aqui a falha histórica de namespace. Esta
consulta não reescreve Atena #17, não reclassifica o parecer e não comprova a entrega comercial.

| Fonte | SHA-256 do texto observado pelo leitor |
| --- | --- |
| [Resolva](https://www.resolvameulook.com/book-online) | `0e8e6dd4d7bdb32b3b8295af7a87f42ede9ee47e889b49ae0aaf101b57af1174` |
| [Style DNA](https://play.google.com/store/apps/details?id=style.dna.app&hl=pt_BR) | `33c839572eadfdcd652a8df326015267c699389fe4741e67dcc7c9168ecf8442` |
| [Dressly](https://play.google.com/store/apps/details?id=world.dressly.fashion) | `955573c7caf91799cd8a9029bbc6c31a483ae187888841f78049a4cae4cde692` |

O hash identifica o texto observado; não é uma cópia imutável da página nem prova de
compra, demanda ou preço efetivamente pago. A próxima avaliação deve reutilizar fontes
ainda válidas e se concentrar na prova real e na economia, sem repetir a mesma pesquisa
paga com os mesmos impedimentos. O novo teto de consumo permanece pendente de autorização.
