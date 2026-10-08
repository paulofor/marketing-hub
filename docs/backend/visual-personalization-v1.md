# Personalização visual privada — contrato v1

O backend principal governa produto, plano, experimento, orçamento, tentativa e resultado.
O backend PDE transporta somente APIs de seu domínio; pde-ai-worker executa a imagem.
A tela administrativa abre a preparação e a tela PDE recupera essa mesma entrega.
Nenhuma tela comanda atividades seguintes nem publica oferta.

Prefixo: `/api/pde/visual-personalization/v1`.

| Endpoint | Condição e resultado |
| --- | --- |
| `GET /products/{productId}/context?commercialPlanId&experimentId` | Origem, autorização, subtotal e último resumo técnico, sem credencial/imagem |
| `POST /products/{productId}/preparations` | Entrada sintética, consentimento, versão, autorização e chave idempotente; PLANNED/PLAY |
| `GET /preparations/{jobId}` | X-PDE-Visual-Session; entrada e entrega da própria sessão |
| `POST /preparations/{jobId}/reconcile` | Mesma credencial; reaplica resposta/custo preservados, sem IA |
| `GET /internal/stage-executions/pending` | Credencial PDE; fila, replay e reserva sem request auditada |
| `POST /internal/stage-executions/{jobId}/claim` | Reserva vinculada à origem; recuperação só antes da request auditada |
| `POST /internal/stage-executions/{jobId}/request` | Request real antes do provedor; entrada/hash/modelo/Flex/tamanho/formato/limites conferidos |
| `POST /internal/stage-executions/{jobId}/result` | Correlação e payload persistidos antes de aplicar custo e imagem |
| `POST /internal/stage-executions/{jobId}/replay` | Aplica o mesmo payload sem nova inferência |

Entrada: ocasião até 500 caracteres, 2–12 peças até 150 caracteres cada, preferências
até 500 e restrições práticas até 500. Não aceita upload/foto corporal. A credencial
opaca entra pelo fragmento da URL, é removida da barra e enviada em header; o banco
armazena somente seu hash. Respostas privadas são no-store.

Preparação no Hub: `/ai/image-generator?mode=personalized-preparation&productId=…&commercialPlanId=…&experimentId=…`.
O modo manual anterior permanece válido. A entrega separada usa `personalization.html`
na origem privada cadastrada. Leitura e recuperação não geram imagens.

## Auditoria, custos e recuperação

Reutiliza image_generation_request e studio_cost_ledger_entry, sem fábrica/ledger paralelo.
Prefixo pde-visual-v1- e estados PDE_* segregam a preparação do histórico manual.
Request/response brutos, identidades, hash da entrada, versão, horários e estimativa
permanecem auditáveis. A prova técnica não entra entre artefatos aprovados de agentes.

Esta primeira versão lê o formato explícito já persistido no plano: data, teto total,
produto/experimento e exclusões de mídia/cobrança. Outros textos ambíguos falham fechado;
não extrair um orçamento de uma recomendação. O saldo inclui ledger do contexto e
pareceres de Atena/Plutus desde a autorização. Falha terminal com custo conhecido
permanece no subtotal. Custo desconhecido ou tarefa ativa impede nova inferência;
custos anteriores permanecem no histórico sem serem convertidos em zero.

Executor limitado a uma imagem PNG 1024×1024, uma ferramenta e até 4096 tokens de saída
do modelo principal, em Flex. Exige saldo técnico mínimo USD 0,50 e reserva o saldo
inteiro para impedir imagens paralelas no contexto. Esse mínimo é proteção operacional,
não meta de margem ou nova autorização. Auditorias de referência 115/116 tiveram
estimativas USD 0,079122 e 0,078388. Estimativa por tokens não é fatura conciliada ou
garantia do provedor sobre cobrança final; divergência/custo ausente bloqueia novas chamadas.

O publicador versionado mantém um único pde-ai-worker e volume persistente de callbacks.
Retorno perdido da reserva pode ser recuperado somente sem request auditada. Depois da
request, não renovar reserva automaticamente: timeout com possível consumo permanece
custo desconhecido. A resposta fica no spool privado até confirmação do callback e no
backend antes de aplicar custo/saída. Reinício/replay reutilizam a resposta. A fila
visual indisponível não interrompe MUSA durante a publicação encadeada.

PNG íntegro, dimensão e correlação são aceite técnico da geração, não parecer semântico.
functionalReviewStatus=PENDING_INDEPENDENT_REVIEW, trafficClass=AGENT_VALIDATION,
paymentEnabled=false e published=false permanecem explícitos. Íris distingue prova
integrada disponível de revisão pendente, sem liberar aquisição, escala, cobrança
ou comunicação. Provas estáticas anteriores não são promovidas para geração real.

Os cinco critérios e vídeo permanecem no contrato de Íris, dependentes das entradas
e pareceres do percurso existente. Vídeo pago não é autorizado aqui; demonstrações
precisam mostrar a experiência real, sem promover simulação a evidência comercial.

Validação: [matriz e limites](../homologacao/alcyone-personalizacao-integrada-v1.md).
