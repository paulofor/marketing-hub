# Vega — espera e preparação comercial do ciclo 2

Data: 2026-09-15. Escopo: produto 4, cadeia 14/v14, processo 75/v6,
ciclo 2, experimento 92, `musa-pde-entry-v12-primeiro-ajuste-aplicavel`.

## Diagnóstico confirmado antes da correção

- Navegação pelo processo 75 → **Retomar subprocesso · ciclo #2** abriu o ciclo
  correto. A tela e o MySQL via MCP confirmaram `OPEN/AUTHORIZATION`, revisão 13.
- O evento 16 concluiu a homologação privada no gate 289. Os vídeos 41/42 foram
  aprovados; nenhum evento posterior registra autorização comercial. A autorização
  de USD 20 do evento 11 cobre vídeos, não mídia.
- A execução 4 permanece `WAITING_ACTIVITY`, 0/4, sem `userAction`. O resolvedor de
  orientação reconhecia apenas etapas audiovisuais, descartando `AUTHORIZATION`.
- O formulário pede novamente versão, teto e referências já persistidas. A
  autorização do ciclo e a autorização final da campanha são decisões distintas.
- Processo 56/v6 permite iniciar Psique/Têmis, embora o experimento 92 esteja
  `PLANNED`, sem slot, criativo, checkout, público ou orçamento operacional.
  O gate final reconhece essas faltas; a entrada das revisões não as verifica.
- Histórico comparado: #91 está `USER_STOPPED`, com orçamento, checkout e recibo
  próprios. Reaproveitar sua identidade comercial contaminaria o sucessor. Os
  eventos de 12–15/09 e os loops `LOOP-BPM-DECISAO-HUMANA-COMO-EXECUCAO` e
  `LOOP-CICLO-AUTORIZACAO-SEM-SUPERFICIE-COMERCIAL` sustentam o diagnóstico.
- MCP `db_health`: `ok`, schema `marketinghubdb`. Consultas de logs funcionaram;
  filtro `LearningCycle` não retornou linhas. Ausência desse filtro não comprova
  ausência de atividade; o diário persistido e a conciliação atual foram consultados.

## Alternativas e decisão

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Alterar apenas o rótulo da tela | Esforço baixo | Mantém formulário redundante e avaliações sem insumos | Insuficiente |
| Criar uma nova etapa versionada de preparação comercial no BPM | Responsabilidade explícita e relatório próprio | Esforço alto de migração e compatibilidade com a versão congelada deste ciclo | Válida para reestruturação futura |
| Reutilizar requisitos canônicos na orientação e antes das revisões | Pendência explícita, menor retrabalho e custo evitável | Esforço moderado; exige testes de contratos e navegação | Escolhida |

Não alterar oferta, preço, versão, público ou formato aprovados. Não consultar IA
para explicar uma pendência que pode ser verificada deterministicamente.

## Matriz definida antes dos testes

| Área | Casos e critério de aceite |
| --- | --- |
| Reprodução | Contexto original e identificadores novos: autorização aparece como decisão, não tarefa em execução |
| Preparação | Destino, criativo, checkout, instrumentação e público faltantes aparecem antes de iniciar revisões comerciais pagas |
| Caminho feliz | Dependências comerciais locais prontas → decisão explícita → orçamento atômico → fluxo oficial de publicação simulado → medição, entrega e decisão com provas |
| Limites | Teto ausente/zero, janela inválida/expirada, aprovação revogada, versão ou revisão divergente bloqueiam sem avançar |
| Concorrência | Reenvio idempotente, revisão antiga e decisões concorrentes preservam um único evento e a autorização exata |
| Retomada | Pausar sem tarefa real termina; tarefa real permanece protegida; retorno conserva cadeia, ciclo e experimento |
| Histórico | Ciclo anterior, custos, provas privadas e autorizações de vídeo preservados; não viram autorização de mídia |
| Integrações | Controller/service reais, MySQL 5.7 real e doubles explícitos para fontes externas; nenhum fornecedor pago |
| Observabilidade | Motivo, responsável, próxima ação e requisitos vêm do backend; leitura não grava nem dispara |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: links, formulário, confirmação desmarcada, loading, erros e sem overflow |
| Métricas | Fixtures locais identificadas; nenhuma campanha, cobrança, venda, receita ou avaliação produtiva criada |
| Regressão | Etapas de vídeo, canais fora do escopo, processos pausados/encerrados e gates finais preservados |

Após a última correção, duas rodadas locais completas e consecutivas devem passar.
Publicação e autorização comercial reais ficam fora da homologação. Emulação
Chromium não representa Safari nativo. Os resultados finais estão registrados abaixo.

## Reprodução adicional e decisão sobre a fila

