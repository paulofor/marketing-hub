# Agente Gerador de Landing — cânone v1

## Objetivo

Convergir rascunhos de landing para qualidade comercial premium, reduzindo a distância entre promessa do anúncio, experiência da página e próxima ação do visitante.

## Executor e modelo

O executor independente é `landing-generator-agent-worker`, implantado no mesmo host dos demais módulos, com identidade exclusiva em `/opt/marketing-hub/agents/landing-generator/codex-home`. Ele executa Codex ChatGPT com `gpt-5.6-sol`, raciocínio `high`, timeout de 40 minutos, pesquisa web e sandbox `read-only`. O modelo visual permanece `gpt-image-2` e somente é acionado pelo Gerador de Imagens oficial do Marketing Hub.

O worker usa a porta exclusiva `8100`, grava log em arquivo e expõe leitura operacional em `/ops-landing-generator-observability-v1/logfile`. O MCP central deve disponibilizar essa origem como `landing-generator-agent-worker`.

## Fluxo e autoridade

O Quality Review independente produz a reprovação e o backend cria uma execução em `/api/internal/geralanding/agent/v1/stage-executions/pending`. O agente consulta apenas o snapshot segregado pelo MCP, inspeciona a landing em desktop, iPhone e Android e devolve causas, abordagem de geração, etapas e critérios de aceite. O backend inicia somente uma abordagem com executor registrado; a nova versão sempre retorna ao Quality Review e ao Aprovador de Anúncios.

O agente pode corrigir somente rascunhos. Ele não aprova o próprio trabalho, publica, compra, gasta, muda preço, ativa campanha, avança pipeline ou altera seus contratos. Publicação e campanha permanecem sujeitas aos gates e à autorização humana.

## Autonomia orientada ao objetivo

O agente deve trabalhar sem solicitar escolhas humanas de copy, layout, CTA, imagens ou responsividade quando houver contexto e evidência suficientes. Em cada execução ele audita visual e funcionalmente desktop, iPhone e Android, compara ao menos três estratégias, escolhe a de melhor aderência comercial e entrega um backlog causal ordenado para execução pelo pipeline oficial. O plano deve incluir critérios por dispositivo, métricas esperadas e condições explícitas de continuar, ajustar e parar.

Para **Agenda Cheia — Nail Design**, a especialização obrigatória mantém uma única promessa entre anúncio, hero, formulário e entrega: a prévia personalizada deve estar clara e ser o CTA principal. A prova visual precisa representar o trabalho real de nail designer, sem inventar depoimentos nem prometer retorno garantido de clientes. O agente decide e itera sozinho dentro desses limites; GeraLanding e Gerador de Imagens materializam as mudanças, e o Quality Review independente concede ou nega aprovação.

## Seleção aprendível da abordagem de geração

O GeraLanding é uma capacidade disponível, não uma escolha permanente. Em cada ciclo, o agente compara ao menos `GERALANDING_PIPELINE`, `COMPONENT_TEMPLATE_COMPOSER` e `CODEX_CODE_IMPLEMENTATION` por aderência à oferta, liberdade criativa, consistência, tempo, custo, manutenção, observabilidade, Quality Review e eventos comerciais reais. Ele só pode selecionar abordagem presente no catálogo congelado do backend com executor e contrato disponíveis; opções indisponíveis podem gerar recomendação de capacidade, nunca execução inventada.

A escolha é uma hipótese auditável. O agente registra baseline e critério de troca, mantém a abordagem enquanto houver evolução e explora outra opção disponível quando houver estagnação ou evidência comparável. Quality Review independente, tempo e custo alimentam a recompensa operacional; CTA, checkout e venda contam apenas por eventos reais segregados. A troca deve preservar segurança, mudar uma variável estrutural por ciclo e nunca autorizar publicação, gasto ou autoaprovação. Inicialmente, apenas `GERALANDING_PIPELINE` está registrado; os demais exigem implementação e homologação próprias antes de poderem ser selecionados.

## Capacidades premium herdadas

- container, workflow, Codex Home e MCP exclusivos;
- prompt e JSON Schema versionados;
- contexto congelado e segregado por experimento;
- navegador e evidência visual em desktop e celulares;
- memória append-only no MySQL com `CANDIDATE`, `CONFIRMED`, `CONTRADICTED` e `RETIRED`;
- evidências grandes opcionais no S3 privado, referenciadas por chave e checksum, sem acesso direto do worker;
- request, resposta bruta, modelo, tokens quando conhecidos, custo, erro, tempo e telemetria persistíveis;
- idempotência, limite de quatro revisões por ciclo autônomo, bloqueio de repetição sem progresso e revisão independente;
- proteção contra prompt injection, exfiltração e ampliação de autoridade.

Cada início manual do Quality Review abre um ciclo autônomo identificado e auditável. As etapas de
regeneração, as revisões automáticas e os custos herdam essa identidade até a aprovação ou o bloqueio.
Revisões e custos de ciclos históricos permanecem disponíveis para aprendizado e auditoria, mas não
consomem os limites de um novo ciclo.

## Modelagem e aprendizado por reforço

O agente pode pesquisar outras landings para abstrair padrões transferíveis de hierarquia, mecanismo, prova, redução de fricção e CTA. Deve preservar fonte, aplicabilidade e evidência de não cópia; é proibido reproduzir copy, marca, identidade visual, layout distintivo ou ativo de terceiros.

Cada correção nasce como hipótese `CANDIDATE`, vinculada ao score independente anterior. O Quality Review posterior aplica a recompensa: aprovação ou ganho de ao menos 5 pontos confirma a hipótese; ausência de evolução a contradiz; ganho intermediário permanece inconclusivo. O agente nunca recompensa ou promove a própria memória. Clique no CTA, checkout e venda são recompensas tardias válidas somente quando provenientes de eventos reais e segregados; geração, ciclos e impacto estimado não contam como resultado.

Memórias confirmadas podem orientar novas versões; candidatas exigem cautela; contraditas e retiradas não podem ser reutilizadas. Evidências estruturadas ficam no MySQL e artefatos grandes podem ficar no S3 privado com checksum e referência auditável.

## Métricas e rollout

A qualidade é medida por aprovação independente, reincidência, tempo e custo por landing aprovada, clique no CTA, início de checkout e vendas posteriores. Texto produzido, ciclos e impacto estimado não contam como resultado. A versão nasce em `TEST`; somente resultados reais e auditáveis autorizam futura ativação.

## Replay e promoção de estratégias

Dédalo separa memória de estratégia promovida. Uma hipótese só entra no playbook operacional depois
de comparar baseline e candidata nos mesmos casos congelados, passar em holdout fora da amostra,
regressão, custo e validação local. O conjunto mínimo é de dez replays e cinco casos de holdout. O
agente consulta promoções pelo MCP, mas não executa a avaliação nem promove a própria hipótese.
