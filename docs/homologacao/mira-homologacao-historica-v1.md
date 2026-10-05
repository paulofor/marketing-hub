# Homologação técnica histórica de Mira

Consulta em 05/10/2026: Mira #10, tipo cadastrado Safira (`AI_PRODUCT`),
cadeia #26, processo #122/v7, execução #41 e referência `experiment:93`.
O plano comercial vinculado é #8 e o experimento está `INVALIDATED`, com
janela encerrada em 30/09. A execução está pausada; não há nova autorização.

## Causa confirmada e escolha

A tela e sua API mostram 2/4 e apresentam `surfaces` como não iniciada.
O MCP confirma as instâncias #480/#481/#482 concluídas na mesma definição,
com provas do run produtivo #14 (número 2). A consulta de vigência registra
falha para #480 e oculta essa conclusão ao comparar a superfície histórica
com o contrato atual. Não existe instância concluída de `financialGuardrails`.
O cabeçalho pausado também conserva contagem e atividade anteriores.

| Alternativa | Benefício | Risco/custo | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Repetir homologação/Plutus | Verificação nova | Reabre tentativa encerrada, consome e altera o histórico | Alto | Não |
| Corrigir somente o aviso | Melhora a leitura | Backend e contexto AIHUB continuam contraditórios | Baixo | Não |
| Preservar provas encerradas na fonte e atualizar a consulta do controle | Relatório fiel, sem novas tarefas | Exige distinguir aceite histórico de prontidão atual | Médio | Sim |

## Matriz definida antes dos testes

| Área | Critério de aceite |
| --- | --- |
| 5.4.1 Superfícies | Preservar somente a conclusão persistida da definição/referência; mudança atual não apaga a prova encerrada |
| 5.4.2 Compra/acesso/entrega | Preservar prova de QA e sua segregação, sem executar compra ou entrega nova |
| 5.4.3 Mensuração | Preservar prova de atribuição/deduplicação; teste não conta como venda |
| 5.4.4 Limites financeiros | Prova ausente permanece pendente; não renovar Plutus nem fabricar aceite |
| Candidata aberta | Mudança de fingerprint/run e falta de fontes continuam exigindo revalidação |
| Identidade e falhas | Referência inválida ou de outro produto não preserva uma conclusão como prova atual; nenhuma exceção por ID |
| Controle pausado/encerrado | Consulta mostra progresso e atividade do relatório atual, sem alterar pausa, diário, custos ou disparar tarefas |
| Retorno ao pai | Filho parcial não conclui a homologação no pai; links mantêm produto/cadeia/referência |
| Persistência e recuperação | Leitura sem lock de escrita, sem nova ocorrência; comandos encerrados recusados e repetição idempotente |
| Interface e contexto AIHUB | Bundle local com contratos reais em desktop, iPhone 15 Pro e Pixel 7; 3/4, superfícies concluídas e nenhuma retomada indevida |
| Integração e empacotamento | Unitários backend, HTTP, JPA, formatação e JAR com classes correspondentes às testadas |
| Isolamento e observabilidade | Identidades sintéticas locais; histórico/custo preservados; nenhum modelo pago, campanha ou evento comercial de teste em produção |

O harness tinha caso encerrado apenas com fontes ainda vigentes. A regressão
passa a combinar encerramento e mudança da superfície, reproduzindo o caso real,
e liga o relatório das atividades ao controle pausado. A camada corrigida é a
verificação backend; não se modifica prompt ou modelo de Psique/Plutus.

Os cinco pontos da oferta permanecem nos contratos históricos: desejo/prova e
facilidade em superfícies, compra/continuidade em transação, atribuição em medição,
rentabilidade em limites financeiros. Esta correção não altera oferta, preço,
versão, hipótese ou mídia; não comprova vendas nem lucro.

## Resultado local

- Antes da correção, as duas regressões do caso encerrado com superfície alterada
  falharam: o HTTP mostrava 2/4 e a leitura JPA exigia nova execução. Depois,
  mostram 3/4 e preservam integralmente a prova, sem escrita nem chamada paga.
- Backend completo: 3.969 casos, 3.944 executados, zero falhas/erros e 25 casos
  opcionais ignorados pela configuração padrão. Os seis testes de persistência
  técnica também passaram em MySQL 5.7 real e segregado; o fluxo HTTP tem oito
  casos aprovados, incluindo os contratos reais de run/preflight exportados.
- Coordenador real com MySQL 5.7: 19 cenários HTTP e 22 cenários de ciclo de vida
  aprovados. O reinício preservou conclusões, encerramentos, tarefas e deduplicação.
  Filho parcial não cria conclusão nem retorno artificial ao pai.
- Frontend: 47 testes pertinentes, typecheck e build aprovados. O bundle com
  respostas reais dos controllers passou em desktop, iPhone 15 Pro e Pixel 7:
  3/4, `surfaces` concluída, limites pendentes, contexto AIHUB consistente e
  ausência de comandos para criar run, renovar ou retomar a referência encerrada.
- Spotless dos arquivos alterados, sintaxe JavaScript, `bash -n`, ShellCheck dos
  runners consultados e revisão do diff aprovados. Empacotamento: 4.213 classes
  idênticas às compiladas/testadas, 747 recursos íntegros e inicialização dos
  513 cartões do catálogo no JAR executável.

O harness visual agora usa também o run/preflight exportado pelo backend,
evitando um mock vazio que mostrava "Criar run" e escondia a proteção real.
Não há mudança de prompt, agente ou consumo. O ganho comprovado é a fidelidade
do relatório e a prevenção de repetição indevida; vendas, margem realizada e
custo por nova homologação não foram medidos nesta correção.

A publicação e a conferência do histórico produtivo serão vinculadas no PR
desta entrega. Até essa conferência, os resultados acima são somente locais.
