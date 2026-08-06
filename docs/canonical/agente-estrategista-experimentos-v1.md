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

Recomendação e experimento criado não contam como resultado. O Índice de Maturidade só deve reconhecer resultado quando existir consequência humana ou comercial posterior auditável. A autonomia permanece bloqueada até dez decisões consecutivas confirmadas sem violação de autoridade.

## Fluxo operacional

O usuário solicita a pesquisa no painel do planejamento comercial. O backend congela o contexto e persiste a execução como `PENDING`. O `experiment-strategist-worker` reserva uma execução pelo endpoint interno canônico, pesquisa em sandbox somente leitura e devolve o parecer estruturado ou a causa completa da falha.

O frontend exibe status, pergunta, exatamente três alternativas, recomendação, hipótese, métrica, critérios de continuar, ajustar e parar, fontes públicas e diagnóstico técnico quando houver falha. O histórico não depende dos logs do worker.

O timeout padrão do Codex é de 40 minutos. O worker processa no máximo uma pesquisa por ciclo e nunca avança experimento, publica ativo ou executa a recomendação.

## Endpoints operacionais

- `POST /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: solicita pesquisa;
- `GET /api/experiment-strategist/v1/commercial-plans/{planId}/executions`: lista histórico e pareceres;
- `POST /api/experiment-strategist/v1/internal/executions/pending/claim`: reserva uma pendência;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/complete`: recebe o parecer;
- `POST /api/experiment-strategist/v1/internal/executions/{id}/fail`: recebe a falha detalhada.
