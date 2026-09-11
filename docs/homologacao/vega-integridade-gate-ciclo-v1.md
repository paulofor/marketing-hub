# Vega: integridade e gate do segundo ciclo

Data: 2026-09-11. Produto 4, cadeia 14, processo 70/v8, ciclo 2, experimento 92.

**Resultado publicado:** atividades 3.6–3.11 concluídas na versão privada v12. Tarefas #394–#399 concluídas; gate #258 aprovado. Próxima atividade 4.1 de Íris disponível pela tela, preservando ciclo #2 e aprendizado do #91. Experimento #92 continua PLANNED. Nenhum PR criado; publicadores protegidos até integração em main.

## Diagnóstico e alternativas

O MCP confirmou #388 (técnica) e #389–#391 (três cenários) aprovadas na v11.
O request persistido da #392 selecionou o prompt humano de Têmis e acrescentou
`versionedArtifactEvidence` do catálogo global, incluindo Kit WhatsApp. A #393
herdou esse parecer. O código de Têmis reconhecia apenas referências `product:*`,
enquanto este ciclo usa `experiment:92`. O gate backend tinha a mesma restrição,
confirmada na resposta do endpoint de atividades. São falhas de integração do
contrato do ciclo; não comprovam defeito no resultado útil nem falta de demanda.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Ajustar somente o prompt de Têmis | Pequeno esforço inicial | Mantém seleção, evidências e gate incompatíveis | Descartada |
| Migrar o ciclo para a referência de produto | Reutiliza o caminho existente | Separa tarefas do histórico do experimento e mistura ciclos | Descartada |
| Integrar a referência do ciclo em Têmis e no gate, preservando auditoria | Resolve a causa em todas as fronteiras | Esforço moderado com testes de identidade e regressão | Escolhida |

O parecer antigo será preservado. A correção terá aceitação versionada e nova
sequência independente de Dédalo, técnica, Psique e Têmis antes do gate. Nenhuma
aprovação, preferência humana ou venda será fabricada. O aprendizado do #91 e
o experimento #92 PLANNED permanecem distintos da homologação sintética.

## Matriz definida antes dos testes

| Grupo | Critérios de aprovação local |
| --- | --- |
| Têmis | Referências de produto e ciclo; identidade divergente bloqueada; prompt e schema sintéticos; nenhum catálogo global; callback auditável |
| Backend | Mesma referência/versão/URL; ciclo aberto e produto correto; todos os checks obrigatórios; tentativas recentes e ordem preservadas |
| Continuidade | Correção → técnica → três cenários → Têmis → gate; histórico preservado; gate idempotente; fonte do ciclo não muda após aprovação |
| Aprendizados | Estratégia e versões anteriores identificadas como histórico; aceitação atual explícita; ausência de provas de outro produto |
| Experiência | Geração, uso, retomada, entrada inválida, falha de integração e bloqueio seguro no protótipo real local |
| Integrações | Backend e MySQL 5.7 locais; executores reais; modelos/serviços externos simulados; request, response e falhas verificáveis |
| Interface | Atividades, tarefa própria e próxima ação em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Métricas | Dados locais sintéticos; AGENT_VALIDATION separado de HUMAN; sem cobrança, campanha, publicação comercial ou gasto de mídia |
| Imagens | Dockerfiles versionados; imagens finais testadas; identidade de versão; segredos preservados; diff revisado |
| Operação | Coordenação ACTIVE antes de intervenção; tarefas criadas pela UI; resultados confirmados no MCP e na tela; publicadores protegidos até integração em main |

Após a última correção serão exigidas duas rodadas locais completas consecutivas
sem falhas. Limitações e resultados efetivos serão registrados ao concluir.

## Defeitos encontrados localmente

- O teste de repetição do gate revelou comparação de números JSON dependente do tipo
  em memória. A identidade da aprovação passou a usar IDs e hashes das provas.
- A primeira rodada integrada passou backend, API e cinco cenários técnicos, mas o
  consumidor de Psique recusou a v12: mantinha uma lista fechada v9/v10/v11. Foi corrigido
  para a família executável versionada, com regressões para versões posteriores e
  rejeição da especificação v8, formato inválido e família diferente.