Em MySQL local, o ciclo em `PUBLICATION` manteve o processo de vendas ativo e o
processo comercial iniciado ficou `QUEUED`: "Aguardando outro processo deste produto
encerrar ou ser pausado". A fila por produto selecionava sempre a raiz de menor ID,
inclusive quando essa raiz aguardava justamente o processo preparador. O teste
reproduziu o bloqueio antes de alterar a política de seleção.

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Pausar manualmente o processo pai antes de preparar | Usa comandos existentes | Mais intervenção, repetida a cada ciclo; pode esconder trabalho pendente | Não resolve recorrência |
| Transformar o retorno comercial em nova chamada formal no grafo | Dependência explícita e composição auditável | Exige nova versão de BPM, migração e adaptação de vínculos históricos | Válida para futura reestruturação |
| Permitir o preparador exato enquanto o pai aguarda sem tarefa real | Resolve a fila atual e as próximas sem trocar versão | Exige conferir identidade, estados e trabalho real em toda a árvore | Escolhida para esta recuperação |

O preparador continua passando pelos requisitos e pelo mecanismo existente de
deduplicação. A mudança não dispara tarefa por GET, não aprova revisores, não troca
versão de processo e não libera campanha. A referência do BPM comercial v6 nos
testes é extraída de `2026-08-28-agent-responsibility-matrix-v3.yaml`; os fornecedores
e os recibos externos são test doubles explicitamente identificados.

## Camada aperfeiçoada e avaliação

O impedimento pertence ao **harness do backend**: orientação, prontidão de entrada
dos revisores e seleção do trabalho. Psique e Têmis recebem a proteção antes de
criar tarefas. Plutus não é chamado para verificar requisitos determinísticos;
seus pareceres, custos e limites existentes continuam preservados. Não houve
troca de modelo, alteração de pesos ou nova chamada paga de revisão.

O diagnóstico usa trace de eventos, atribuição por camada e replay local conforme
`docs/canonical/aihub-aperfeicoamento-agentes-canon.v1.md`, seções **Conceitos do anexo
aplicados ao Marketing Hub** e **Critérios de conclusão**. Não foi adotada alegação
externa nem necessário alterar prompt operacional.
Os casos sintéticos adicionais avaliam outros IDs e entradas, mas foram usados
durante o ajuste: não são apresentados como held-out independente nem evidência
de ganho real em conversão, receita ou latência de fornecedor.

Durante a primeira rodada completa, o navegador antigo tentou preencher `summary`
antes de a nova etapa ser renderizada. O teste passou a aguardar a etapa retornada
pela API aparecer na tela, além de conferir explicitamente o resumo de autorização.
Não foi removida nenhuma verificação de aceite ou de evidência. A contagem das duas
rodadas completas foi reiniciada após essa correção do harness de homologação.

A regressão do coordenador isolado revelou ainda que uma dependência obrigatória
injetada em campo era resolvida até no bean simulado pelo teste. A dependência
comercial passou ao construtor do resolvedor de orientação; um teste de composição
Spring reproduz esse cenário sem carregar outros domínios. O contrato de produção
continua obrigatório.

A revisão da confirmação final encontrou uma contradição também presente na tela
consultada: o texto dizia **"sem criar campanha paga"**, enquanto o handler de
Facebook já chamava `releaseForFacebook`. A mensagem havia permanecido igual à do
canal direto depois da mudança do efeito operacional. Foram consideradas três
opções: resumo genérico, instrução complementar na tela do ciclo ou explicação por
canal na fonte backend da própria confirmação. A última foi escolhida por manter
benefício e baixo esforço da mudança textual, com menor risco de divergência entre
consumidores. Testes com ambos os canais e navegação confirmam a correspondência
entre o consentimento mostrado e o efeito do comando, sem conceder autorização.

A rodada `complete-1` passou unidades, REST e navegação, mas falhou na limpeza
anterior ao rollback: `fk_process_run_cycle` preservou corretamente um ciclo ainda
referenciado pelos novos registros da automação. O verificador local passou a
remover eventos e execuções sintéticas antes dos ciclos, sem desabilitar FKs,
alterar changelog ou apagar histórico produtivo. Esse defeito só apareceu porque
a matriz nova exercitou a automação junto dos ciclos. A contagem foi reiniciada
após essas últimas correções; rodadas incompletas não contam como homologação.

## Comparação com a versão anterior

| Critério | Antes | Candidata local |
| --- | --- | --- |
| Orientação do processo pai | `WAITING_ACTIVITY`, sem ação comercial identificada | Pendência de preparação ou decisão humana explícita, com link do mesmo ciclo |
| Formulário de autorização | Responsável, síntese, referência, versão, teto e confirmação preenchidos pelo operador | Responsável e confirmação; versão, teto, janela e evidência vêm do backend |
| Consentimento final por canal | O resumo Meta dizia "sem criar campanha paga", embora o comando liberasse publicação | Meta explicita publicação e gasto até o teto; o canal direto conserva sua operação sem mídia |
| Revisões sem insumos | O processo permitia solicitar os revisores e descobria a falta no gate final | A mesma fonte canônica bloqueia a entrada antes de criar tarefas |
| Retorno ao processo comercial | A raiz anterior retinha a fila enquanto aguardava sua dependência | O processo comercial da mesma ocorrência avança; tarefas reais, pausas e outros contextos continuam protegidos |
| Custo e resultados | Custo produtivo dessa espera não informado; nenhuma autorização de mídia | Zero chamada paga na avaliação; custos dos revisores e recibos comerciais dos testes são sintéticos |

