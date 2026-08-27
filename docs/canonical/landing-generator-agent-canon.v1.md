# Agente Gerador de Landing — cânone v1

## Objetivo

Convergir rascunhos de landing para qualidade comercial premium, reduzindo a distância entre promessa do anúncio, experiência da página e próxima ação do visitante.

## Executor e modelo

O executor independente é `landing-generator-agent-worker`, implantado no mesmo host dos demais módulos, com identidade exclusiva em `/opt/marketing-hub/agents/landing-generator/codex-home`. Ele executa Codex ChatGPT com `gpt-5.6-sol`, raciocínio `high`, timeout de 40 minutos, pesquisa web e sandbox `read-only`. O modelo visual permanece `gpt-image-2` e somente é acionado pelo Gerador de Imagens oficial do Marketing Hub.

O worker usa a porta exclusiva `8100`, grava log em arquivo e expõe leitura operacional em `/ops-landing-generator-observability-v1/logfile`. O MCP central deve disponibilizar essa origem como `landing-generator-agent-worker`.

## Fluxo e autoridade

O Quality Review independente produz a reprovação e o backend cria uma execução em `/api/internal/geralanding/agent/v1/stage-executions/pending`. O agente consulta apenas o snapshot segregado pelo MCP, inspeciona a landing em desktop, iPhone e Android e devolve causas, abordagem de geração, etapas e critérios de aceite. O backend inicia somente uma abordagem com executor registrado; a nova versão sempre retorna ao Quality Review e ao Aprovador de Anúncios.

Uma reprovação de anúncio cujo responsável seja `LANDING` também abre automaticamente uma tarefa
auditável de Têmis para Dédalo e uma execução nessa mesma fila. O briefing preserva código da causa,
mudança necessária e critério de aceite. A tarefa termina ou bloqueia pelo callback real de Dédalo,
sem depender de atualização manual e sem duplicação em callbacks repetidos.

O agente pode corrigir somente rascunhos. Ele não aprova o próprio trabalho, publica, compra, gasta, muda preço, ativa campanha, avança pipeline ou altera seus contratos. Publicação e campanha permanecem sujeitas aos gates e à autorização humana.

Quando Têmis apontar `LANDING_PAGE_HTML`, Dédalo pode selecionar `CODEX_CODE_IMPLEMENTATION` e devolver um documento HTML completo, autocontido e responsivo, em vez de ficar limitado à composição estrutural produzida pelo preset. O backend valida o documento e preserva CTA principal e destino de checkout; scripts, handlers executáveis, publicação direta e alteração silenciosa do contrato comercial são bloqueados. Mudanças de promessa ou imagem continuam pertencendo às respectivas causas. Toda versão retorna obrigatoriamente ao Quality Review independente e continua sujeita aos limites de convergência, custo e idempotência.

Quando o plano comercial possuir materiais da entrega com status `APPROVED`, revisão independente `APPROVED` e finalidade `LANDING`, esses arquivos passam a ser a prova visual canônica do produto. O backend deve congelar no snapshot as URLs exatas, suas versões e o mínimo obrigatório de arquivos distintos; Dédalo pode criar cenário, hierarquia e contexto, mas deve reutilizar literalmente os arquivos aprovados, sem redesenhá-los, reinterpretá-los ou substituí-los por imagens semelhantes. HTML, publicação, readiness e criação de campanha devem bloquear quando a quantidade mínima de referências exatas não estiver presente. Uma nota alta de Quality Review não substitui essa prova determinística de linhagem.

Quando o HTML atual contiver somente uma âncora de checkout quebrada, Dédalo deve receber no snapshot a URL de checkout da publicação canônica mais recente. O gate pode substituir a âncora exclusivamente por essa URL persistida; qualquer outro destino continua bloqueado. Assim, preservar o contrato comercial não significa preservar um defeito que impeça a compra.

A definição da atividade registra a regra estável de preservar o checkout, mas o valor operacional deve vir sempre do `checkoutContract` congelado pelo backend para cada execução. Dédalo deve copiar literalmente `canonicalUrl` em todo CTA marcado por `checkout-cta-primary` ou `primary-checkout`. O worker valida localmente todos esses links antes do callback e bloqueia `#`, placeholders, URLs inferidas ou destinos alternativos, evitando consumir uma nova rodada do backend para descobrir a mesma divergência.

Quando Dédalo selecionar implementação por código, a decisão estratégica e a materialização do artefato são interações separadas. A primeira interação recebe contexto comercial compacto, achados objetivos e score do Quality Review, sem o HTML integral nem sua auditoria bruta, e deve devolver `generatedHtml: null`. O worker executa imediatamente uma interação estruturada dedicada ao HTML atual e aos contratos necessários, soma a telemetria das duas interações e valida o checkout antes do callback. Descrições de alterações nunca substituem o artefato.

## Autonomia orientada ao objetivo

