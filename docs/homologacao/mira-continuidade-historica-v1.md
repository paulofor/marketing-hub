# Continuidade visível após homologação encerrada — 06/10/2026

## Evidência e causa

Mira #10, cadeia #26: decisão #3 aprovada (evento #26), ciclo #3 ADJUSTED,
experimento #93 INVALIDATED. A execução #41 foi pausada antes do encerramento.
O histórico já expõe CLOSED e três provas de quatro; o controle preservava PAUSED
na resposta e apresentava a quarta atividade como atual. A atividade não tinha
um destino de continuidade. O card de produtos já foi corrigido em outra entrega
para abrir o ciclo, portanto não requer outra lógica paralela.

Consultas: UI produtos/ciclo/processo, APIs activity-executions e automation/v1,
MCP db_query nas tabelas learning_sales_cycle_v1, learning_sales_cycle_event_v1 e
product_process_run_v1. O filtro de logs cycleId=3 retornou zero linhas na retenção atual.
Capella #7 tem sucessor #5/#98; Mira ainda não possui sucessor. Não copiar orçamento,
experimento ou decisão de outro produto. O teto USD 10 da conversa pertence a Alcyone.

## Escolha

Só trocar o botão não resolve a entrada por links antigos. Reabrir a homologação
renovaria indevidamente o histórico. Expor encerramento e continuidade em todos os
pontos da tela preserva provas e dirige ao contexto oficial sem mutações na leitura.

## Matriz de aceite antes dos testes

- Pausa com referência encerrada: resposta CLOSED, 3/4, sem atividade atual executável,
  sem comandos, custo e pausa persistida preservados; nenhuma tarefa/evento criado.
- Pausa legítima com referência aberta: mantém PAUSED e retomada compatível.
- Produto com ajuste aprovado sem sucessor: destino do próprio ciclo com identificação
  explícita; não pedir nova homologação histórica nem fingir preparação em andamento.
- Produto independente com outro ciclo e cadeia: nenhuma identidade fixa ou transferência
  de provas, decisão, limite ou mídia.
- Falha/ausência de consulta ou cadeia divergente: não oferecer destino de outro contexto.
- Página histórica: cabeçalho, resumo e atividade pendente explicam ausência de ação
  naquela referência e oferecem continuidade; não dizem que a etapa encerrada rodará.
- Caminho anteriormente válido: atividades abertas e comandos existentes preservados.
- Bundle local com APIs simuladas: Chromium desktop, iPhone 15 Pro e Pixel 7; links,
  ausência de overflow, erros JavaScript e POSTs; fixture de automação gerada no teste
  Java de persistência. Dados sintéticos não entram nas métricas comerciais.

## Harness e limites

O harness não cobria uma pausa anterior ao encerramento vista conjuntamente com a
projeção atual das provas. A regressão percorre persistência e serialização reais;
a fixture alimenta o navegador local. Não adicionar prompts ou serviços de agentes:
a causa está na apresentação do estado e no encaminhamento do fluxo.
Nenhum teste ou leitura autoriza nova análise paga, mídia ou cobrança. A chegada ao
mercado continua dependendo da execução do sucessor e dos gates e limites próprios.

## Resultado local

- Backend: suíte completa de 4.125 casos, 27 dispensados pela configuração existente.
  Três expectativas antigas falharam (URL sem cycleId e PAUSED em referência encerrada);
  corrigidas conforme o contrato, repetindo as quatro suítes afetadas. Relatórios finais:
  4.098 executados com sucesso, zero falhas/erros. Persistência H2 comprova pausa e custos
  preservados; HTTP comprova CLOSED sem comandos. Nenhum changelog foi alterado.
- Frontend: 129 testes afetados aprovados; typecheck, build e Prettier concluídos.
- Bundle Vite local: desktop, iPhone 15 Pro e Pixel 7 aprovados, com resposta serializada
  pelo teste Java, navegação para cycleId exato, sem mutações, erro JS ou overflow.
- Backend empacotado e Spotless dos Java alterados aprovados; diff revisado. Comentários
  de responsabilidade de classes e métodos preservados/atualizados em português.
- O orçamento adicional de Mira não foi presumido: o limite USD 10 existente é de Alcyone.
  A consulta e a validação não criaram sucessor, tarefa paga, anúncio ou compra.

A prova de publicação e a nova leitura de produção serão vinculadas ao PR desta entrega.
Testes locais não comprovam chegada ao mercado, vendas ou margem.

## Regressão adicional encontrada no CI e resolvida localmente

O job integrado de automação do PR #5517 identificou que o novo aviso escondia o
motivo de um subprocesso dispensado (`CLOSED`, destino já aprovado). A causa estava
na renderização do cabeçalho, não no avanço ou na persistência. Teste unitário
reproduziu a ausência antes da correção; o motivo passou a ser preservado junto da
orientação de continuidade. As verificações anteriores não foram enfraquecidas.

Validação adicional local: 28 testes dos dois componentes (total afetado agora 130),
typecheck/build, 41 cenários de API/ciclo de vida com MySQL 5.7 real e dependências
simuladas, reinício do backend, matriz de automação, versões retiradas, reserva de
fila e reserva projetada em desktop/iPhone/Pixel. O mesmo browser-matrix que falhou
no CI passou integralmente, incluindo o retorno ao pai. A matriz de Mira também
passou novamente com o bundle corrigido. Topologia temporária isolada e removida.
