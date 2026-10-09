# Vega — registro da entrega para homologação

## Estado e causa

Em 09/10/2026, o ciclo 10 / experimento 103 / produto 4, cadeia 26, chegou à execução 59,
atividade `technicalHomologation`, em `WAITING_INPUT`. Não havia tarefa de Psique nessa atividade.
Os contratos de jornada, entregas e acesso 661/663/666 estavam concluídos. A variante privada
`musa-pde-entry-v13-primeiro-ajuste-aplicavel` já existia na imagem publicada pelo PR 5544,
`da3f020cf81877e00cdd1c59e00f203347d2b373`, com fonte
`d557a6097b433976b993b63af251364b60a0ef70ff2e762af28e4920f811d373`.

A consulta MCP ao diário do ciclo encontrou apenas os eventos de aprendizado e planejamento.
Não havia `REGISTER_PROTOTYPE` da v13. O evento 48 era exclusivo do predecessor 7, v12.
O contrato público confirmou suporte às variantes v12/v13, geração sintética determinística,
pagamento e publicação desabilitados e mídia zero. A disponibilidade do backend corretamente
impedia a homologação sem a aceitação corrente; não havia motivo para repetir Dédalo.

## Escolha e melhoria reutilizável

Reimplementar a v13 duplicaria uma entrega já existente. Herdar a prova da v12 misturaria versões.
Revalidar e registrar a mesma v13 pelo comando existente resolve o vínculo com menor esforço,
preservando os pareceres anteriores e as revisões independentes. Esta é a opção adotada.

O prompt compartilhado de ajuda passa a conferir o registro antes de recomendar nova construção.
A regressão executável compõe o evento aceito, o contexto real e a disponibilidade técnica:
sem prova há bloqueio; prova histórica não libera; prova corrente entrega URL e linhagem exatas.
O teste repete o percurso com outra identidade. Não cria fila, serviço ou autorização adicional.

## Matriz local e evidência

- Mesmo backend e harness versionados, MySQL 5.7 efêmero e frontend compilado na sandbox.
- Produto/ciclo/experimento sintéticos 91004/91002/91092, separados dos dados comerciais.
- Cinco cenários do harness: aderente em desktop/iPhone 15 Pro/Pixel 7, recuperação no iPhone
  e segurança no Pixel. Todos passaram; duração total 15 segundos.
- Resultado contextual, eventos, callbacks determinísticos, salvar/retomar, entrada preservada,
  acessibilidade básica, ausência de erro do navegador, layout e privacidade passaram.
- Os 13 controles técnicos passaram, sem modelo pago, cobrança, publicação, campanha ou mídia.
- Antes da melhoria, 64 testes relevantes de backend passaram. Após acrescentar a regressão
  parametrizada, os 43 testes relacionados de registro, contexto e disponibilidade passaram;
  os relatórios dos seis conjuntos registram 66 casos sem falhas. ArchUnit e contratos comerciais
  foram preservados.
- Os 39 testes relevantes da tela, a verificação de tipos e o build administrativo passaram.
- Os seis testes de clareza da continuidade v12/v13 passaram na sandbox. A imagem publicada,
  contrato e fonte foram conferidos por leitura em desktop/iPhone/Pixel. A execução simultânea
  encontrou o limite de processos da sandbox; somente os casos afetados foram repetidos com
  um navegador por vez e passaram.
- `bash -n` e ShellCheck aprovaram os scripts existentes usados como referência para a matriz.

## Passagem comprovada no ciclo corrente

Às 14:53 UTC de 09/10/2026, a opção **Registrar implementação já testada** da tela registrou
a mesma v13, URL e imagem: evento 64, revisão 3, ação `REGISTER_PROTOTYPE`, resposta HTTP 200.
O registro não criou orçamento, campanha nem aprovação independente.

O backend reconciliou a execução 59 sem novo disparo manual. A tarefa técnica 696 foi aprovada,
com os cinco cenários e três dispositivos, e a revisão independente aderente de Psique 697
iniciou automaticamente. O estado mudou de `WAITING_INPUT / technicalHomologation` para
`WAITING_ACTIVITY / psiqueAdherent`. Custos ausentes da tarefa determinística não são declarados
como faturamento zero; os custos das revisões por modelo permanecem no teto cumulativo vigente.

As provas locais são sintéticas e não aprovam o produto em nome de Psique ou Têmis. O teto original
de US$ 10 é cumulativo e já foi autorizado para o ciclo 10; mídia e vídeos pagos estão excluídos.
Custos conhecidos continuam estimativas, sem fatura conciliada. Esta correção não comprova vendas.

Capella já concluiu o Processo 4 após o PR 5559, integrado como
`64d3339f1beba108ba9b192a8679c6c5f36e5532`: execução 64 concluída, nenhuma atividade restante.
Preservar essa entrega; não abrir nova tarefa ou PR para reproduzi-la.
