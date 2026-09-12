# Acesso ao próximo processo nos cards

Data: 12/09/2026. Escopo: `/products` e componente compartilhado com a página inicial.

## Evidências e decisão

Chromium na página pública confirmou Mira e Rigel com “Abrir próxima atividade” e Vega
com trabalho interno do ciclo (sem `nextWork`). Todos os endpoints de leitura responderam
200. Captura inicial: `artifacts/product-next-process/before-desktop.png`.
O histórico `cards-proxima-atividade-v1.md` e `vega-card-ciclo-atual-v1.md` confirma que a
orientação deve preservar a passagem do ciclo, inclusive retornos a processos anteriores.

Os contratos já existem nos módulos canônicos `businessprocess.execution` e
`businessprocesschain.learningcycle.v1`: controllers únicos em `controller`, serviços
canônicos e DTOs `record` em `service`. São reutilizados `activity-executions` e
`process-context`, documentados em `docs/swagger`, sem criar endpoint ou alterar Java.
A consulta de posição preexistente permanece como entrada; o frontend não calcula avanço.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Destacar o processo dos contratos atuais | Preserva destino, ciclo e gates; esforço baixo | Mantém as consultas atuais por card | Escolhida, atende ao novo modelo de uso |
| Criar resumo de navegação em lote | Pode reduzir payload em catálogos grandes | Novo contrato e regressão backend; esforço maior | Evolução se o volume justificar |
| Executar o processo diretamente no card | Reduz um clique | Duplica controles e consentimentos do painel; esforço maior | Fora do pedido, que solicita direcionamento |

## Matriz definida antes dos testes

| Critério | Verificação local |
| --- | --- |
| Caminho feliz | Cards → painel do processo correto, sem rolar até atividade individual |
| Produto/cadeia/ciclo | Identidades preservadas; produto sem ciclo não recebe experimento alheio |
| Subprocesso | Usa o processo/subprocesso indicado pelo backend; nenhuma escolha por nome |
| Ciclo sem próximo trabalho | Abre processo coordenador e preserva acesso às decisões |
| Conclusão e ausência | Não inventa próximo processo nem reabre ciclo encerrado |
| Pendências | Motivo e responsável oficiais permanecem visíveis, sem alegar prontidão |
| Falhas | Timeout, resposta divergente, falha de atualização e retentativa com carregamento |
| Integrações e métricas | Frontend real com HTTP simulado localmente; somente GET, sem eventos comerciais |
| Observabilidade | Capturas, lista de requisições, erros de navegador e resultados locais |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; teclado, toque e overflow |
| Regressão | Componentes/páginas afetados, TypeScript, build, Prettier e revisão do diff |

Uma rodada completa sem defeitos conclui a homologação. Se um defeito exigir correção,
executar duas rodadas completas consecutivas sem falhas após a última correção.

## Resultado

Uma rodada local completa aprovada, identificada como `round2` nos artefatos. A tentativa
de preparação `round1` parou antes de executar testes porque as dependências do frontend
ainda não estavam instaladas (`vitest: not found`); `npm ci` resolveu a preparação.
A rodada completa não revelou defeitos e, conforme a regra, não foi repetida apenas por contagem.

| Verificação | Resultado |
| --- | --- |
| Componentes e páginas relacionados | 99/99 testes, em 9 arquivos |
| TypeScript | Aprovado |
| Build | Aprovado; permanece aviso preexistente sobre tamanho de bundle |
| Navegador | 18/18 controles: 12 navegações, 3 acessos ao processo coordenador e 3 grupos de estados/recuperação |
| Formatação e diff | Prettier e `git diff --check` aprovados; arquivos novos também revisados |
| Integrações e métricas | Somente GET, zero escrita, zero chamada externa inesperada e zero erro JavaScript |

O clique pelo teclado e por toque chegou à âncora `#process-execution`, com o título do
painel visível, preservando produto, cadeia e ciclo. O backend permanece responsável por
autorizar execução e progressão. As pendências de atividade não são apresentadas como
estado inferido do processo. Processo já concluído que ainda conserva a última atividade
não é oferecido como próximo trabalho; esse caso tem proteção explícita no teste.

Fixtures locais reproduzem o retorno do ciclo de Vega a um processo anterior e Rigel sem
ciclo. O caso de Vega sem `nextWork`, observado na tela publicada nesta investigação, também
foi exercitado: abre seu coordenador oficial e mantém o acesso às decisões. O teste de
componente cobre subprocesso, posição indisponível, dados divergentes, ciclo encerrado,
carregamento, falha de atualização e histórico. Nenhuma atividade real foi executada.

Logs, capturas e requisições: `artifacts/product-next-process/round2/`. Prévia desktop:
`browser/desktop-catalog-Vega.png`; mobile: `browser/iphone-catalog-Vega.png`.
As capturas foram inspecionadas visualmente. Reproduzir com
`bash infra/testing/product-next-activity/run-round.sh <rodada>`; o caminho do runner foi
preservado por compatibilidade. O navegador de `vega-cycle-card` agora delega ao mesmo
roteiro para manter as expectativas de navegação sincronizadas.

Limites: HTTP simulado localmente sobre o frontend real, sem alteração de contratos backend.
iPhone e Pixel são emulações Chromium, sem certificação Safari ou aparelho físico.
Servidor temporário e navegadores encerrados; nenhuma topologia Docker foi criada.
Alterações de frontend, testes e documentação prontas na sandbox, sem commit, PR ou deploy.
O ganho esperado é reduzir procura e cliques até o processo; nenhum aumento de receita foi
atribuído à homologação.
