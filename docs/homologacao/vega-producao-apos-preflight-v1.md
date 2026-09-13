# Vega — continuidade após o preflight isolado

Data: 13/09/2026. Contexto preservado: produto 4, cadeia 14/v14, processo
75/v6, execução 4, ciclo de aprendizado 2, experimento 92, versão
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`, projetos 4/5 e perfis 59/60.

## Diagnóstico confirmado antes da correção

UI, endpoints oficiais e MCP (`marketinghubdb`) confirmam os ciclos de produção
15/16 em `PROVIDER_PREFLIGHT_ONLY_COMPLETED`, sem job, tarefa financeira ou
reserva. Os preflights 8/9 passaram e venceram; não são produção autorizada.
O evento 11 registra o teto humano de USD 20 para produção e revisão das duas
peças. O briefing do evento 12 preserva essa referência e as restrições comerciais.

O comando de consulta chama `/api/sales-videos/autonomy/v1/provider-preflights`.
`VideoProductionCycleService.completeProviderPreflight` encerra corretamente
essa consulta antes da reserva ou da tarefa de Plutus. A produção usa outro
comando, `/api/sales-videos/autonomy/v1/cycles`. `ProcessRunVideoGuidance` só
projetava falhas, deixando a consulta encerrada como espera automática no pai.
O teste anterior incluía explicitamente esse estado entre os casos sem orientação.
O ciclo histórico 11/#91 comprova que produção, Plutus e entrega para revisão
possuem estados próprios; o seu sucesso não autoriza reutilizar provas na v12.

Recorrência de `LOOP-BPM-DECISAO-HUMANA-COMO-EXECUCAO`: a ausência de trabalho
enfileirado deve orientar o comando necessário, sem simular execução ou aprovação.

## Alternativas comparadas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Iniciar geração automaticamente ao concluir a consulta | Remove um comando | Contraria o contrato sem cobrança; risco financeiro alto | Recusada |
| Explicar somente nesta conversa como abrir o Estúdio | Esforço mínimo | Mantém a causa da espera enganosa para outros ciclos | Insuficiente |
| Projetar a solicitação de produção no backend e preservar o comando governado | Explica a pendência e mantém preflight atualizado, Plutus e auditoria | Mudança localizada; exige regressões de identidade e estados | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Critérios |
| --- | --- |
| Reprodução | Consulta encerrada sem job deve expor solicitação de produção; teste antigo reproduz ausência de orientação |
| Caminho feliz | Processo → projeto correto → solicitação de produção → preflight novo → Plutus → Apolo → artefato e revisões independentes; aprovações humanas preservadas |
| Validações/falhas | READY e EXPIRED da consulta não autorizam gasto; bloqueios anteriores preservados; preflight/Plutus/Apolo ativos não oferecem disparo duplicado; inconsistência de tarefa/job não vira autorização |
| Integração | Backend real na fixture REST/MySQL 5.7, callbacks canônicos, suites de vídeo e financeiro com dependências locais; nenhuma API paga na homologação |
| Observabilidade | Motivo, ação, referência do preflight e histórico persistidos; leitura não escreve; custos estimados separados dos confirmados |
| Segregação | Produto, experimento, versão, papel da peça e marco temporal exatos; fixtures 91001+ sem campanha, cobrança ou métrica humana |
| Navegação | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; contexto copiado, ausência de spinner enganoso, ida ao projeto e retorno ao processo |
| Encerramento | Duas rodadas locais completas consecutivas após a última correção, revisão do diff, empacotamento íntegro antes da aplicação excepcional autorizada |

Evidências brutas da investigação: `.sandbox/vega-recuperacao/`. Resultados e
situação operacional serão registrados após a execução da matriz.


## Impedimento adicional reproduzido localmente

A reprodução do contrato de produção de quinze segundos falhou nos dois aliases
Gen-4.5: o backend fornecia quatro cortes; o schema e o planejador do executor
exigem cinco funções. Comparadas três opções: reduzir a exigência do planejador
(esforço baixo, perda de prova comercial); aumentar a duração (esforço médio,
mudança de material e custo); distribuir cinco funções nos mesmos quinze segundos
(esforço baixo, custo de clipes preservado). Escolhida a terceira. O teste agora
consome o metadata exportado no planejador real, com somente a IA simulada.
O plano técnico do Estúdio deve refletir cinco cortes antes da solicitação real.

## Finalização e prova privada — ampliação antes da homologação final

A revisão do caminho completo confirmou que a rota genérica só entrega clipes brutos.
O histórico Product UGC possui pós-produção automática e prova determinística; essa
integração não existia na rota genérica escolhida para a v12. Não houve geração paga
para descobrir essa diferença. As duas rodadas anteriores passaram, mas a homologação
final deve ser reiniciada depois desta ampliação causal.

Alternativas: (1) editar e carregar arquivos manualmente — pouco código, mas quebra
a automação e a auditoria de produção; (2) trocar para Product UGC — usa caminho já
existente, porém exige referências HTTPS públicas e aumenta a estimativa de render;
(3) inserir a prova privada pelo executor e acionar a finalização existente — esforço
local moderado, mantém rota, orçamento e captura privada. Escolhida a terceira.

Matriz adicional definida antes dos novos testes: captura técnica aprovada da versão
exata; recusa de produto, experimento, versão, hash, tarefa ou enquadramento divergentes;
download apenas pelo controller do módulo de vídeo; nenhuma chamada externa paga em
caso de prova inválida; voz e legenda derivadas do texto aprovado; composição com FFmpeg
real, inspeção por ffprobe e reprodução desktop/mobile; linhagem e custos no job filho;
nenhuma alegação de prova humana ou comercial por usar captura de AGENT_VALIDATION.

A revisão do filtro HTTP confirmou que a rota administrativa de prova exige `X-Tenant-ID`.
A primeira fixture HTTP da composição não exigia esse cabeçalho e teria permitido um falso
positivo. O contrato agora propaga o tenant e a fixture recusa sua ausência; testes recusam
tenant divergente. A rodada `final1` fica diagnóstica, pois as duas rodadas finais devem
seguir esta correção de integração. Nenhum container publicado ou provider pago foi usado.

A suíte integral também recusou dependência direta de `salesvideo.service` em entidades e
serviços de agentes. A regra foi preservada: `VideoProductProofSource` é a porta neutra e
`AgentTaskVideoProductProofSource` traduz a evidência dentro do módulo proprietário.
Não foi adicionada exceção ao ArchUnit para fazer a integração passar.

## Homologação local concluída

As rodadas consecutivas `final2` e `final3` passaram na matriz completa, com os
mesmos arquivos de implementação e testes (hashes conferidos entre as rodadas).
Por rodada: **2.922 testes executados no backend**, **170 no executor de vídeo**,
**38 no financeiro** e **128 no frontend**: **3.258 testes executados sem falhas**.
O backend registrou ainda oito testes opcionais desabilitados pelas condições
próprias da suíte; eles não estão incluídos no total executado.

Também passaram os contratos de CI e pacote, correspondência de classes/recursos
do JAR, typecheck, build, sete testes do aplicador, REST financeiro/preflight com
MySQL 5.7 real, planejamento de Apolo com o contrato exportado pelo backend,
download privado com tenant obrigatório, FFmpeg/ffprobe, voz HTTP simulada,
legendas e reprodução desktop/iPhone 15 Pro/Pixel 7. Nenhuma API paga ou métrica
comercial foi usada pelas fixtures. A composição usa um tom sintético: naturalidade
da voz real e mérito comercial dependem da inspeção do candidato real.

Evidências: `artifacts/video-production-continuity/final2/` e `final3/`, com
`counts.json`, relatórios Surefire, `media/worker-result.json`,
`media/player-results.json`, `browser/results.json` e screenshots por dispositivo.
O diff foi revisado, incluindo comentários de responsabilidade dos métodos Java,
isolamento entre módulos, identidade da prova, aprovação humana e ausência de
novos changelogs. Os Compose temporários foram removidos com volumes e órfãos.

Antes da aplicação, a consulta MCP continuava mostrando os ciclos 15/16 sem tarefa,
job ou custo. Os dois projetos pertenciam a produto 4/experimento 92/v12; nenhuma
produção desta recuperação havia sido solicitada. As pré-checagens somente de
leitura confirmaram que os três serviços publicados usam os Compose versionados
esperados e que a configuração operacional pode ser preservada na aplicação.