- O teste antigo da tela ainda tentava criar a correção no card dependente. A matriz
  foi atualizada para o contrato atual: link até 3.6 e acompanhamento exclusivo de sua tarefa,
  incluindo erros HTTP, fila, execução, releitura, bloqueio e conclusão nos três dispositivos.
- A revisão do gate encontrou texto e auditoria fixos em STOP. No ciclo, a mensagem e a
  evidência agora registram a operação realmente preservada (PLAY ou STOP).

Os defeitos acima foram corrigidos na sandbox antes da publicação. A conferência
operacional posterior revelou uma limitação adicional com o volume real do histórico,
reproduzida localmente e descrita adiante.

## Homologação local final

As rodadas `approved1` e `approved2` terminaram consecutivamente sem falhas: **18/18
controles em cada uma**, após a última alteração do prompt. Backend, API, técnica,
Psique, Têmis, gate, Dédalo, executor de geração, contrato de publicação, interface,
TypeScript, navegadores, recuperação de acesso, imagens, proxy TLS e diff aprovados.
A formatação dos três módulos Java também passou; os arquivos Java alterados possuem
comentário de responsabilidade de classe e dos métodos alterados.

O backend do protótipo e o MySQL 5.7 foram executados localmente. Os navegadores
consumiram a interface construída. As saídas reais dos executores locais foram
encadeadas até o gate; modelos e callbacks externos usaram test doubles. O teste do
gate usa repositórios BPM simulados; a persistência e a integração publicadas serão
conferidas separadamente no MCP. Nenhum resultado local simula uma venda ou uma
preferência humana. Os dados de teste usam IDs 91004/91092/91002 e marcação interna.

Evidências: `artifacts/vega-multiagent-recovery/{approved1,approved2}/result.txt`,
`flow/*.json`, `admin-browser/`, `harness/`, `image-harness/` e `final-images.json`.
As quatro imagens finais são construídas pelos Dockerfiles versionados de backend,
Têmis, Psique e frontend privado do Vega, com tag `vega393-gate-v12`.

## Intervenção autorizada

Intervenção `3333f1d2d09a4beeb75db63d2b17d4c1`, escopos `app`, `pde` e `dedalo`.
O coordenador confirmou `ACTIVE`, fila vazia e sete publicadores protegidos antes de
qualquer transferência. Os comandos operacionais usam `execute`. A transferência
compara as camadas da imagem do host com o build homologado na sandbox.
A retomada continua condicionada à integração da correção na `main`; não foi criado PR.

A primeira transferência contínua do backend perdeu a conexão SSH e deixou temporariamente
um receptor sem emissor segurando uma importação. A imagem não foi ativada nesse estado.
O envio foi retomado em partes com hash; após a liberação da importação anterior, o arquivo
completo foi carregado e as camadas foram comparadas com o build local. Nenhuma transação
remota foi cancelada. As próximas importações usam arquivo completo no host e heartbeat,
evitando depender de uma entrada de dados contínua pela sessão SSH.

## Execução publicada pela interface

As quatro imagens foram aplicadas pelos scripts versionados sob `execute`, preservando
a configuração e os segredos existentes. Camadas e configuração de execução foram
comparadas com o build local (Docker 27 e 29 usam identificadores de imagem diferentes,
sem alteração das camadas ou da configuração efetiva). Backend, Psique e Têmis saudáveis;
URL privada e contrato publicados confirmaram a v12.

A UI registrou a nova aceitação no ciclo 2, revisão 6, preservando `experiment:92`,
a cadeia 14 e o aprendizado herdado. O MCP confirmou `product_version` v12.

- **#394 — Dédalo / 3.6:** COMPLETED, decisão READY, nenhuma mudança pendente e retorno à técnica.
- **#395 — Psique / 3.5:** COMPLETED, decisão APPROVED, cinco cenários PASS na v12,
  três dispositivos e evidências visuais persistidas 118–122. Geração no worker publicado;
  métricas AGENT_VALIDATION separadas de pessoas e vendas.

- **#396 — Psique / 3.7:** COMPLETED, decisão APPROVED, cenário ADHERENT na v12.
  O MCP confirmou o resultado antes da correção adicional da leitura da tela.

