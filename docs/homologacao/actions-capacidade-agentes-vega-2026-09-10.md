# Actions e continuidade do Vega — 10/09/2026

## Evidência e decisão antes da homologação

Os runs `34418552643` (Atena), `34419726095` (Psique) e `34419726185`
(Íris/Têmis) passaram pelos testes e falharam no gate de capacidade, antes do
restart. Respectivamente, os logs mostram 8.434/8.576 MiB, 8.432/10.211 MiB
e 11.051/11.409 MiB disponíveis/exigidos. A coleta já preservava apenas um
rollback por repositório; repetir indiscriminadamente o deploy não resolve.

SSH autenticado em `root@163.245.202.80` foi confirmado. Dez containers ativos,
38,77 GB de imagens e nenhum build cache. O store containerd mantém blobs e
snapshots: 17 GB e 20 GB respectivamente. Atena usa runtime próprio e Íris usa
Playwright 1.49.0, enquanto Argos e Psique usam 1.54.2; os runtimes de Atena e
Íris ainda empacotam cerca de 154 MiB de cache npm cada.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Ampliar disco | Mais capacidade imediata | Custo externo, mantém duplicação | Não escolhida |
| Reduzir reserva ou apagar rollback | Liberação imediata | Perde margem operacional e recuperação | Rejeitada |
| Compartilhar base versionada e eliminar cache na camada de instalação | Reduz duplicação mantendo ferramentas e rollback | Validar navegador, imagens e consumo real | Escolhida |

Não alterar a reserva de 4 GiB nem a estimativa de duas vezes o tamanho antes
da carga. Conservar artefato, checksum, conteúdo portátil, mesmo SHA, fila e
Compose sem rebuild. A continuidade produtiva da Atena tem autorização
excepcional já registrada no cânone de deploy; outros módulos seguem o PR.

