# Vega — continuidade do inconclusivo aprovado — 06/10/2026

## Causa e evidências iniciais

UI, API e MCP confirmam produto #4, tipo PDE/Opala, cadeia histórica #14,
ciclo #2/experimento #92 em INCONCLUSIVE revisão 22. A proposta Atena #4
foi aprovada no evento #33, em 06/10/2026. Sua recomendação KEEP_FOCUS tem
requiresNewCycle e hipótese de novo teste. O recibo contém somente
decisionProposalId/humanApproved: o parecer completo permanece na proposta.
A política e a fila preparatória admitiam apenas ADJUST/ADJUSTED. O serviço
exigia destino de correção inexistente no inconclusivo e a tela continuava
exibindo solicitação de aprovação. Capella #4/#88, aprovado como ADJUST,
já preparou #5/#98: esse histórico confirma a limitação por ação, não uma
falha de gravação da aprovação.

Medição histórica: 6 visitantes/sessões humanos, nenhum início ou compra,
R$25,42 de gasto conciliado; amostra insuficiente para concluir rejeição.
O contexto da tela não informava plano comercial ou ficha de execução.
A consulta dos vínculos encontrou o plano histórico #3 em BLOCKED, associado
ao predecessor e outras referências; isso não autoriza o consumo do sucessor.
O sucessor usa a mesma identidade cadastrada, oferta/preço de referência
R$67 e hipótese de Atena; não inventa aprovações ou custo completo.

Na homologação integrada, `AUTHORIZATION` com pendências comerciais deixou
de orientar o ciclo e tentou abrir a operação. O histórico #5486 confirma que
a proteção de medição projetada foi estendida também às esperas legítimas.
A seleção passa a consultar o contrato de orientação no contexto autorizado;
a drenagem continua distinguindo projeção de trabalho real. Não retirar a
proteção de encerramento/fila nem declarar uma condição financeira como aceite.

## Alternativas

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Recuperação explícita pela tela com orientação completa | Baixo esforço, preserva a decisão e reduz dúvida | Exige uma ação a cada ocorrência futura | Reservada para históricos |
| Preparação síncrona no recebimento da aprovação | Passagem imediata e recibo correlacionado | Acopla encerramento e criação; exige recuperação separada para aprovações anteriores | Viável, maior acoplamento |
| Fila canônica após aprovação, com recuperação dos históricos pelo mesmo serviço | Passagem durável, preserva autoria e evidências | Intervalo do polling existente; ajuste restrito de elegibilidade, criação e orientação | Adotada |

A fila e os locks já existem. Reutilizá-los resolve novas ocorrências sem criar
outro agendamento; o comando recupera a aprovação de Vega sem repetir a decisão.

## Matriz definida antes dos testes

| Cenário | Aceite |
| --- | --- |
| Caso original: INCONCLUSIVE aprovado com recibo mínimo | Parecer reaproveitado; sucessor único em PLANNING, mídia zero, sem janela |
| Outro produto e histórico de ajuste válido | Mesmo comportamento genérico e regressão de ADJUST preservada |
| INCONCLUSIVE ainda não aprovado | Nenhuma preparação ou inferência nova |
| Decisão editada, STOP, expansão, hipótese vazia, recibo inválido/desatualizado | Bloqueio antes de criar experimento ou tarefa |
| Aprovação → planejamento → construção → comunicação | Motor e MySQL reais, callbacks locais simulados, retorno auditável |
| Concorrência/replay e falha de persistência | Um sucessor; atomicidade; nenhum recibo ou custo perdido |
| Histórico sem adesão à política | GET/deploy/polling não migram; recuperação explícita reutiliza aprovação |
| Espera comercial projetada, contexto retirado e tarefas reais | Orientação correta sem abrir operação; pausa sem callback fictício; encerramento e tarefas reais protegidos |
| Observabilidade e métricas | Evento correlacionado com origem; inconclusivo e autoria preservados; nenhuma venda simulada contabilizada |
| Desktop/iPhone/Pixel Chromium | Orientação e recuperação visíveis, sem escrita por navegação ou nova aprovação |
| Empacotamento e formatação | Artefato contém as classes/contratos testados; Java documentado em português |

A melhoria de harness é proporcional: ampliar os testes físicos existentes
com o recibo mínimo e o encerramento INCONCLUSIVE que estavam ausentes,
incluindo a passagem completa pelos processos e três dispositivos. Não
há novo motor, agendamento ou integração de IA. Modelos externos ficam
simulados; dados e SMTP pertencem somente à sandbox.

A rodada também encontrou fixtures antigas: orientação de ajuste manual já
substituída, links que presumiam ciclo como último parâmetro e preflight
sintético que declarava apenas `OK` e repositório simulado que não atendia à
leitura compacta das tarefas. Os cenários agora conferem contexto por parâmetros,
contrato completo de versão/URL/data/HTTP e status/custo das tarefas no resumo,
preservando os gates e a pausa de trabalho em andamento.
O runner conserva logs do MySQL antes de remover a topologia, inclusive em
falha de inicialização; isso fecha a lacuna de diagnóstico observada na sandbox.

## Resultados

- Rodada inicial completa: 4.134 testes backend sem falhas (27 cenários físicos
  específicos dispensados nessa rodada; MySQL executado separadamente), 890
  testes frontend, typecheck/build e 49 testes do executor Atena sem falhas.
- Após a correção da seleção de esperas: 645 testes das famílias de processo,
  ciclo, fluxo comercial e arquitetura sem falhas ou dispensas.
- API/MySQL 5.7: recibo mínimo inconclusivo aprovado, dois produtos, fila automática
  e recuperação histórica explícita, concorrência, replay, rollback real e janela
  sem gasto; aprovação → planejamento → construção → comunicação com callbacks
  simulados. Histórico, autoria e orçamento preservados.
- Fluxo comercial real na sandbox: sete verificações de condições, autorização,
  pausa/retomada, concorrência e deduplicação, duas identidades e janela expirada.
  Tarefas simuladas e dados QA segregados; nenhuma venda ou chamada externa.
- Navegação em Chromium desktop, iPhone 15 Pro e Pixel 7: ciclos, entrada pela
  cadeia, contexto legado, decisão, fluxo comercial e recuperação do inconclusivo.
  Contratos de produto/cadeia/ciclo, ausência de escrita por leitura e layout
  verificados. Captura mobile inspecionada.
- MySQL: aplicação, rollback, reaplicação e idempotência do catálogo existente.
- `bash -n`, ShellCheck, compilação Python e revisão dos comentários Java passaram.

- Empacotamento: 15 testes da evidência comercial e nove do verificador de recursos;
  4.231 classes testadas idênticas no JAR, 756 recursos íntegros e 519 cartões
  carregados pelo catálogo executável. Spotless dos arquivos alterados aprovado.
- Topologia temporária removida com volumes e órfãos; nenhum serviço produtivo alterado
  por SSH ou por imagem manual.

A implementação está validada localmente; PR e publicação são a próxima etapa.
Os resultados acima são internos;
não comprovam vendas, aprovação de economia ou autorização de mídia. Limites de
IA do novo contexto devem estar autorizados antes de novas chamadas pagas; o
teto anterior de Alcyone não é transferível para Vega.
