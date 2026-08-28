# Agente Estrategista de Experimentos v1

## Responsabilidade

Atuar como Estrategista-Chefe de Mercado: transformar sinais reais, pesquisa de clientes,
comportamento observável, concorrência e portfólio em uma decisão sobre o que vender, para quem, por
que será desejado e como posicionar. Atena compara exatamente três alternativas e congela a escolhida
em um Contrato Estratégico de Mercado; Hermes opera distribuição, instrumentação e otimização sem
redefinir essa estratégia; o usuário mantém as aprovações de publicação, preço, gasto e comunicação.

## Contrato Estratégico de Mercado v2

Por decisão de 2026-08-28, Atena é a única autora de segmento, comprador, problema, desejo,
comportamento, concorrência, lacuna, posicionamento, tese de oferta e hipótese causal. Cada pesquisa
`READ_ONLY_RESEARCH` deve produzir `marketStrategicContract` com versão
`MARKET_STRATEGY_V2`, status, evidências, métrica e critérios de continuar, ajustar e parar.

O backend preserva a execução autora, o conteúdo e seu SHA-256. O contrato fica disponível às
tarefas posteriores como contexto persistido; parecer histórico v1 sem esse artefato é explicitamente
`MISSING`, nunca completado a partir dos campos do experimento. `READY_FOR_OPERATION` exige ao menos
duas classes independentes de evidência; caso contrário, o status é `INSUFFICIENT_EVIDENCE`.

Plutus ou o plano humano aprovado governam preço e economia. Dédalo materializa a estratégia em
produto, landing e artefatos não audiovisuais; Apolo materializa o audiovisual; Têmis apenas revisa
a integridade comercial do resultado real.
Dédalo materializa o produto e as superfícies. Psique valida a resposta humana. Hermes mede e opera o
crescimento. Se eventos posteriores contradisserem uma decisão estratégica, Hermes bloqueia a
operação e solicita uma nova execução de Atena em vez de reescrever o contrato.

## Fontes de verdade

- sessões e eventos persistidos do experimento;
- funil consolidado pelo backend;
- aprendizados fechados do produto e do experimento;
- fontes públicas identificadas por URL, título e data de acesso.
- memória comportamental estruturada e vigente no MySQL;
- artefatos textuais anonimizados, criptografados e privados no S3.
- definições comerciais versionadas dos produtos e comparações persistidas de seus experimentos.
- snapshots agregados do Microsoft Clarity, quando configurado, limitados a página, origem e dispositivo.

## Comportamento real agregado com Microsoft Clarity

O Microsoft Clarity entra como recurso opcional do Estrategista, não como agente novo. Um adaptador
versionado expõe somente a consulta agregada `PAGE`, `SOURCE` e `DEVICE`; ferramentas de gravação,
identificadores individuais e timelines não ficam disponíveis ao agente. Cada consulta deve informar
o experimento, usar janela de um a três dias e ser filtrada pela URL pública `flows/exp-<id>-...`.

O backend autoriza e persiste a requisição, a resposta bruta, o status, a dimensão, a janela, o
experimento, os horários e o custo direto do provedor, atualmente zero. O limite preventivo é de três
snapshots por execução e nove por dia, preservando uma chamada da cota pública para diagnóstico.
`mh_test=1` e a marca persistida `mh_internal_test` nunca podem carregar o coletor Clarity.

Clarity opera em modo sem consentimento de armazenamento por padrão (`consentv2` com armazenamento de
anúncios e analytics negado), sem identificadores customizados e com formulários mascarados. Mudança
para cookies ou associação entre páginas exige consentimento explícito e nova decisão canônica.

Scroll, engajamento, rage clicks, dead clicks, quick backs, erros e desempenho são sinais observados,
não causas. O Estrategista deve confrontá-los com o funil interno, registrar explicação concorrente e
declarar amostra insuficiente quando não houver base segura. É proibido inferir emoção, intenção,
saúde, personalidade ou outro perfil individual.

## Aprendizado de portfólio

