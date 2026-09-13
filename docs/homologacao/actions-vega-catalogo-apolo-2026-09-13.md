# Backend CI — catálogo de Apolo e homologação local

Data: 13/09/2026. Base local: `e0d1d442592294a5908e1f96d045948aeae5083b`.
Escopo: corrigir a falha do GitHub Actions após a recuperação Runway de Vega.
Esta tarefa não altera produto, experimento, campanha, gasto ou runtime publicado.

## Evidências e causa

- [Backend CI 34775747406](https://github.com/paulofor/marketing-hub/actions/runs/34775747406/job/103773415261)
  falhou em `AgentHarnessCatalogTest.catalogsEveryBehaviorFileFromEveryAgentModule`:
  o prompt `video-management-service/src/main/resources/prompts/sales-video/runway-router-v1.md`
  existe no executor, mas está ausente no manifesto do agente `videomaker`.
  A suíte descobriu 2.910 testes: uma falha, nenhum erro e oito ignorados.
- [Backend CI anterior 34744400914](https://github.com/paulofor/marketing-hub/actions/runs/34744400914)
  passou. A comparação entre as revisões `496e62fa` e `1b530d3f` confirma a inclusão
  do prompt sem alteração do manifesto. O PR #5184 já estava integrado quando
  esta investigação começou.
- A matriz `runway-gen45` delegava ao runner `runway-clip-plan`, que selecionava
  apenas testes de vídeo e arquitetura. O catálogo compartilhado não era executado.
  É recorrência de `LOOP-AGENTE-PROMPT-FORA-DO-CATALOGO`, já registrada para Têmis.
- O [Build & Deploy 34776211007](https://github.com/paulofor/marketing-hub/actions/runs/34776211007/job/103774781862),
  iniciado antes desta correção, terminou com a mesma falha nos testes do backend.
  O deploy do backend foi dispensado pela dependência que falhou. A publicação do
  executor de vídeo, independente desse job, passou. Isso não comprova a conclusão
  do processo comercial de Vega nem constitui validação desta correção local.
- Os erros anteriores do reconciliador indicavam lock ocupado durante intervenção.
  O run `34776166662` posterior terminou com sucesso. Não são a causa da falha do
  catálogo e não justificam liberar locks ou repetir publicações.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Aderência e decisão |
| --- | --- | --- | --- |
| Registrar somente o prompt no manifesto | Resolve a omissão com mudança mínima | Esforço baixo; a matriz local continua sem detectar a próxima omissão | Insuficiente para prevenir recorrência |
| Descobrir automaticamente todos os prompts no runtime | Evita omissões manuais | Esforço alto; exige outro contrato para autoria, finalidade, versões e históricos | Fora do escopo desta recuperação |
| Registrar o prompt e alinhar a matriz de vídeo à suíte e ao pacote do Backend CI | Corrige auditoria e detecta regressões compartilhadas antes de publicação | Esforço baixo; aumenta a duração local, sem custo de APIs | Escolhida: preserva curadoria e os gates existentes |

## Matriz definida antes dos testes

| Critério | Evidência exigida |
| --- | --- |
| Reprodução | Teste de cobertura falha localmente pelo mesmo prompt omitido, antes da correção |
| Caminho feliz | Catálogo de Apolo expõe o prompt uma única vez, com tipo, versão, conteúdo integral e SHA-256 exatos |
| Validações e falhas | Suíte mantém rejeição a omissões, arquivos ausentes, contratos inválidos e conteúdo sensível; verificador rejeita recursos/classes ausentes ou divergentes |
| Integração | Suíte integral do backend, incluindo arquitetura, catálogo e testes de persistência isolados; package e inicialização do catálogo dentro do JAR |
| Prevenção operacional | Runner compartilhado de vídeo executa contrato do CI, suíte integral, package e verificação do JAR; contrato impede voltar à seleção parcial e exige os gatilhos correspondentes |
| Observabilidade e métricas | Logs e relatórios separados por rodada; totais executados distinguem ignorados; nenhuma API paga ou dado comercial usado como fixture |
| Segregação | Bancos efêmeros locais e doubles dos testes; nenhuma consulta ou alteração de dados produtivos necessária para esta falha de catálogo |
| Navegadores e dispositivos | Não aplicável: não há alteração de tela, layout ou interação; a resposta do catálogo é validada no backend |
| Encerramento | Duas rodadas locais completas consecutivas sem falha depois da última correção, diff e comentários Java revisados |

Os comandos da matriz não criam commit, push, PR, execução de Actions ou publicação.
Não há migração Liquibase nem necessidade de topologia Docker nesta correção.

## Resultados

Reprodução local concluída antes da correção do manifesto: 15 testes de catálogo,
uma falha pelo mesmo arquivo omitido e nenhum erro. O novo contrato do runner
também rejeitou a seleção parcial e a ausência dos gatilhos dos runners.

Duas rodadas locais completas consecutivas passaram depois da última correção:

| Critério | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend integral, incluindo arquitetura e catálogo | 2.903 executados; zero falhas/erros | 2.903 executados; zero falhas/erros |
| Testes condicionais ou desabilitados existentes | 8 ignorados | 8 ignorados |
| Contrato do Backend CI | 9 aprovados | 9 aprovados |
| Contratos do verificador de pacote | 9 aprovados | 9 aprovados |
| Actionlint versionado, sintaxe shell e Spotless | Aprovados | Aprovados |
| Package e correspondência das classes testadas | 3.944 classes idênticas | 3.944 classes idênticas |
| Recursos externos do JAR | 431 íntegros | 431 íntegros |
| Inicialização dos catálogos comportamental e de pesquisa | Aprovada; 225 cartões | Aprovada; 225 cartões |
| Manifesto e prompt dentro do JAR | Iguais às fontes | Iguais às fontes |
| Diff e comentários da classe/métodos Java alterados | Aprovados | Aprovados |

Os 2.911 testes descobertos em cada rodada incluem oito ignorados, que não foram
contados como executados. São dois testes condicionais MySQL, dois de navegador,
três dependentes de artefatos exportados de outros fluxos e uma comparação literal
de HTML já desabilitada. Todos os 16 testes do catálogo foram executados. Nenhum
critério essencial desta recuperação ficou pendente; não se declara homologação
comercial ou nova produção de vídeo por esses resultados.

O manifesto e o prompt dentro do JAR também foram comparados byte a byte com
as fontes. Há exatamente um registro `videomaker`/`runway-router-v1.md`, versão
`v1`, com conteúdo e SHA-256 preservados.

SHA-256 do manifesto: `ba67a1030cdc42567768283b149b8b848e88422af2ecf359a916155dd414acca`.
SHA-256 do prompt original: `1e18c76945407df07608e24607fe87a62fb5407efab9ab7c57a7ba3bd9fe2585`.

Evidências locais em `artifacts/actions-vega-backend/`:

- `baseline/backend-ci.log` e `baseline/deploy-backend.log`: duas falhas do GitHub
  pela mesma omissão, preservadas sem reexecutar os workflows.
- `baseline/local-catalog.log` e `baseline/local-workflow-contract.log`:
  reprodução e regressões antes da correção correspondente.
- `run-round.sh`, `round1-progress.log` e `round2-progress.log`: comandos e gates
  da matriz desta recuperação. Executam as mesmas etapas de backend acrescentadas
  ao runner de vídeo, sem iniciar seu fluxo audiovisual, que não foi alterado.
- `round1/` e `round2/`: `result.txt`, `backend-summary.json`, `backend-reports/`,
  `backend-package-integrity.log` e `catalog-package-evidence.json` comprovam
  os resultados próprios de cada rodada e a correspondência dos recursos no JAR.

Estado da entrega: alterações locais concluídas e revisadas, sem commit, push,
PR, nova execução de Actions ou publicação nesta solicitação. Os runs vermelhos
referem-se ao código anterior; a correção precisa seguir pelo PR solicitado pelo
usuário para chegar ao GitHub. Nenhuma mudança em runtime, lock, campanha ou gasto.
