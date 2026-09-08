# Adoção de campanha histórica no ciclo de vendas

Data: 08/09/2026. Pedido: continuar o ciclo de Vega a partir do #91 e preparar seu sucessor.

## Diagnóstico confirmado antes da correção

A UI publicada em `/products/4/value-chain-history/processes/73/activities` abre o subprocesso
do processo 6, na cadeia v13. O catálogo impede selecionar #91 (`available=false`,
`baseline=false`). O MCP confirmou o backend no commit `ae5460e146e87acc5db7a678380bdbc354f8efb2`,
o #91 `USER_STOPPED`, nenhum run para ele e a campanha Meta `120251556536430326`, `PAUSED`,
com recibo externo persistido em 07/09. O #90 tem run produtivo próprio e continua separado.
Não existe experimento #92 nem ciclo de Vega nesta fotografia.

O código reconhecia histórico exclusivamente pelo último run produtivo publicado. O callback
oficial da Meta também persiste campanha/recibo; a publicação antiga não criou run. O histórico
de homologação de Hermes já documentava essa lacuna. Consultar logs de backend pelo MCP não
retornou linhas para `learning-cycles`: o catálogo recusa a opção antes de haver comando de criação.

## Alternativas e escolha

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Adotar o recibo histórico já persistido | Usa evidência verificável e preserva o fluxo existente | Esforço moderado; exige separar aprendizado de aprovação | Escolhida |
| Criar um cadastro dedicado de referências históricas | Permite anexar evidências de outros canais | Mais tela, persistência e operação para um recibo já disponível | Evolução futura se necessária |
| Consolidar um inventário de publicações legadas em migração | Visão central de todo o legado | Amplia escopo e requer conciliar outros produtos | Desnecessária para resolver este contrato |

A correção registra origem, referência, data e limitação no histórico do ciclo. Não cria run,
preflight, homologação ou aprovação retroativos. A elegibilidade de nova publicação permanece
estrita, com run/preflight da execução nova, versão, vídeos, gate, janela e orçamento próprios.

## Matriz definida antes de testar

| Área | Critério local |
| --- | --- |
| Histórico moderno | Reconhecer run produtivo publicado, inclusive antes de tentativa posterior não publicada |
| Histórico legado | Campanha do experimento com recibo externo e data permite adoção pausada na medição |
| Validação | Status isolado, campanha local sem recibo, run de teste e dados de outro experimento não comprovam publicação |
| Não regressão | Experimento em operação não pode ser adotado; experimento exposto não pode nascer como ciclo novo |
| Auditoria | Adoção idempotente persiste fonte e lacuna sem alterar run, campanha ou métricas; sucessor recebe essa memória |
| Ponta a ponta | BPM → adotar legado → medir → ajustar → sucessor planejado → aprendizado → planejamento → ajuste → vídeos → homologação → autorização → publicação simulada → medição |
| Gates e falhas | Recibo legado nunca substitui preflight/homologação do sucessor; saltos e versões divergentes continuam bloqueados |
| Persistência | Ciclo, eventos e sucessão em MySQL 5.7 local; reaplicação, integridade e idempotência do contrato existente |
| Integrações | Backend e UI locais, campanhas e agentes simulados; nenhuma escrita produtiva ou chamada paga |
| Métricas | Identificadores locais 91001+; zero venda, visita ou custo real; dados históricos nunca somados ao sucessor |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; seleção, decisão, memória, navegação e erros |
| Regressão | Testes de ciclos/BPM e publicação, frontend, compilação, formatação e revisão do diff |

Depois de qualquer defeito corrigido, executar duas rodadas completas e consecutivas sem falhas.
A homologação deste contrato não comprova a melhoria do produto, observações humanas ou a
publicação comercial do #92. Essas entregas precisam de evidências próprias no BPM.

## Resultado

