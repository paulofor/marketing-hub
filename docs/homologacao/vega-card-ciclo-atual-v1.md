# Card do Vega orientado pelo ciclo atual

Data: 10/09/2026. Escopo: início e catálogo administrativo; apresentação e navegação somente leitura.

## Evidências antes da alteração

- O Chromium reproduziu o card informado na página inicial pública: “Etapa 6 de 6”, objetivo
  do ciclo #2 misturado ao retorno original `productArchitecture` e custo acumulado do processo.
- `GET /api/products/value-chain-positions/4` usa `ProductValueChainPositionService`,
  `PdeProcessCodeResolver` e a projeção `salesFlow`. A posição comercial deriva de `product.commercial_status`.
- MCP `db_health` confirmou `marketinghubdb`. `db_query` confirmou produto 4 `ATIVO`, ciclo #1
  `ADJUSTED` no #91/cadeia 13 e ciclo #2 `OPEN/ADJUSTMENT` no #92/cadeia 14, predecessor #1.
- O contrato existente `GET /api/business-process-chains/learning-cycles/v1/products/4/process-context`
  com processo 75/cadeia 14 retorna segunda passagem, memória do #91 e próximo trabalho **3.5,
  Psique, BLOCKED**: falta protótipo executável aceito. O banco não perdeu os ciclos.
- `java_module_logs` respondeu pelo MCP, sem linhas para `cycleId=2`. O diagnóstico resulta da
  comparação entre tela, dois contratos de leitura e registros persistidos, não da ausência de log.
- Causa: o card mostrava só a projeção comercial e a atividade coordenadora do Processo 6.
  O contrato de contexto, já usado nas atividades, não era consumido pelo card.

## Alternativas consideradas

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Enriquecer a listagem de posições com o contexto completo de cada ciclo | Uma resposta unificada para o card | Aumenta custo e latência da listagem geral; exige alteração e regressão de backend | Viável; desnecessário para este escopo |
| Criar um contrato em lote específico para resumos dos ciclos | Permite otimizar muitos cards simultâneos | Novo endpoint, projeção e manutenção; maior esforço inicial | Viável para maior volume |
| Apresentar contexto oficial do ciclo e manter histórico separado | Próximo trabalho correto, memória e isolamento | Uma consulta de leitura por card com ciclo; esforço moderado | Sim |

Mudar somente o título ou regravar o status comercial não resolve a lacuna de informação:
o cadastro comercial está correto e não representa sozinho o trabalho da passagem atual.

O endpoint de contexto já pertence ao controller/service canônico versionado de ciclos, com
records e Swagger. Reutilizá-lo evita criar outra regra de avanço ou outro endpoint no backend.

## Matriz definida antes dos testes

| Critério | Validação local prevista |
| --- | --- |
| Segundo ciclo do Vega | Ordinal, #92, Processo 3, Psique e pendência; sem destaque “Etapa 6 de 6” |
| Navegação ponta a ponta | Card → atividade correta → contexto com ciclo, cadeia e aprendizado preservados |
| Aprendizado | Melhoria/hipótese e eventos anteriores com evidências e limites, inclusive cadeias antigas |
| Histórico / métricas | Tempos e custos acumulados identificados; nenhum total atribuído ao ciclo por inferência |
| Estados | Carregando, erro e retentativa, resposta vazia/divergente, bloqueado, disponível, em execução, encerrado e sem trabalho externo |
| Isolamento | Outro produto sem ciclo mantém card; nenhuma escrita, tarefa, campanha, evento comercial ou integração externa |
| Superfícies e dispositivos | Início e catálogo; Chromium desktop, iPhone 15 Pro e Pixel 7, teclado e ausência de overflow |
| Regressão | Testes de componentes/páginas/contexto, tipagem e build do frontend alterado |
| Observabilidade | Capturas, contratos de leitura, logs de testes, ausência de erro JavaScript e requisição inesperada |

As APIs serão substituídas por respostas locais identificadas como teste, derivadas dos contratos
oficiais. Nenhuma métrica ou tarefa produtiva será criada. Após correção de defeito encontrado na
rodada, exigir duas rodadas completas consecutivas aprovadas após a última alteração.

## Resultado

**Duas rodadas locais completas e consecutivas aprovadas**, `final1` e `final2`, depois dos
últimos ajustes de apresentação e testes. O código do frontend permaneceu igual entre elas.

| Controle por rodada | Resultado |
| --- | --- |
| Componentes e páginas relacionadas | 72 testes em sete arquivos, todos aprovados |
| TypeScript | `tsc --noEmit` aprovado |
| Build real do frontend | Aprovado; aviso preexistente de bundle grande, sem erro |
| Navegador | Seis percursos início/catálogo → atividade e três verificações de erro/recuperação, aprovados |
| Formatação | Prettier nos quatro arquivos TSX/CSS de implementação/teste aprovado |
| Diff | Whitespace e revisão de escopo aprovados |

O navegador executou Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação. Cada percurso
confirmou segunda passagem, #92, Processo 3, responsável Psique, pendência real, memória do #91,
limite da conclusão, fonte, melhoria/hipótese, histórico acumulado e URL com cadeia/ciclo.
A origem de custo ficou restrita ao histórico. Mira conservou sua posição independente e nenhum
card consultou o contexto de outro produto. Teclado, expansão dos detalhes e ausência de overflow
horizontal passaram. Não houve erro JavaScript, requisição de negócio inesperada ou escrita.

As tentativas preparatórias encontraram incompatibilidades no próprio teste (matcher indisponível
na versão instalada, importação de fixture JSON sem configuração correspondente, comparação de
texto composto e valores históricos repetidos). Foram corrigidas antes das rodadas finais. A
inspeção visual também ajustou o peso do texto e identificou explicitamente #91 como **experimento**.

Capturas, logs e listas de requisições estão em `artifacts/vega-cycle-card/final1/` e `final2/`.
A captura `final2/browser/desktop-home-card.png` mostra o card final. As consultas produtivas de
diagnóstico ficaram em `artifacts/vega-cycle-card/`; nenhuma é uma execução criada para teste.
Fixtures e runner reproduzíveis ficam em `infra/testing/vega-cycle-card/`.

O ambiente chegou a ficar sem espaço; foram removidas capturas exploratórias e mapas de depuração
das dependências instaladas localmente nesta tarefa. Código, tipos e dependências executáveis
foram preservados. As duas rodadas concluíram. Os servidores de teste e navegadores foram fechados
pelos próprios runners; esta homologação não utilizou topologia Docker.

Módulo de implementação alterado: **frontend**. O endpoint Java existente foi reutilizado e
documentado, sem mudança de persistência ou executor. Os testes integrados usam respostas locais
dos contratos HTTP e não equivalem a executar uma tarefa Psique ou homologar o protótipo.
O ciclo produtivo #2 continua em ajuste e a pendência de implementação da versão executável
permanece visível. A entrega corrige a orientação do card; não aprova essa atividade.

Mudanças disponíveis na worktree. Nenhum commit, push, PR ou deploy foi realizado.