As demais avaliações e o gate continuam sendo executados e serão registrados abaixo.


### Ajuste adicional identificado no acompanhamento publicado

A #396 foi aprovada em 3.7, mas a consulta do histórico não completava no limite da UI.
O MCP confirmou a conclusão e os logs mostraram respostas encerradas. A leitura integral
medida transferiu 9.274.127 bytes em 100,14 s; omitir apenas os prompts na lista reduz o
mesmo documento a 453.707 bytes (95,11%), preservando seus demais campos.

Foram comparadas três alternativas: ampliar o timeout (baixo esforço, mantém espera e
crescimento), comprimir a resposta (redução medida de 75,86%, ainda retransmite tudo) e
carregar prompts individualmente (esforço moderado, elimina a retransmissão no acompanhamento).
A terceira foi escolhida. O contrato antigo permanece compatível e a auditoria individual
não trunca texto. Novos testes cobrem payload extenso, identidade, erro/retry e navegadores.
As rodadas finais completas serão repetidas após essa correção antes de sua publicação.


As rodadas `approved3` e `approved4` repetiram a matriz inteira após a correção da leitura:
**18/18 grupos por rodada**, consecutivos, sem falhas. A leitura de prompts sob demanda
passou em desktop, iPhone 15 Pro e Pixel 7, inclusive erro de consulta e retry. A API
preservou integralmente os acentos e o conteúdo extenso; produto, processo, tarefa ou
referência divergente receberam 404. O contrato anterior continua disponível.

As imagens adicionais `marketing-hub/backend:vega393-gate-v12-ui` e
`marketing-hub/frontend:vega393-gate-v12-ui` foram construídas por Dockerfiles do repositório
e testadas localmente. A versão do protótipo privado permanece v12; as provas já aprovadas
#394–#396 não foram invalidadas por uma mudança restrita à consulta administrativa.


### Interpolação do Compose administrativo

O backend atualizado iniciou saudável. A aplicação da interface foi interrompida antes de
recriar seu container: o Compose compartilhado exige `LEAD_PORTAL_PAYMENTS_AUTH_TOKEN`
da configuração do backend, embora somente o serviço frontend tenha sido selecionado.
O procedimento versionado passou a obter esse contexto do backend do mesmo projeto,
exclusivamente em memória para interpolar o Compose. O payload de publicação contém
somente o frontend e não recebe essa credencial. Teste local reproduz a exigência e
confere preservação da rede, isolamento do serviço e ausência do segredo no payload.
A pré-checagem operacional é somente leitura; a aplicação será retomada após validação.


As rodadas `approved5` e `approved6` passaram com **18/18 grupos**, após o ajuste
operacional. A pré-checagem no host confirmou um único serviço e preservação da
configuração; nenhuma credencial foi impressa ou gravada.

### Causa adicional: leitura repetida dos prompts no banco

A resposta compacta ainda levou 39,17 s (38,59 s até o primeiro byte). A leitura por
loopback no próprio host levou 42,72 s, confirmando processamento no servidor. Três
amostras de stack, obtidas sem interromper a JVM, localizaram a consulta repetida em
`PdeAgentValidationReworkReadinessProvider.processHistory`: cada card carregava as
tarefas completas, incluindo prompts que não participam da decisão de prontidão.

Também havia consultas concorrentes de recuperação automática carregando todos os
bloqueios. O MCP contou 104 tarefas WORK/BLOCKED, 32.755.260 bytes nos dois campos de
prompt e **zero** erros candidatos à retomada. O filtro só ocorria depois da leitura.

Alternativas comparadas: ampliar timeout (baixo esforço, mantém crescimento), cachear
histórico (esforço moderado, risco de mostrar estado antigo) ou selecionar campos e
candidatos no SQL (esforço moderado, preserva leitura atual). Escolhida a terceira:
projeção escalar de prontidão sem prompts, segregada por origem e processo; fila de
recuperação filtra erros candidatos no banco e mantém lock, contrato, comparação exata
e limite de tentativa no service. Não há alteração da decisão funcional.

