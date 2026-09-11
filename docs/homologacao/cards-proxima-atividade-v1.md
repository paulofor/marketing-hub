# Acesso direto à atividade nos cards de produtos

Data: 11/09/2026. Escopo: início e catálogo administrativo, somente apresentação e navegação.

## Evidências e decisão

- A captura fornecida e o Chromium na página inicial pública mostram o card de Vega aguardando
  o contexto do ciclo, enquanto Rigel oferece links ao modelo BPM e aos subprocessos.
- O contrato de posições respondeu em 4,01 s. O `process-context` de Vega respondeu em 18,24 s:
  ciclo 2, experimento 92, atividade 3.7 `psiqueAdherent`, `IN_PROGRESS`, responsável Psique.
  O carregamento observado não comprovava falha nem perda do ciclo.
- O `activity-executions` do produto 9/processo 75 respondeu em 6,39 s: `optimization`,
  atividade 6.1, `NOT_STARTED`. O card sem ciclo não consumia essa orientação.
- Histórico comparado: `vega-card-ciclo-atual-v1.md` já protege o retorno de Vega ao processo 3.
  `LOOP-BPM-EXPERIMENTO-PLANEJADO-MASCA-OPERACAO` reforça o isolamento da passagem/experimento;
  o link não deve retornar ao processo 6 somente pelo status comercial.
- Backend existente: `BusinessProcessActivityExecutionController` / serviço canônico de
  execução com DTOs em `service.productProcessExecutions`, e controller/service versionados
  de ciclos. Endpoints documentados em Swagger são reutilizados sem alteração Java/schema.
- MCP `db_query` confirmou os nomes internos Vega (4) e Rigel (9), ambos `ATIVO`. A primeira
  descoberta de tools expirou; a consulta seguinte respondeu normalmente. Os registros estão
  em `artifacts/product-next-activity/db-evidence.json`, sem qualquer escrita no banco.

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Atalho para a página do processo atual | Alteração pequena | Ainda exige procurar a atividade e pode confundir ciclos | Insuficiente |
| Novo resumo de navegação em lote no backend | Menor payload para muitos cards | Novo contrato e manutenção, esforço maior | Possível evolução se o volume justificar |
| Destacar a atividade oficial usando contratos existentes | Destino preciso, contexto preservado, esforço moderado | Uma leitura por card sem ciclo | Escolhida |

## Matriz definida antes dos testes

| Critério | Validação local |
| --- | --- |
| Caminho feliz | Início e catálogo → atividade exata de Vega com ciclo e Rigel sem ciclo |
| Integração | Contratos oficiais de leitura, mesma chave de cache da tela de atividades |
| Estados | Disponível, pendente, em execução, bloqueado, concluído sem orientação e sem posição |
| Falhas | Resposta vazia/divergente, erro inicial e ao atualizar, timeout e retentativa |
| Continuidade | Âncora visível no destino; produto, cadeia e ciclo preservados ao clicar e voltar |
| Aprendizado | Memória anterior e contexto do segundo ciclo continuam acessíveis |
| Isolamento e métricas | Somente GET; APIs locais sintéticas; nenhum evento comercial ou tarefa produtiva |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, teclado, alvo de toque e overflow |
| Observabilidade | Capturas, requisições e erros JavaScript registrados; falhas visíveis no card |
| Regressão | Testes frontend relacionados, TypeScript, build, Prettier e revisão do diff |

Os testes usarão o frontend real com dependências HTTP simuladas localmente. Uma rodada completa
sem defeito conclui a homologação; havendo correção após falha, duas rodadas completas consecutivas
devem passar após a última correção. Nenhuma publicação será usada para testar.

## Resultado

Nas verificações preparatórias, o teste de navegação detectou perda de `chainId` no ambiente
de navegador que não implementa `URLSearchParams.size`; a composição agora usa `toString()`.
O teste de retentativa detectou que o botão desaparecia durante a nova consulta; o estado de
primeiro carregamento foi separado da retentativa, que mantém o botão desabilitado e visível.
Também foi ajustado o retorno do hook de preparação do próprio teste para cumprir TypeScript.

**Duas rodadas completas consecutivas aprovadas: `final1` e `final2`.** O código de aplicação
permaneceu inalterado entre elas, gerando os mesmos bundles JavaScript e CSS.

| Controle por rodada | Resultado |
| --- | --- |
| Testes de componentes e páginas | 87/87, em oito arquivos |
| TypeScript | Aprovado |
| Build do frontend | Aprovado |
| Navegador | 15/15 controles: 12 navegações (dois produtos × duas telas × três dispositivos) e três grupos de estados/recuperação |
| Formatação | Prettier aprovado |
| Diff | Revisão de escopo e whitespace aprovada |

Verificação adicional: o teste existente `useProductProcessActivityExecutions.test.tsx`
passou (1/1), cobrindo fila, execução, falha, reconexão, conclusão e atualização das consultas
com a mesma referência. A extração da leitura reutilizável preservou o acompanhamento da tela
de tarefas, sem iniciar polling nos cards.

Os percursos chegaram à âncora da atividade correta e voltaram aos cards, preservando o
segundo ciclo do Vega e seus aprendizados, sem contaminar Rigel com o experimento 92.
O destino mudou conforme a orientação recebida, inclusive para bloqueio ou trabalho interno
do ciclo. A retentativa permaneceu visível e desabilitada durante a consulta. Não houve erro
JavaScript, requisição inesperada, escrita ou evento comercial nos testes.

Capturas, requisições e logs ficam em `artifacts/product-next-activity/final1/` e `final2/`.
A prévia de referência é `final2/browser/desktop-home.png`. Contratos produtivos consultados
em leitura estão na pasta superior; fixtures reproduzíveis e runner estão em
`infra/testing/product-next-activity/`.

Limites: integrações HTTP foram simuladas na sandbox; iPhone e Pixel são emulações Chromium,
sem Safari físico. O build mantém o aviso preexistente sobre tamanho de bundle. Servidores
locais e navegadores foram encerrados. Nenhuma topologia Docker foi criada.

Entrega: somente frontend e documentação, com endpoints existentes. Não houve alteração
Java/Liquibase, commit, push, PR, deploy, publicação de imagem nem execução de atividade real.
O benefício esperado é reduzir procura e navegação intermediária até o trabalho do produto;
não foi atribuído aumento de vendas ou conclusão de atividade à alteração de interface.
