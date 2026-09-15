# Aceite de orçamento independente da preparação comercial

## Diagnóstico e escolha

Em 15/09/2026 a tela pública, seu GET e o MCP confirmaram produto 4, cadeia 14,
ciclo 2, experimento 92, versão v12, AUTHORIZATION/OPEN, revisão 13. O botão
recebe COMPLETE indisponível por ausência de superfície, landing, criativo, checkout e
público. Logs consultados por budget-authorization não retornaram linhas. O histórico
local da simplificação e LOOP-CICLO-AUTORIZACAO-SEM-SUPERFICIE-COMERCIAL mostram que
foi preservado no aceite inicial um bloqueio necessário apenas às revisões comerciais.

Alternativas: liberar só a UI (baixo esforço, API ainda rejeita); exigir preparação prévia
(mantém o impedimento de registrar os limites); separar aceite financeiro da prontidão
comercial no serviço existente (escolhida: baixo escopo, preserva controles finais).

## Matriz definida antes da alteração

- GET oferece aceite mesmo com preparação incompleta, sem gravar nem autorizar.
- POST canônico aplica diário/total, registra evento e avança a PUBLICATION, sem RUNNING.
- Homologação revogada, janela vencida, montantes inválidos, revisão antiga e replay conflitante.
- Mesmo comportamento em outros identificadores; histórico preservado e nenhuma chamada paga.
- Preparação continua explícita, revisores e ativação final mantêm gates existentes.
- Interface real em Chromium desktop, iPhone e Pixel emulados: valores, submissão, falha,
  retentativa, bloqueio legítimo e ausência de transbordamento.
- Duas rodadas consecutivas depois da última correção; testes locais com dependências
  simuladas, sem vendas, custos ou dados de homologação lançados em produção.

## Resultados

Duas rodadas locais após a correção, com 271 testes Java por rodada
(`LearningCycle*Test,SalesFlowResolverTest,PdeCommercial*Test`), 31 testes de frontend,
TypeScript e navegação Chromium desktop/iPhone 15 Pro/Pixel 7 emulados.
Resultados finais dos navegadores registrados em `artifacts/budget-disabled/browser-round*.log`.

A primeira execução de desenvolvimento identificou duas asserções antigas de rótulos
sem o asterisco obrigatório; foram ajustadas antes das rodadas finais. O teste de backend
agora injeta explicitamente a preparação incompleta: a versão anterior bloqueava o GET
ou rejeitava o comando nessa condição. A candidata oferece o comando e grava os valores,
um evento e a revisão, preservando PLANNED. O replay não duplica o aceite e conteúdo
conflitante é recusado. Casos 4/2/92 e 400/200/920 cobrem 20/100 e 30,25/120.

## Evidências e limites

- Tela pública consultada com Playwright, sem enviar aprovação financeira. MCP consultado
  por `db_query` e `java_module_logs`; arquivos locais em `artifacts/budget-disabled`.
- Fonte do formulário: GET dos ciclos → LearningCycleController → LearningCycleService.response.
  Envio: POST budget-authorization → authorizeBudget → command/apply → materializador de limites,
  repositórios do experimento, ciclo e evento. O botão bloqueado não disparava esse envio.
- Backend: testes de serviço e controller, dependências simuladas e testes existentes com H2.
  Sem migração ou alteração de schema; não se alega concorrência física MySQL nesta rodada.
- Navegador: componente e cliente HTTP reais com respostas simuladas; três perfis Chromium,
  sem validação em dispositivos físicos ou Safari. Screenshot mobile inspecionada.
- Suites antigas `cycle-commercial` foram alinhadas ao novo aceite; suas topologias completas
  MySQL não foram executadas nesta recuperação. Python/JavaScript verificados sintaticamente.
- Swagger atualizado para a rota já existente e analisado pelo parser YAML do Prettier.
  Diff revisado, sem alteração de worker, prompt de IA, dados ou publicação produtiva.

## Prevenção e situação real

A correção está no serviço compartilhado, sem exceção por identificador. Mantém homologação,
janela, autoria administrativa, idempotência, revisão e gates finais. A preparação permanece
visível com link. A causa era a confusão entre aceite financeiro e prontidão comercial,
não a falta de novos campos humanos. Aprendizado registrado no cânone e no loop conhecido.
A aplicação publicada continua com o comportamento anterior até PR/deploy do usuário.
Nenhum orçamento foi aprovado e não há receita ou ganho de conversão atribuído aos testes.