Testes de persistência cobrem auditoria acima de 4 MB, versões anteriores, segregação
de origem/processo, ausência de hidratação de entidades na prontidão e uma consulta SQL.
A recuperação cobre seleção por agente/estado/tipo, ordem, bloqueio funcional e proteção
contra nova tentativa. A imagem administrativa de backend recebe tag
`vega393-gate-v12-ui2`; o protótipo e suas provas continuam na v12.


A rodada `approved7` parou no teste HTTP de #377 porque seu repositório simulado ainda
não fornecia a nova projeção; a fixture foi atualizada mantendo todas as asserções de
bloqueio, erro e continuidade. O teste de ciclo de vida com persistência real H2 também
foi incluído na matriz para proteger criação, reserva, callback, falha e nova tentativa.
Nenhuma publicação ocorreu após essa rodada interrompida.


As rodadas **`approved8` e `approved9` passaram consecutivamente, 18/18 controles em
cada uma**, após a última correção. Incluem consultas JPA reais e ciclo de vida HTTP
com persistência H2, além da matriz de backend, protótipo/MySQL 5.7, três executores,
gate, imagens e Chromium desktop/iPhone/Pixel. A revisão do diff passou antes da
publicação adicional. A auditoria de responsabilidade encontrou 28 arquivos Java
alterados ou novos, todos com comentário de classe.


A imagem `backend:vega393-gate-v12-ui2` foi carregada e comparada: camadas e
configuração efetiva idênticas ao build local; ID do host
`sha256:4546eb434a96b5564f1ae12fa97398301c679ae31777614c8e86f61c6b817e69`.
Backend e frontend administrativo foram aplicados pelo coordenador. A primeira consulta
imediatamente após iniciar nginx recebeu conexão recusada; após sua inicialização,
ambos os healthchecks retornaram sucesso, sem nova troca de imagem. A leitura compacta
passou de 44,79 s para **14,28 s** na primeira conferência da nova aplicação, 505.316
bytes e #396 COMPLETED preservada. O frontend publicado consulta a projeção compacta.


- **#397 — Psique / 3.8:** criada e acompanhada pela UI, COMPLETED e APPROVED,
  nove checks verdadeiros. O cenário RECOVERY demonstrou restauração do mesmo cartão
  e progresso após falha controlada, sem alegar validação humana ou comercial.
- **#398 — Psique / 3.9:** criada e concluída pela UI após a #397, COMPLETED e
  APPROVED, nove checks verdadeiros. O pedido fora do escopo foi bloqueado sem gerar
  cartão; contexto e orientação de reformulação permaneceram disponíveis.
- **#399 — Têmis / 3.10:** criada pela UI após a #398, COMPLETED e APPROVED.
  Confirmou técnica e três cenários na mesma URL/versão/identidade, com política
  AGENT_VALIDATION e sem artefatos ativos de outro produto ou alegações humanas/comerciais.


### Continuidade após 3.11: contrato de Íris

As atividades 3.6–3.11 concluíram, mas a navegação efetiva a 4.1 bloqueou antes do botão.
O MCP confirmou Atena #359 com MARKET_STRATEGY_V3, Plutus #361 com economia privada
aprovada e Dédalo #362; Íris procurava MARKET_STRATEGY_V2 via plano comercial, que não
representa esse ciclo. Três caminhos foram comparados: usar plano/estratégia de outro
escopo (risco de mistura), repetir Atena/Plutus (custo sem causa nova) ou publicar a
projeção dos contratos privados já aprovados (esforço moderado e linhagem preservada).
Escolhida a projeção, condicionada ao gate e aos hashes/IDs/estados ainda atuais.

A matriz foi ampliada antes do teste: ciclo sem gate, versão ou produto divergente,
prova alterada, tentativa posterior bloqueada, estratégia ausente, contrato nativo V3,
contexto idêntico para tela e worker, alvo privado correto e manutenção do regime
legado por plano. A disponibilidade não cria tarefa nem modifica os contratos originais.
O teste publicado concorrente de dois navegadores também encontrou timeout na última
leitura; a checagem de predecessoras relia tarefas completas apesar de já haver instâncias
BPM autoritativas. Esse caminho passa a consultar tarefas somente no fallback histórico.