O Estrategista deve comparar formatos após amostra mínima, venda, entrega, evidência de valor,
reembolso ou encerramento. O parecer registra fatos observados, lacunas, nível de confiança, variável
isolada e próximo teste recomendado. O agente não pode declarar um formato vencedor sem consequência
comercial e humana auditável, incluindo venda aprovada e entrega satisfatória. Ausência de eventos ou
instrumentação incompleta deve resultar em evidência insuficiente, nunca em ranking inventado.

MUSA, Agenda Cheia e novos formatos usam as mesmas dimensões comparáveis, preservando diferenças de
entrega, receita, unidade de valor e evidência pós-entrega. O Estrategista não inicia, pausa, avança ou
encerra experimentos; essas decisões operacionais permanecem com o Operador de Crescimento e com a
autorização humana aplicável.

### Matriz de homologação ponta a ponta

- caminho feliz: dois ou mais formatos com eventos auditáveis produzem comparação, três alternativas e próximo teste;
- validações: parecer incompleto, sem três alternativas ou sem fronteira do Operador é rejeitado;
- falhas: ausência de eventos, integração indisponível ou evidência contraditória resulta em lacunas explícitas e baixa confiança;
- integrações e observabilidade: snapshot do backend, request/resposta brutos, modelo, custo, fontes e status permanecem persistidos;
- integração Clarity: caminho feliz, token ausente, cota, timeout, resposta granular bloqueada e segregação por experimento são cobertos;
- métricas: vendas, entrega, satisfação, reembolso, margem, esforço e repetibilidade são comparados sem converter proxies em vendas;
- segregação: resultados permanecem vinculados ao produto, planejamento e experimentos corretos;
- interface: o painel comunica em desktop e mobile que o Estrategista recomenda e o Operador executa.

A rodada local completa deve validar desktop, iPhone 15 Pro e Pixel 7, comprovando que o coletor não é
carregado em `mh_test`, não causa overflow e não interfere no CTA, checkout ou analytics próprio.

## Memória híbrida

O MySQL é a fonte de verdade de hipóteses, mecanismos, evidência, confiança, validade, status e resultado observado. O S3 guarda somente relatórios e transcrições textuais extensas depois de anonimização no backend, com hash, retenção e criptografia. E-mail, telefone, CPF e IP não podem ser persistidos no artefato.

Memórias têm validade explícita e um dos estados `HYPOTHESIS`, `CONFIRMED`, `CONTRADICTED` ou `INCONCLUSIVE`. `CONFIRMED` exige resultado humano ou comercial posterior auditável. Conteúdo vencido não entra em novas pesquisas.

## Ciência comportamental

A biblioteca versionada orienta hipóteses sobre fricção, incerteza, risco percebido, prova, sobrecarga de escolha, recompensa tardia, aversão ao arrependimento, confiança e adequação da promessa. Ela não autoriza diagnóstico psicológico nem inferência de intenção como fato. Toda interpretação deve registrar comportamento observável, explicação concorrente e teste de validação com fonte científica auditável.

## Autoridade

O modo inicial é `READ_ONLY_RESEARCH`. O agente não cria ou altera campanha, preço, orçamento, página, ativo, publicação ou comunicação. Toda recomendação exige aprovação humana para execução.

## Contrato de qualidade

Cada execução deve diferenciar fato, inferência e hipótese; apresentar exatamente três alternativas; comparar benefício, risco, esforço e aderência; escolher uma; e definir métrica principal e critérios de continuar, ajustar e parar.

Antes de propor as alternativas, o agente deve construir inteligência de mercado auditável: linguagem literal de clientes, mapa de concorrentes com promessa, mecanismo, entrega, preço visível, CTA, canal, prova e esforço residual, além da lacuna competitiva. O insight deve seguir a estrutura “o mercado oferece X, mas o cliente ainda precisa fazer Y; oportunidade de reduzir Y por meio de Z”.

Cada alternativa deve declarar comprador, promessa verificável, mecanismo, entrega, canal, estágio mental atendido e pelo menos duas evidências de sustentação. A recomendação deve incluir posicionamento concreto, risco de expectativa e regra para atualizar a memória como confirmação, contradição ou resultado inconclusivo.