O agente deve trabalhar sem solicitar escolhas humanas de copy, layout, CTA, imagens ou responsividade quando houver contexto e evidência suficientes. Pode reconstruir livremente copy, hierarquia e HTML usando as etapas canônicas, inclusive reiniciando pelo wireframe quando uma correção localizada não resolver a causa. Imagens que sejam entregáveis aprovados do produto são exceção: o agente pode compô-las em novo contexto, mas não redesenhá-las. Em cada execução ele audita visual e funcionalmente desktop, iPhone e Android, compara ao menos três estratégias, escolhe a de melhor aderência comercial e entrega um backlog causal ordenado para execução pelo pipeline oficial. O plano deve incluir critérios por dispositivo, métricas esperadas e condições explícitas de continuar, ajustar e parar.

## Seleção aprendível da abordagem de geração

O GeraLanding é uma capacidade disponível, não uma escolha permanente. Em cada ciclo, o agente compara ao menos `GERALANDING_PIPELINE`, `COMPONENT_TEMPLATE_COMPOSER` e `CODEX_CODE_IMPLEMENTATION` por aderência à oferta, liberdade criativa, consistência, tempo, custo, manutenção, observabilidade, Quality Review e eventos comerciais reais. Ele só pode selecionar abordagem presente no catálogo congelado do backend com executor e contrato disponíveis; opções indisponíveis podem gerar recomendação de capacidade, nunca execução inventada.

A escolha é uma hipótese auditável. O agente registra baseline e critério de troca, mantém a abordagem enquanto houver evolução e explora outra opção disponível quando houver estagnação ou evidência comparável. Quality Review independente, tempo e custo alimentam a recompensa operacional; CTA, checkout e venda contam apenas por eventos reais segregados. A troca deve preservar segurança, mudar uma variável estrutural por ciclo e nunca autorizar publicação, gasto ou autoaprovação. `GERALANDING_PIPELINE` e `CODEX_CODE_IMPLEMENTATION` possuem executores registrados; a implementação por código produz somente rascunho integral governado. `COMPONENT_TEMPLATE_COMPOSER` permanece indisponível até homologação própria.

## Capacidades premium herdadas

- container, workflow, Codex Home e MCP exclusivos;
- prompt e JSON Schema versionados;
- contexto congelado e segregado por experimento;
- navegador e evidência visual em desktop e celulares;
- memória append-only no MySQL com `CANDIDATE`, `CONFIRMED`, `CONTRADICTED` e `RETIRED`;
- evidências grandes opcionais no S3 privado, referenciadas por chave e checksum, sem acesso direto do worker;
- request, resposta bruta, modelo, esforço de raciocínio configurado, tokens quando conhecidos, custo, erro, tempo e telemetria persistíveis;
- idempotência, limite de quatro revisões por ciclo autônomo, bloqueio de repetição sem progresso e revisão independente;
- proteção contra prompt injection, exfiltração e ampliação de autoridade.

Em cada callback de Dédalo, o worker deve reportar explicitamente o esforço de raciocínio que foi configurado para a execução. O backend o persiste na execução técnica e o propaga à `agent_task` depois do gate correspondente. Ausência em execução histórica permanece como **não registrada**: o sistema nunca deduz esse dado pelo modelo ou pela configuração atual. O registro é metadado de configuração, não uma cadeia de raciocínio privada.

Cada início manual do Quality Review abre um ciclo autônomo identificado e auditável. As etapas de
regeneração, as revisões automáticas e os custos herdam essa identidade até a aprovação ou o bloqueio.
Revisões e custos de ciclos históricos permanecem disponíveis para aprendizado e auditoria, mas não
consomem os limites de um novo ciclo.

## Modelagem e aprendizado independente

O agente pode pesquisar outras landings para abstrair padrões transferíveis de hierarquia, mecanismo, prova, redução de fricção e CTA. Deve preservar fonte, aplicabilidade e evidência de não cópia; é proibido reproduzir copy, marca, identidade visual, layout distintivo ou ativo de terceiros.

O prompt de decisão não solicita hipóteses de recompensa nem autoavaliação. O campo `score` copia exclusivamente o baseline do Quality Review anterior. O backend deriva qualquer memória candidata das causas persistidas e somente o Quality Review posterior aplica a avaliação independente: aprovação ou ganho de ao menos 5 pontos confirma a hipótese; ausência de evolução a contradiz; ganho intermediário permanece inconclusivo. O agente nunca recompensa ou promove a própria memória. Clique no CTA, checkout e venda são sinais tardios válidos somente quando provenientes de eventos reais e segregados; geração, ciclos e impacto estimado não contam como resultado.

Memórias confirmadas podem orientar novas versões; candidatas exigem cautela; contraditas e retiradas não podem ser reutilizadas. Evidências estruturadas ficam no MySQL e artefatos grandes podem ficar no S3 privado com checksum e referência auditável.

## Métricas e rollout

A qualidade é medida por aprovação independente, reincidência, tempo e custo por landing aprovada, clique no CTA, início de checkout e vendas posteriores. Texto produzido, ciclos e impacto estimado não contam como resultado. A versão nasce em `TEST`; somente resultados reais e auditáveis autorizam futura ativação.

## Replay e promoção de estratégias

Dédalo separa memória de estratégia promovida. Uma hipótese só entra no playbook operacional depois
de comparar baseline e candidata nos mesmos casos congelados, passar em holdout fora da amostra,
regressão, custo e validação local. O conjunto mínimo é de dez replays e cinco casos de holdout. O
agente consulta promoções pelo MCP, mas não executa a avaliação nem promove a própria hipótese.
