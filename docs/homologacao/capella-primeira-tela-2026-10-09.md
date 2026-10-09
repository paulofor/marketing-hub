# Ações úteis na primeira tela de Capella — 09/10/2026

## Evidência e causa confirmada

Psique #676 pediu ADJUST de capella-private-v2: copiar aparecia na segunda dobra,
baixar na terceira, atrás de duas prévias com altura natural. A matriz #675
aprovou nove cenários funcionais; isso não comprova uma boa hierarquia visual.
O parecer anterior #660 já descrevia as ações após rolagem, mas atribuía nota 3;
o novo atribuiu 2. A limitação existia antes: a ordenação não mudou no ajuste de
segurança. A conferência preventiva só testava controles nomeados e ausência de
rolagem horizontal, sem medir alcance das ações na primeira tela.

Dédalo #677 consumiu US$ 0,5170628 estimados para declarar que a candidata do
ciclo continuava sendo a versão rejeitada. A regra de prontidão oferecia correção
por modelo em `experiment:...` mesmo quando a alteração funcional exige sucessor.
Esse gasto de diagnóstico não produziu implementação e fica preservado no histórico.

## Alternativas e decisão

| Caminho | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Encolher as duas imagens | Reduz altura | Pode prejudicar legibilidade e esconde o acesso às ações em telas menores | Não |
| Recolher todas as artes | Abre espaço imediato | Oculta a demonstração visual e exige novo caminho para conferir o primeiro resultado | Não |
| Antecipar copiar/baixar e recolher controles internos | Ação útil imediata, artes e proporções preservadas | Atualizar navegador e teste da expansão | Sim |

A entrada no resultado volta ao início da página. Copiar e baixar precedem as
artes; os controles de preferência/checkout simulado ficam sob demanda. Arquivos,
preço, briefing, eventos, quotas e segurança permanecem no contrato existente.
O harness mede os botões no viewport realmente mostrado, sem rolar para aprovar.
A prontidão recusa correção de rejeição funcional no experimento atual e orienta
sucessor antes de consumo por modelo. Diagnóstico de falha técnica e referência
legada de produto mantêm seus caminhos válidos.

## Matriz anterior à execução

| Caso | Critério |
| --- | --- |
| Capella/nails-v1 | Copiar/baixar inteiros na primeira tela desktop/iPhone/Pixel; pacote 36 arquivos e uso real |
| Outro produto/barber-v1 | Mesmo critério com IDs, perfil e versão diferentes |
| Recuperação | Entrada inválida recusada; arquivo 503 recuperado; mesmas ações após retorno/reload |
| Segurança | Bloqueio, causa e ação do mesmo ciclo preservados, sem pacote ou efeito externo |
| Controle preventivo | Geometria antiga, botão parcial/oculto/desabilitado/sem rótulo e controles internos abertos recusados |
| Mudança funcional | Prontidão bloqueia correção por modelo sem sucessor em duas identidades; nenhum parecer é fabricado |
| Caminhos antes válidos | Correção legada, falha técnica e retomada do mesmo contrato preservadas |
| Integração | Backend e compositor reais com MySQL 5.7; Psique/Têmis simulados e gate real |

Reutilizar `infra/testing/private-kit/run-local.sh`, projeto Compose exclusivo desta
sessão. Registrar capturas e relatórios; executar unidades, arquitetura, formatação,
`bash -n` e ShellCheck do runner. Todo teste local usa fontes fictícias e providers
simulados; não é compra, margem ou reação humana. Após publicação verificada,
CHANGE_PER_CYCLE_V1 exige candidata/ciclo/experimento sucessores; o teto autorizado
permanece cumulativo em US$ 10, contando #676 e #677, sem mídia ou vídeos pagos.

## Resultado local

A primeira rodada reproduziu uma falha no viewport efetivo de 393 × 659 do
iPhone: a introdução repetida do resultado ainda deslocava os botões. O estado
READY passou a omitir somente essa repetição, mantendo a orientação de entrada e
de bloqueio. A rodada após a correção aprovou 18 percursos, seis callbacks de
Psique com modelo simulado, dois fluxos Têmis/gate e nove grupos de controles.
As capturas e relatórios estão em `artifacts/capella-first-actions-local-fixed/`.
Os dois perfis passaram em desktop, iPhone e Pixel, inclusive retorno ao pacote,
falha recuperável e segurança; providerCalls permaneceu zero.

Os 138 testes direcionados de backend/arquitetura e 36 testes do navegador
passaram, incluindo prontidão
de retrabalho em duas identidades e a preservação da correção técnica. O pacote
de backend foi gerado localmente e os nove testes do verificador de recursos
passaram; o verificador conferiu 4.309 classes testadas/empacotadas e 765 recursos
externos, além de inicializar o catálogo do JAR. A formatação e o diff passaram.
O runner também passou em `bash -n` e ShellCheck. As topologias
temporárias foram removidas com seus volumes. Essas provas confirmam execução
e prevenção técnica; não substituem os pareceres reais da candidata sucessora.

O CI identificou que o provedor de prontidão anterior e seu teste integram uma
atestação histórica vigente de Mira. Os dois arquivos foram restaurados sem
alteração. Entre revalidar Mira fora do escopo, remover a prevenção ou compor um
gate adicional, foi escolhido o ponto de extensão já existente de múltiplos
provedores de prontidão. `PdeFunctionalCorrectionCycleReadinessProvider` impede
somente a correção funcional sem sucessor; os gates originais continuam
responsáveis por versão, predecessores e diagnóstico. Seus seis testes incluem
o contrato publicado, outra identidade, candidata nova e falha técnica posterior.
Os testes de claim/correção e a arquitetura foram preservados. A construção local
do pacote imutável aprovou 469 arquivos e 93 manifestos, sem editar provas ou hashes.