Recomendação e experimento criado não contam como resultado. O Índice de Maturidade só deve reconhecer resultado quando existir consequência humana ou comercial posterior auditável. A autonomia permanece bloqueada até dez decisões consecutivas confirmadas sem violação de autoridade.

## Pesquisa externa orientada a vendas

O worker deve oferecer busca pública e Chromium/Playwright versionados em sua imagem de produção. A imagem deve fixar uma distribuição Linux suportada pela versão instalada do Playwright, com teste de contrato protegendo a base escolhida e build obrigatório do container no CI. A pesquisa parte sempre do gargalo e cruza evidência interna com sinais externos sobre linguagem e dor do público, demanda, concorrência, ofertas, canais, tendências sociais/econômicas, capacidade de entrega, margem e risco. Para novos nichos, deve comparar demanda observável, urgência, disposição de pagar, saturação, facilidade de acesso ao público e aderência operacional.

Cada fonte registra URL, título, data, tipo, método de coleta e aprendizado. Páginas decisivas devem ser confirmadas com Playwright. Snippets, anúncios ativos, seguidores, visualizações, comentários e tendências são proxies exploratórios e nunca comprovam vendas. Recomendações de nicho, produto ou oferta exigem ao menos duas classes independentes de evidência; sem isso, o parecer deve declarar evidência insuficiente.

A pesquisa de clientes deve usar linguagem pública literal de avaliações, comentários, fóruns, comunidades ou páginas acessíveis sem login, preservando URL e contexto. Fala do concorrente não pode ser apresentada como voz do cliente. O agente não copia criativos nem identidade: modela mecanismos comerciais e lacunas de esforço, demora, risco e complexidade.

O mapa psicológico de `docs/neuron`, consolidado em `docs/canonical/psicologia-aplicada-ofertas-canon.v1.md`, orienta a jornada `UNFAMILIAR → RELEVANT → COMPREHENSIBLE → PLAUSIBLE → PERSONALLY_VALUABLE → DESIRABLE → PURCHASABLE`. Estado mental, emoção e identidade permanecem hipóteses até consequência humana posterior auditável.

## Fluxo operacional

O usuário solicita a pesquisa no painel do planejamento comercial. O backend congela o contexto e persiste a execução como `PENDING`. O `experiment-strategist-worker` reserva uma execução pelo endpoint interno canônico, pesquisa em sandbox somente leitura e devolve o parecer estruturado ou a causa completa da falha.

O schema enviado ao Structured Outputs deve declarar `type` em todas as propriedades, inclusive quando também usar `const`. O deploy só considera o worker pronto quando, além do container e da autenticação Codex, o endpoint operacional publicado na porta 8096 responder `UP`; esse endpoint é a origem canônica dos logs consultados pelo MCP. A verificação de prontidão deve medir e registrar esses três requisitos separadamente em cada tentativa, tolerar espaços válidos no JSON do Actuator e oferecer uma janela mínima de dois minutos antes de declarar falha.

O frontend exibe status, pergunta, exatamente três alternativas, Contrato Estratégico de Mercado,
recomendação, hipótese, métrica, critérios de continuar, ajustar e parar, fontes públicas e
diagnóstico técnico quando houver falha. O histórico não depende dos logs do worker.

O timeout padrão do Codex é de 40 minutos. O worker processa no máximo uma pesquisa por ciclo e nunca avança experimento, publica ativo ou executa a recomendação.

A autenticação Codex usa o volume persistente e exclusivo
`/opt/marketing-hub/agents/strategist/codex-home`. É proibido compartilhar o diretório mutável de
outro agente. O deploy deve validar que o volume está gravável pelo usuário do container e que
`codex login status` reconhece a sessão. O bootstrap inicial pode migrar a sessão operacional
confiável conforme o cânone premium, sem imprimir credenciais; criar um diretório exclusivo vazio
não configura autenticação e deve bloquear o deploy com diagnóstico explícito.

## Endpoints operacionais

- `POST /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: solicita pesquisa;
- `GET /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: lista histórico e pareceres;
- `POST /api/experiment-strategist/v1/internal/executions/pending/claim`: reserva uma pendência;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/complete`: recebe o parecer;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/fail`: recebe a falha detalhada.