Não houve medição comparativa de latência produtiva, conversão ou economia em
reais. Reduzir o formulário e impedir tarefas inválidas são resultados funcionais;
aumento de receita e redução de custo operacional ainda são hipóteses a medir.

## Limite da preparação e situação produtiva

A inspeção adicional do executor de integração confirmou que ele **valida** os
vínculos comerciais; não produz por si só a publicação do destino, o criativo e o
público. O percurso já existente usa a configuração do experimento e a publicação
versionada, como registrado em `vega-ciclo2-ativacao-comercial-v12.md`, seção
**Estado esperado após integração e publicação pelo fluxo oficial**. A correção
da fila libera o processo de homologação responsável, sem criar outra cadeia nem
executar antecipadamente a publicação comercial.

A homologação desta alteração simula explicitamente os insumos já preparados e
as respostas externas. Ela comprova sua validação, orientação, autorização e
continuidade, mas não comprova que a superfície produtiva de Vega esteja publicada.
Não foram criados ativos no Marketing Hub por SQL ou endpoints alternativos.
O estado produtivo consultado continua `OPEN/AUTHORIZATION` no ciclo 2 e `PLANNED`
no experimento 92, sem autorização de mídia. Os vídeos 41/42 permanecem aprovados.
A janela persistida termina em **16/09/2026 às 23:59 no horário de Brasília**
(`2026-09-17T02:59:00Z`); não foi prorrogada automaticamente.

## Aprendizados reutilizáveis e melhoria comercial

- Verificar entradas com a mesma fonte canônica antes de contratar avaliação;
  ausência de entrada não é uma falha de inteligência do agente.
- Separar espera humana de trabalho em execução e preservar referências de ida
  e volta ao processo; a fila não pode reter a dependência que seu primeiro item
  precisa concluir.
- Reaproveitar dados auditados no formulário, mantendo a confirmação humana
  explícita e a validação transacional contra alterações e reenvios.
- Após publicação e autorização, medir **primeiro resultado útil → checkout →
  venda líquida entregue**, junto da contribuição por pedido. Prioridade comercial
  alta e esforço adicional baixo por aproveitar a instrumentação existente;
  trata-se de hipótese de melhoria, sem vendas atribuídas a esta correção.

## Resultado das rodadas finais

Revisão-base: `8f1cea19b74c2b4cd93d0b549ad12f37a0007547`. Os arquivos alterados de
aplicação e testes, sem commit, são identificados pelo SHA-256 de conteúdo
`e07b7b19e93a43b19f9d3bf7e4d21a2fff89c1391eea870f0c9764559da11217`.

- **final-1 e final-2: aprovadas consecutivamente, sem alteração de código entre elas.**
  Por rodada, backend: 3.093 casos, zero falhas/erros, nove opcionais
  ignorados (3.084 executados com sucesso). Frontend: 678 testes aprovados,
  TypeScript e build. Atena: 33 testes; worker do coordenador: seis testes.
- MySQL 5.7: integração, concorrência, rollback, reaplicação e idempotência
  aprovados. Os cenários REST incluem seis grupos da decisão, 20 dos ciclos,
  sete comerciais, 18 do coordenador, nove do ciclo de vida e reinício persistido.
- Navegação aprovada em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, com
  21 casos comerciais específicos e os percursos da cadeia, legado, decisão,
  venda/entrega e coordenador. A execução usa banco novo por rodada.
- A segunda rodada repetiu todos os grupos, incluindo reinício, pausa/retomada,
  preservação de tarefas, retorno ao processo e navegadores do coordenador.
- A topologia temporária foi removida com volumes e órfãos. A conferência final
  não encontrou containers ou volumes do projeto Compose exclusivo da sessão.

Logs e capturas locais: `artifacts/cycle-commercial/final-{1,2}` e
`artifacts/cycle-commercial/final-{1,2}-automation`. Contagens, grupos, hash da
revisão e fotografia produtiva estão na
[evidência estruturada](evidencias/vega-ciclo2-espera-comercial-v1.json).
As chamadas pagas e as escritas
produtivas permaneceram fora da execução. O número de testes não representa
vendas, margem ou satisfação de clientes reais.

A consulta final via MCP reconfirmou o ciclo 2 em `OPEN/AUTHORIZATION`, revisão 13,
e o experimento 92 em `PLANNED`, com checkout e limite operacional ainda ausentes,
criativo não aprovado e sem solicitação de liberação Facebook. O predecessor 91
permanece `USER_STOPPED`. A implementação local está concluída; a retomada
produtiva aguarda PR e publicação pelo fluxo autorizado, preparação dos vínculos
comerciais na tela e a decisão humana real de mídia. Nenhuma atividade foi
encerrada artificialmente para representar vendas ou entregas ainda não ocorridas.
