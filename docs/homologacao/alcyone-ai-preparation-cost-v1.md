# Alcyone — retomada da preparação e custos de imagem — 05/10/2026

## Identidade e limites

Produto 11 / Alcyone / Look para a Ocasião, plano 34 v4, experimento 97 PLANNED,
execução 44, Processo 4 definição 113 v11 na cadeia 26. Autorização do usuário:
USD 10 no total para geração personalizada e pareceres necessários de Atena/Plutus;
sem mídia, campanha ou cobrança. A tentativa interrompida já produziu o lote
`img-batch-51010bf2-9fbb-4dc5-90b5-9e70b97d5670`; não regenerar nem zerar seus custos.

## Causa confirmada e alternativas

O ledger 840/841 marcou as duas imagens COMPLETED com custo ausente. A auditoria 115/116
preserva tokens de gpt-5.6-sol e de gpt-image-2.5-sunburst em `tool_usage.image_gen`.
`StudioCostLedgerService.recordImage` não consumia esses dados. Recuperar imagem não
informa custo e regenerar repetiria pagamento. A solução calcula uma estimativa completa
pelas duas modalidades e permite reconciliar a mesma tentativa sem inferência; custo sem
fonte continua desconhecido. Cobrança oficial já conciliada prevalece sobre estimativa.

Alternativas: ajuste manual por ID (rápido, recorrente); gerar novamente (custo duplicado,
não resolve auditoria); conciliação genérica por tokens na origem (esforço moderado,
reutilizável e escolhida). Não altera retrospectivamente provas privadas em provas comerciais.

## Matriz definida antes dos testes

| Área | Cenário e aceite |
| --- | --- |
| Custo original | Tokens reais da tentativa preservada; somar texto Flex e ferramenta Standard uma vez |
| Outra execução | IDs/produto distintos, mesmos contratos; sem exceções de Alcyone |
| Fonte incompleta | Uso, modalidade, modelo ou tarifa ausentes/contraditórios continuam desconhecidos |
| Falha após geração | Preservar resposta/estimativa mesmo se extração ou derivado falhar |
| Retomada | Conciliar e recuperar sem HTTP externo; repetir conciliação sem duplicar ledger |
| Financeiro | Não substituir cobrança conciliada, nem apagar custo conhecido com callback sem fonte |
| Pareceres | Processo Codex simulado → JSONL → callback → custo de Atena/Plutus; callbacks antigos compatíveis, contadores parciais desconhecidos, cache separado e contextos independentes |
| Integração | Cliente HTTP simulado → auditoria → custo → ledger → resposta da tela |
| UI | Desktop, iPhone 15 Pro e Pixel 7; botão com loading, resultado estimado, pendência e falha visíveis |
| Isolamento | Recusar outro produto/plano/experimento; QA não alimenta métricas de mercado |
| Entrega | Suítes dos módulos alterados, build/formatadores; PR/revisão/merge/workflows/deploy e comportamento publicado |

A tarifa multimodal versionada foi conferida em https://developers.openai.com/api/docs/pricing
em 05/10/2026: texto de entrada USD 5, imagem de entrada USD 8 e saída de imagem USD 30 por
milhão de tokens para GPT Image 2/2.5. Flex do modelo principal não transforma a chamada síncrona
de imagem em Batch. O catálogo existente continua sendo fonte para o modelo de texto.
Resultado é estimativa por uso auditado, nunca fatura reconciliada. Cache de imagem sem divisão
verificável permanece pendência. USD não é convertido em BRL sem fonte de câmbio.

## Limites de homologação

O painel recuperado demonstra três combinações para uma entrada sintética descrita em texto.
Não comprova fidelidade a fotografia enviada, entrega automática no aplicativo, satisfação,
compra ou contribuição. O runtime privado permanece alcyone-private-v3 com fixtures; essa
limitação deve ser fornecida aos agentes em vez de aprovar uma capacidade inexistente.

## Validação local e arquivos recuperados

- Backend: 3.978 casos na suíte completa final, sem falha/erro; 25 dispensas explícitas da suíte.
- Frontend: 869 testes aprovados, TypeScript e build aprovados.
- Integração local: dois contextos distintos, geração → auditoria → ledger → conciliação/replay →
  recuperação; falha de derivação preserva resposta e custo. Nenhum HTTP adicional ao provedor.
- UI: desktop, iPhone 15 Pro e Pixel 7 aprovaram loading, falha acionável, custo pendente,
  estimativa e três reenvios sem geração. Harness intercepta somente rotas /api/; módulos
  Vite em src/api não são confundidos com requisições de negócio.
- Referência curta de custo respeita os 64 caracteres do schema; URL/data completos aparecem
  no contrato de resposta e no recurso de preços versionado. Swagger, Spotless e diff revisados.
- Revisão final inclui empacotamento e repetição dos testes afetados após esse ajuste do limite.

Matrizes PNG recuperadas sem inferência:

| Auditoria | Job | SHA-256 | Estimativa USD |
| --- | --- | --- | ---: |
| 115 | img-956f30f8-d822-4382-aa5c-40372659fa72 | 22a191f921f92eaa8bf06176707d24efd2321668dcd211cc6f0edba161e6bf5a | 0,079122 |
| 116 | img-dcefd44d-4898-4a58-ba98-a747e32f4e9a | 6f6dad75074a8c74e1a99244aefc5c6245cef640ec3199fda4ee0d7411f6d14c | 0,078388 |

Subtotal estimado: USD 0,157510. Fonte: usage e tool_usage preservados nas respostas, tarifa
oficial conferida em 05/10 e preços Flex do modelo principal no catálogo. Não é confirmação de
fatura nem custo zero de outras execuções. Saldo de preparação deve considerar qualquer nova
execução e pendência; esta correção não altera mídia, preço, aprovações ou o runtime privado.