As rodadas **`approved10` e `approved11` passaram consecutivamente, 19/19 controles em cada uma**. A matriz inclui o executor de Íris recebendo o contexto produzido pelo backend local, além dos contratos privados, regressões do plano comercial e rejeição de provas substituídas. Os 54 arquivos de implementação, configuração e testes do manifesto permaneceram idênticos entre as rodadas. A revisão encontrou 36 arquivos Java alterados/novos com comentários de responsabilidade; métodos alterados/novos foram revisados. O Swagger dos endpoints de histórico e auditoria foi atualizado e validado com SnakeYAML. Backend final construído pelo Dockerfile versionado: `marketing-hub/backend:vega393-gate-v12-ui3`.


## Conferência publicada final e encerramento

A imagem `backend:vega393-gate-v12-ui3` foi comparada com o build local: camadas e
configuração de execução idênticas. ID no host:
`sha256:b67282cbee9dcb2a406f357ad1ed3bceb8c2ce9f756cec8c5126c56e8ff8264f`.
Aplicação feita pelo coordenador, somente no backend. Health administrativo, diagnóstico
privado e contrato público da URL privada responderam corretamente na v12.

| Atividade | Evidência vigente no ciclo 2 |
| --- | --- |
| 3.6 — Dédalo | #394 COMPLETED / READY |
| 3.5 — Nova homologação técnica | #395 COMPLETED / APPROVED, cinco cenários |
| 3.7 — Psique aderente | #396 COMPLETED / APPROVED |
| 3.8 — Psique recuperação | #397 COMPLETED / APPROVED |
| 3.9 — Psique segurança | #398 COMPLETED / APPROVED |
| 3.10 — Têmis | #399 COMPLETED / APPROVED |
| 3.11 — Gate backend | Instância #258 COMPLETED, objectiveAchieved=true, hashes das cinco provas atuais |

O gate não cria tarefa de agente. As tentativas rejeitadas, inclusive #392/#393, continuam
no histórico. Cada tarefa fica vinculada à sua atividade. O cadastro comercial existente
permaneceu ATIVO/PLAY e o experimento #92 PLANNED; não houve campanha, cobrança de cliente
ou transformação das avaliações sintéticas em evidência de vendas.

As seis atividades solicitadas foram conferidas no Chromium desktop, iPhone 15 Pro e
Pixel 7 emulados. Nos três, o endpoint e a tela confirmaram conclusão, ciclo 2 e aprendizado
do #91. O link **Abrir próxima atividade** foi efetivamente seguido: Processo 4, atividade
**4.1 — Materializar contrato de comunicação**, Íris, com botão **Executar atividade**
disponível e referência `experiment:92`. Nenhuma tarefa de Íris foi criada nesta conferência.

A consulta compacta após a atualização levou 10,25 s com 657.381 bytes e todas as novas
provas. Duas consultas simultâneas completaram em 15,17 s e 13,91 s, ambas com as seis
atividades concluídas, dentro do limite de 45 s usado na tela. Não houve novo timeout.

Evidências finais em `artifacts/vega-multiagent-recovery/`: `approved10/`, `approved11/`,
`source-manifest-ui3.json`, `image-backend.json`, `handoff-apply.log`, `final-ui.log`,
`next-ui.log`, `final-ui/next-activity-ready.png`, `final-ui/*-history.json`,
`final-instances-after-handoff.json`, `final-product-state-after-handoff.json`,
`concurrent-history-after.json` e `final-private-health.log`.

A topologia Compose exclusiva foi encerrada com `down --volumes --remove-orphans`.
Um volume antigo `harness-evidence`, sem container e com o rótulo exclusivo deste projeto,
foi removido separadamente. Os cinco processos locais de backend, modelo simulado,
frontend, TLS e geração foram encerrados. Nenhum recurso produtivo foi removido.

A intervenção permanece `ACTIVE`, com os publicadores protegidos. A retomada depende
exclusivamente da integração destas correções na `main`, via PR solicitado pelo usuário,
e da reconciliação pelo coordenador conforme o cânone de deploy. A homologação funcional
solicitada está concluída; a proteção impede que uma publicação antiga restaure o defeito.
