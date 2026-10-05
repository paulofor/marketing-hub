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