Referências: [Docker/containerd e armazenamento](https://docs.docker.com/engine/storage/containerd/),
[estágios compartilhados](https://docs.docker.com/build/building/best-practices/),
`docs/registros/loops.md` (loop de capacidade do VPS).

MCP confirma backend `dc004fe1b993529cfc5e9675b0a9776e9e2cd597` e tarefa #358
`IN_PROGRESS`, sem resultado ou prompt. Atena ainda executa imagem `dde8317…`.
UI do processo 2 confirma atividade 2.1 em execução. Não fabricar conclusão.

## Matriz de aceite definida antes dos testes

| Dimensão | Critério |
| --- | --- |
| Contratos | Pacote, lock, base por digest e CI alinhados para quatro agentes; impedir regressão de browser, caches e bases divergentes |
| Capacidades | Comparar tamanho e camadas reais; manter bloqueio por disco, inodes e reserva; preservar ativo e rollback |
| Atena | Suítes Java/MCP; imagem com estado persistente, resposta 503 e recriação sem repetir inferência |
| Íris/Têmis | Suítes Java/MCP; navegador, landing, checkout somente leitura e decodificador reais na imagem confinada |
| Navegação | Chromium desktop e emulações iPhone/Pixel; retomada correlacionada ao ciclo #2 e experimento #92 |
| Integração | Exportação, checksum, carga real, prova portátil, Compose sem rebuild; transporte SSH e coordenação por revisão |
| Observabilidade | Logs de capacidade e motivo de falha; prompt, resultado, tarefa e próximo comando auditáveis |
| Isolamento | Modelos simulados e páginas locais; sem vendas, e-mails ou métricas de produção nos testes |
| Operação | Só após aceite local, recuperar Atena pela autorização vigente, executar pela UI e conferir banco e próxima atividade |

Como a investigação partiu de defeitos confirmados, exigir duas rodadas locais
completas e consecutivas após a última correção. Registrar resultados e limites
ao concluir, sem usar publicação para descobrir defeitos.

## Resultado local

As rodadas `final-a` e `final-b` passaram integralmente, com **32/32 controles em cada**.
Cada rodada incluiu 33 testes Java de Atena, 89 do revisor/estúdio, 45 contratos de
navegador, 62 contratos de imagens/workflows/coordenação e as provas reais descritas
na matriz. Modelos e fontes comerciais foram simulados; não foram geradas vendas de teste.

Comando reproduzível, com o frontend local em `127.0.0.1:4173`:

```sh
AIHUB_HOMOLOGATION_SESSION=vega-actions-20260910 \
ACTIONS_TEST_COMPOSE_PROJECT=aihub-bbea24fc-0f9d-4c14-a56e-4d2822ad04e9-c8fbe43c82 \
bash scripts/run-docker-homologation.sh bash scripts/test-agent-capacity-local.sh final-a
```

Requer Java 21/Maven, npm, Docker/Compose e ShellCheck. O script registra cada
controle em `artifacts/actions-capacity-2026-09-10/<rodada>/results.tsv` e preserva
seus logs nessa pasta ignorada pelo Git. Containers, volumes e referências temporárias
são removidos ao fim de cada rodada. Logs desta sessão:
`/tmp/vega-capacity-final-a.log` e `/tmp/vega-capacity-final-b.log`.

A revisão local identificou e corrigiu expectativas antigas dos Dockerfiles nos testes,
a interpretação de múltiplos estágios no contrato de digest/cache e a interceptação
excessiva da fixture visual: `**/api/**` também capturava módulos `/src/api` no Vite.
O teste agora intercepta somente endpoints da origem local. ShellCheck foi instalado
na sandbox e a declaração de variáveis da matriz foi corrigida antes das duas rodadas finais.

As imagens locais compartilham quatro camadas de base e a camada do cliente. Atena mede
3.219.623.760 bytes e o revisor 3.492.648.646 bytes; a base compartilhada aumenta o
tamanho nominal de Atena frente ao runtime anterior, mas elimina sua base particular
no conjunto das próximas versões. Não declarar uma redução nominal de todas as imagens.
O pico do revisor passa a 10.758 MiB, com o estúdio carregado e validado depois;
o pacote anterior exigia 11.409 MiB para carregar os dois juntos. O ganho efetivo no
host depende do rollout e da retenção, não apenas da soma dos tamanhos de `docker images`.

## Recuperação operacional

Depois do aceite local e da revisão do diff, a leitura SSH confirmou 11.049 MiB
disponíveis, acima dos 8.576 MiB exigidos pela imagem já aprovada da Atena. O backend
já executava `dc004fe1…`, com o contrato resiliente de callback. O artefato do mesmo
SHA estava íntegro/disponível no run, não expirado, e o job de testes estava verde.

Foi escolhido retomar somente o job de publicação desse run pelo conector GitHub,
usando a imagem construída pelo pipeline do PR já aprovado. Não usar as mudanças
locais dos Dockerfiles como se já estivessem publicadas. A confirmação produtiva
da atividade e o desfecho dos demais runs serão registrados a seguir.

O run de Atena `34418552643` terminou **SUCCESS**. SSH confirmou imagem `dc004fe1…`
e health aprovado, com a correção já publicada de callback e contexto do ciclo. A #358
foi cancelada pela mesa da Atena (`PATCH` oficial, HTTP 200), preservando a tentativa
órfã. Não recebeu conclusão nem parecer fabricados.

A retomada revelou dois defeitos adicionais antes de criar outra tarefa: a atividade
cancelada não oferecia o comando de reinício; a mesa alternativa não listava os
experimentos por `LazyInitializationException` na campanha do #71. Banco e stack trace
confirmaram a campanha; a falha de analytics do #69 na linha anterior era capturada e
não explicava o HTTP 500. O teste local reproduziu também a associação lazy de jornada.

| Decisão | Alternativas comparadas | Escolha e justificativa |
| --- | --- | --- |
| Retomar cancelamento | Alterar status no banco; cadastro alternativo na mesa; unificar elegibilidade no BPM | Regra única no backend, com nova ocorrência, auditoria e gates; evita atalhos ou dependência de outra tela |
| Ler experimento | Reabrir sessão em toda requisição; criar novo leitor DTO transacional; carregar explicitamente o contrato administrativo | Grafo de consulta compartilhado, sem ampliar transações nem alterar relações globalmente; mesmo contrato para lista, página, detalhe e nicho |

A matriz foi ampliada **antes da nova rodada** com backend completo, mapper sobre
entidades destacadas em quatro consultas, nova ocorrência depois de cancelamento,
suíte frontend completa, recursos empacotados e imagem do backend. A fixture visual
agora exige retomada após `BLOCKED` **e** `CANCELLED`, recusa duplicação ativa e verifica
o próximo comando de Plutus no ciclo #2. São 36 controles por rodada; as duas rodadas
anteriores de 32 controles não substituem as duas exigidas após estas correções.

A coleta versionada no host recuperou aproximadamente 2,8 GiB eliminando apenas um
rollback excedente da Atena. Ficaram 9.380 MiB livres, insuficientes para Psique
(10.211 MiB) e para o pacote publicado de Íris/estúdio (11.409 MiB). Nenhum desses runs
foi repetido para descobrir um bloqueio já comprovado. A ampliação de pelo menos 3 GiB
foi consultada ao usuário, preservando a reserva de 4 GiB e um rollback por agente.
Uma nova leitura SSH encontrou 4.447 MiB em `/opt`, sobretudo sessões e registros
dos agentes (2.949 MiB em seus homes), e 767 MiB em logs do sistema. Isso não
oferece uma coleta suficiente que preserve auditoria e recuperação. Não apagar
sessões nem bancos locais dos agentes para liberar publicação.

As rodadas ampliadas `recovery-a` e `recovery-b` passaram **36/36 controles cada**:
2.577 testes backend por rodada (cinco ignorados preexistentes), 535 frontend e
as suítes de agentes já descritas. Somente então o backend foi recuperado pela
exceção autorizada, usando seu Dockerfile e Compose versionados, mesmos mounts e
configuração, imagem anterior preservada e duas verificações consecutivas de saúde.
Imagem `marketinghub-backend:vega358-7efc078aeecb`; SHA-256 do JAR publicado
`012e16cdef3d218f739e37997db3f8779cfeae2bde8aad991bc484f80bdf5ef9`.

Pela atividade 2.1, o comando oficial criou #359/ocorrência 225 no experimento #92.
Atena concluiu `APPROVE` às 01:54:33 UTC: “Cartão de microação pronto com salvar e
retomar”, estratégia v3 para validação privada. Foram auditados 27.901 tokens de
entrada, 4.367 de saída e custo estimado de US$ 0,198944. GET `/api/experiments`
voltou a HTTP 200. A próxima atividade 2.2 foi executada pela tela e criou #360.

## Continuidade Plutus: contrato do sucessor

A #360 recebeu Atena v3 e processo v6, mas usou o prompt v4 por exigir origem
`product-discovery-cycle:*` para selecionar v5. Prazo `2026-09-17T02:59:00Z` e
contribuição 26,95 para preço 67/custo 15 violavam o contrato. O sucesso antigo
da #238 era de uma versão histórica; não justifica aceitar essa resposta atual.
O MCP também confirmou #328 e #329 concluídas com prompt v5 e
`PDE_PRIVATE_ECONOMICS_V1` na origem `product-discovery-cycle:64`: o contrato
atual já funcionava, e a divergência se restringia à seleção por origem.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Normalizar somente a data | Remove o primeiro erro | Mantém cálculo e orçamento no contrato errado | Rejeitada |
| Exceção para o Vega/#92 | Recuperação pontual | Repete o defeito no próximo sucessor | Rejeitada |
| Selecionar contrato pela versão do BPM | Regra uniforme para descoberta e sucessor | Testar compatibilidade histórica e gates privados | Escolhida |

Antes das rodadas finais, ampliar a matriz para **40 controles** com a suíte
completa de Plutus e dez cenários executados também na imagem final: sucessor,
descoberta, versão seguinte, versão histórica, predecessor obsoleto/ausente,
prazo com horário, contribuição divergente, orçamento indevido e STOP. Todos
percorrem controle operacional, `pending`, processo local do modelo, prompt/schema,
tokens e callback com tarefa e produto fictícios (`900xxx`) isolados. Não afrouxar
o parser nem alterar o parecer #360. Após a validação, a recuperação causal de
Plutus usa a mesma autorização excepcional do fluxo do Vega, sem estendê-la à
publicação geral das otimizações de outros agentes.

A primeira tentativa `plutus-a` passou 39 controles, mas o Compose de teste
interpretou as vírgulas de `tmpfs` como entradas separadas de uma lista YAML.
O container foi recusado antes de executar o agente. O valor foi colocado entre
aspas; essa rodada não conta como aprovada. As rodadas completas reiniciam em
`complete-a`/`complete-b`, após a última correção.

A sandbox aproximou-se do limite de disco durante o build. A coleta de cache
BuildKit sem uso recuperou 1,851 GB, preservando 1 GB de cache; a coleta ocorre
também entre as rodadas. Duas referências antigas de testes locais de Plutus
(`#322`, criadas em 02/09, sem containers) foram removidas, sem acesso à engine
produtiva. Containers e volumes temporários continuam restritos ao projeto
exclusivo e são removidos pelo Compose. Nenhuma redução de reserva produtiva.

## Aceite completo após a última correção

As rodadas **`complete-a` e `complete-b` aprovaram 40/40 controles cada**, consecutivas
e sem falhas. Cada uma incluiu 2.577 testes backend (cinco ignorados preexistentes),
535 frontend, 33 de Atena, 89 de Íris/Têmis e 38 de Plutus, além dos contratos,
transportes, imagens e jornadas da matriz. A imagem exata de recuperação de Plutus
também executou os dez cenários e `codex --version` (`0.146.0`) antes do envio.

- Referência: `marketing-hub/financial-agent-worker:vega360-f72354ec3c61`.
- Fingerprint dos arquivos de produção do módulo:
  `f72354ec3c61002ba0b1159c9f5ab40ef5d4017b23867fba48f892d7d10304d8`.
- Identidade local: `sha256:2631b71e35817d671480ad8dd934dc286c99ff9e795a5e4507c5d7a05fdcd408`.
- Prova SHA-256 das camadas:
  `460f3e97a90090b883d9d5e57175942bf8cf6bc24053c69846e09478505b560c`.
- Tamanho: 1.196.632.760 bytes; preflight de carga: 6.379 MiB. O host comprovou
  9.370 MiB livres antes do envio, mantendo a reserva e sem precisar de nova limpeza.

As dependências geradas por `npm ci` foram removidas do diff, preservando somente
package/lock e código/configuração/testes/documentação intencionais. Nenhum commit,
push ou PR foi realizado. As otimizações de Atena/Íris continuam locais; o runtime
de Atena usa o artefato já publicado de `dc004fe1…`.

## Resultado operacional confirmado

A carga preservou exatamente as camadas testadas. O Compose versionado foi aplicado
somente a Plutus, mantendo ambiente (exceto referência auditável do build), usuário,
montagens, sessão individual, porta, readonly, tmpfs e política de reinício. A imagem
anterior foi preservada como `marketing-hub/financial-agent-worker:vega360-rollback`.
O runtime `sha256:4bcdcf3f528f99e56cfdb0aa1f35ba80573c0413fba687f749da3c229b3da537`
passou duas verificações consecutivas de saúde. O manifesto/relatório remoto fica em
`/opt/marketing-hub/recoveries/vega-360-20260910/`.

Pelo botão **Reiniciar tarefa** da atividade 2.2, o POST oficial retornou HTTP 200 e
criou **#361**, origem `experiment:92`. Plutus recebeu às 02:51:58 UTC e concluiu
às 02:53:16, com `APPROVE`, prompt v5 e `PDE_PRIVATE_ECONOMICS_V1`. O prazo é
`2026-09-17`; preço 67, custo variável 12 e contribuição 55 são **hipóteses do
checkout simulado**, não resultados de vendas. Não há autorização de gasto. A
auditoria registra 30.286 tokens de entrada, 2.214 de saída e US$ 0,165424 estimados.

A #361 é uma nova tarefa de retentativa na instância BPM 226, agora `COMPLETED`
com objetivo atingido; a #360 permanece `BLOCKED` no histórico. Atena usa a nova
instância 225, também concluída, enquanto a instância cancelada 224 permanece
preservada. Não confundir nova tarefa de retentativa com nova instância em todos
os tipos de reinício.

Chromium desktop, iPhone 15 Pro e Pixel 7 confirmaram **2.1 e 2.2 concluídas**,
sem erros JavaScript, e **2.3 — Dédalo — Projetar protótipo e harness PDE** disponível
com “Executar atividade”, ainda não iniciada. Banco confirmou #91 `USER_STOPPED`
e #92 `PLANNED`, ambos do produto 4. Nenhuma campanha foi criada ou ativada.

Após a recuperação, o host tem 7.564 MiB livres. Os runs de Psique `34419726095`
e Íris/Têmis `34419726185` continuam bloqueados por capacidade: exigem 10.211 e
11.409 MiB antes da carga. Com a imagem adicional necessária de Plutus, o
dimensionamento recomendado passa a **pelo menos 5 GiB adicionais**, em lugar da
estimativa inicial de 3 GiB. Não houve resposta/consentimento para expansão; esses
runs não foram repetidos nem declarados corrigidos em produção.

Após a consolidação pelo PR solicitado pelo usuário e a comprovação do runtime
oficial, revisar as tags excepcionais de recuperação preservando ativo e rollback;
elas não participam da coleta automática de tags SHA. Isso não é autorização para
remover a versão de recuperação enquanto esta entrega ainda estiver em uso.
