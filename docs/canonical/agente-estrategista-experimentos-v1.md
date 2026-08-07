# Agente Estrategista de Experimentos v1

## Responsabilidade

Transformar sinais reais de sessões, funil e aprendizados em três alternativas pesquisadas de experimento. O agente recomenda; o Operador de Crescimento prioriza e acompanha; o usuário autoriza publicação, preço, gasto e comunicação.

## Fontes de verdade

- sessões e eventos persistidos do experimento;
- funil consolidado pelo backend;
- aprendizados fechados do produto e do experimento;
- fontes públicas identificadas por URL, título e data de acesso.
- memória comportamental estruturada e vigente no MySQL;
- artefatos textuais anonimizados, criptografados e privados no S3.

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

O frontend exibe status, pergunta, exatamente três alternativas, recomendação, hipótese, métrica, critérios de continuar, ajustar e parar, fontes públicas e diagnóstico técnico quando houver falha. O histórico não depende dos logs do worker.

O timeout padrão do Codex é de 40 minutos. O worker processa no máximo uma pesquisa por ciclo e nunca avança experimento, publica ativo ou executa a recomendação.

A autenticação Codex usa o volume persistente e previamente autenticado `/opt/growth-operator/codex-home`, compartilhado em modo operacional pelos agentes executados com o mesmo UID. O deploy deve validar que o volume está gravável pelo usuário do container e que `codex login status` reconhece a sessão. Criar um diretório exclusivo vazio não configura autenticação e deve bloquear o deploy com diagnóstico explícito.

## Endpoints operacionais

- `POST /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: solicita pesquisa;
- `GET /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: lista histórico e pareceres;
- `POST /api/experiment-strategist/v1/internal/executions/pending/claim`: reserva uma pendência;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/complete`: recebe o parecer;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/fail`: recebe a falha detalhada.