O ensaio inicial confirmou o percurso REST/MySQL e os navegadores existentes, mas o novo teste
de legado recarregava a página após o HTTP 200 antes de terminar a atualização de cache e URL.
A navegação anterior bem-sucedida aguardava a seleção do sucessor na interface. O teste foi
ajustado para aguardar URL, fechamento do formulário e título do ciclo antes de recarregar;
também abre o painel de histórico antes de conferir a evidência. Não houve motivo comprovado
para alterar o comportamento de navegação da aplicação.

Após esses ajustes e a formatação final, duas rodadas completas e consecutivas terminaram
aprovadas, com os mesmos 11 arquivos executáveis conferidos por SHA-256:

| Controle por rodada | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Etapas da matriz automatizada | 20/20 | 20/20 |
| Testes relevantes de backend | 220 executados, sem falhas | 220 executados, sem falhas |
| Frontend | 520 aprovados | 520 aprovados |
| REST e persistência MySQL 5.7 | 16 controles aprovados | 16 controles aprovados |
| Fluxo do ciclo em navegador | 12 jornadas aprovadas | 12 jornadas aprovadas |
| Entrada pela cadeia/BPM | 18 controles aprovados | 18 controles aprovados |
| Adoção legada e memória do sucessor | Desktop, iPhone e Pixel aprovados | Desktop, iPhone e Pixel aprovados |
| Recuperação, reaplicação e idempotência do schema existente | Aprovadas | Aprovadas |
| Compilação, typecheck, formatação e diff | Aprovados | Aprovados |

O relatório Java descobre 222 testes; dois testes opcionais de navegador de vídeo permanecem
condicionados e não integram esta matriz de adoção histórica. Os sete testes novos de origem
de publicação usam consultas JPA reais com H2; o ciclo, o ledger e as decisões usam MySQL 5.7
na integração e nos navegadores. Campanhas, publicação, vídeos e pareceres são doubles locais.
Emulação mobile usa Chromium, sem afirmar validação em Safari/WebKit nativo.

Evidências locais:

- Rodada 1: `/tmp/learning-sales-cycle-round-XM1DTB`.
- Rodada 2: `/tmp/learning-sales-cycle-round-dY38o7`.
- As pastas contêm contagens, logs de API/SQL, screenshots e confirmação de limpeza.
- A consulta de recibos foi também confrontada, somente leitura pelo MCP, com o schema real
  e retornou exclusivamente a campanha `120251556536430326` do #91.
- Swagger atualizado e validado. Nenhum changelog produtivo foi alterado.

Reprodução, informando o projeto exclusivo da sandbox de cada sessão:

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-0564fec3-367e-4ccd-8264-f79fa13bc726-64ca574eb2 \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh --video-matrix
```

O script encerra banco, rede, volumes e processos locais em cada rodada. O Compose foi
reconferido sem recursos restantes. Não houve commit, PR, publicação de código, geração paga,
criação de experimento comercial ou reativação de campanha.

## Continuidade operacional

O bloqueio permanece no backend publicado até esta correção passar pelo PR/deploy do usuário.
A reconferência às 21:56 UTC encontrou um deploy posterior, `0244c71ab622`, mas o catálogo
continuava retornando `available=false` e `baseline=false` para #91. O MCP continuava sem #92.
A criação de artefatos deve então continuar pela UI, dentro do processo 6: adotar #91, reconciliar,
registrar ajuste e criar o sucessor com memória. O #91 permanece interrompido.

Na fotografia produtiva desta execução, não há #92, nova versão PDE vinculada a ele ou gate
multiagente registrado em `product:4@agent-validation-v1`. O slot mais recente continua v7,
associado ao #90. A homologação local acima comprova o contrato do ciclo, não uma melhoria
comercial já entregue. A versão melhorada, os vídeos, as revisões e as demais evidências do
roteiro Vega devem ser concluídos no BPM antes de tráfego. Teto, duração e autorização do novo
gasto precisam ser próprios do sucessor; a verba antiga não é transferida por esta correção.