## Extensão comprovada: consumo dos pareceres

Antes de solicitar novos pareceres, o histórico revelou oito execuções recentes de Atena e seis
de Plutus concluídas com `estimated_cost` ausente. O runner genérico de Atena não produzia JSONL
nem enviava tokens; Plutus já os enviava, mas o backend não os usava quando não havia tarefa BPM.
Ambos agora usam os contadores completos e o catálogo existente, sem nova tabela ou serviço.
O parser de Plutus passa a recusar uso parcial em vez de completar os campos ausentes com zero.

Alternativas: estimar manualmente cada parecer (não reutilizável), executar somente tarefas BPM
(não corrige os contratos genéricos existentes), conciliar no callback canônico (escolhida, menor
mudança com cobertura das duas entradas). Histórico sem telemetria continua desconhecido.
O Codex OAuth usa Standard pela exceção funcional existente; não assumir preço Flex.

Validação final da extensão: 47 testes de Atena, 55 de Plutus e sete testes dos adaptadores MCP,
todos aprovados. Os dois Dockerfiles construíram imagens locais; o Codex empacotado aceitou
`exec --help`/`--json` como usuário sem privilégios em containers sem rede/credenciais. A topologia
temporária foi removida pelo projeto Compose exclusivo. Nenhuma inferência ocorreu nesses testes.
Spotless, arquitetura premium, `bash -n` e ShellCheck do validador consultado passaram.

## Correção complementar observada na preparação

Atena #18 completou a revisão da prova isolada e manteve `INSUFFICIENT_EVIDENCE` por fixtures
no aplicativo e economia incompleta. Plutus #69 completou a conciliação, mas registrou recusa
de consulta por aprovação `never`. Seu MCP não anotava as ferramentas GET como leitura; o
snapshot não incluía `nextAction`, onde a autorização USD 10 está persistida na versão 4,
nem a oferta com acesso/recuperação. Histórico #69 permanece intacto.

O schema real de Atena é DECIMAL(38,2), apesar de Liquibase original DECIMAL(12,4), porque o
campo JPA omitira precisão/escala. MySQL 5.7 local reproduziu 0,000928 como 0,00. Precisão
DECIMAL(18,8) é fixada em JPA e migração incremental nos dois especialistas, alinhada ao
cálculo existente. Valores já arredondados permanecem históricos; não são reconstruídos.

| Extensão da matriz | Aceite |
| --- | --- |
| Persistência | Valor positivo inferior a um centavo preservado em JPA e MySQL 5.7; histórico e NULL conservados |
| Migração | Aplicação real, reaplicação sem duplicação e rollback que recusa perder precisão |
| Ferramentas | MCP JSON-RPC real → listagem anotada → GET de execução/memória com IDs distintos; nenhuma consulta vira escrita |
| Política | `read-only` e `approval_policy=never` mantidos; servidor usa `default_tools_approval_mode=writes`, deixando escritas sob aprovação |
| Contexto | Condições e autorização originais, inclusive campos ausentes de outro plano, presentes no snapshot imutável; texto não cria autorização automática |
| Antes/depois | Reusar #18 e #69; reavaliar Plutus somente depois da correção e com contexto novo, dentro do mesmo teto USD 10 |

Referência operacional: https://developers.openai.com/codex/mcp, conferida em 05/10/2026.
Alternativas: repetir o parecer com a mesma fonte incompleta (rejeitada), relaxar aprovação
global (rejeitada), classificar consultas e transmitir o contexto persistido (escolhida).

Validação complementar local: 3.981 casos do backend, sem falha/erro, com 26 dispensas explícitas;
47 testes de Atena e 56 de Plutus aprovados. A migração real também passou separadamente no
MySQL 5.7.44, incluindo precondição de faixa, histórico/nulos, reaplicação e rollback recusado.
O teste confirma as auditorias novas antes do rollback: a reversão da transação do teste não
deve ser confundida com perda provocada pela migração. O servidor MCP real passou em dois
contextos, incluindo fonte HTTP 503 sem sucesso fictício ou escrita.

A primeira suíte completa foi encerrada pelo limite de memória da sandbox (exit 137,
`memory.events` com `oom_kill=1`). Encerrado o Vite temporário e limitada a oito a retenção de
contextos de teste, a suíte terminou com sucesso. O teste JPA dedicado encerra seu contexto após
a classe. As duas imagens locais vieram dos Dockerfiles versionados; o Codex 0.159.3 empacotado
aceitou a política MCP em containers sem rede/credenciais. Nenhum modelo foi chamado na matriz
local. MySQL e volume temporários foram removidos pelo projeto Compose autorizado.

O CI do PR #5506 revelou atestação privada v8 com hash antigo de uma classe compartilhada,
alterada legitimamente nos PRs #5487/#5498. Reproduzido e corrigido localmente por sucessora v9,
preservando v8 e a mesma alcyone-private-v3. O CI do backend passa a validar o pacote real antes
de integrar mudanças nas fontes. Relatório específico: `alcyone-agent-validation-v10.md`.
Não altera fixtures em geração comercial nem transforma a atestação candidata em aprovação.

A extensão passou em 13 testes do contrato de CI, 15 do construtor e no pacote real com 317
arquivos/61 manifestos. Psique: 150 casos, zero falhas e duas dispensas; Têmis: 108 casos, zero
falhas e uma dispensa. As capturas foram geradas pelo script versionado
`infra/testing/commercial-evidence/capture-private-boundary.cjs`, sem mutação ou inferência.
