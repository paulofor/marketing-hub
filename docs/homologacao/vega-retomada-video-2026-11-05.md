# Vega — retomada privada do Estúdio em 05/11/2026

## Contexto conferido

Produto 4, projeto 14, perfil 69, cadeia 27, ciclo comercial 21 e experimento 114.
O aceite continua sendo um MP4 privado de 32 segundos/oito cortes, com voz,
legendas e revisões independentes do mesmo arquivo. Storyboard não é vídeo.
O recibo 128 representa US$ 15 novos totais; não autoriza o histórico, outro
vídeo, recarga, campanha ou publicação comercial.

Às 20:33 UTC, o MCP confirmou backend no SHA
`01720b5c789b1ceb16f75e9fd429d25da5c57d54`, do PR 5792. UI e banco confirmaram
perfil DRAFT, nenhum job para 69 e ciclos 42–46 exclusivamente de preflight.
O PR 5793 permanece draft, sem revisão e com conflito no cânone; sua base e
alterações independentes devem ser preservadas. Nenhuma geração paga ocorre
nos testes desta entrega.

## Escolhas de implementação

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Consultar o ciclo no navegador e enviar seu contrato ao endpoint existente | Reutiliza o render e seus gates | Mais leituras, acoplamento financeiro na UI e possibilidade de estado desatualizado | Não adotada |
| Usar um comando de projeto com contexto persistido no servidor | Reutiliza decisões, reserva e job com lock e menor alteração | Exige regressões de UI/backend | Adotada |
| Encadear automaticamente a produção no aceite do processo 4.2 | Reduz a operação manual futura | Amplia a mudança de BPM e exige homologação de todos os gates e responsáveis | Preservada para demanda própria |

Para recuperação foram comparadas três opções: consultar somente a task e baixar
seu resultado ainda disponível (menor persistência, mas dependente da validade da
URL); preservar imediatamente os bytes no storage existente e seu recibo/hash
(adotada, resistente ao reinício e à expiração); ou separar toda a montagem em
um novo tipo de job referenciando os clipes (isolamento maior, mas mudança mais
ampla no pipeline). Repetir geração não é recuperação. Timeout de envio sem ID
conhecido permanece incerto e bloqueia novo POST; timeout de polling consulta a
mesma task.

## Matriz definida antes da execução

| Área | Critério de aceite local |
| --- | --- |
| UI e backend | Comando Runway usa ciclo canônico; contexto elegível cria/reutiliza um único job; outro tenant/projeto/produto/perfil é recusado |
| Bootstrap financeiro | Registrar as sete parcelas e fontes pela UI, reavaliar sem resetar teto ou criar outro ciclo; ausência, vencimento e divergência bloqueiam |
| Teto compartilhado | Recibo, custos, pendências e reservas são consultados no servidor antes do consumo; não presumir que metadata declarada implementa limite |
| Recuperação | Falha/reinício depois dos clipes reutiliza IDs/bytes/hash sem POST pago; timeout com resultado incerto preserva reservas e exige conciliação |
| Mídia | Montagem sintética preserva 32s e streams; nenhuma alegação de qualidade premium baseada em fixture |
| Regressões | Caso original, outro produto/contexto e caminho antes válido; repetição e concorrência sem duplicação |
| Observabilidade | IDs, decisões, erros, custos e recibos persistidos; fonte desconhecida não vira zero |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 com doubles locais, sem mídia paga |
| Entrega | Diff e testes locais antes de commit; PR 5793 reutilizado; revisão independente real, checks do HEAD e deploys/saúde comprovados antes da operação |

## Aprendizado e limites

A lacuna de harness observada é a validação do planner isolado sem exercitar o
payload do botão real, o bootstrap financeiro e a perda de estado entre render
e montagem. As regressões desta entrega precisam comprovar esses percursos.
Métrica verificável: ausência de POST pago duplicado e preservação de contexto,
arquivo e teto. Ganho comercial ainda não foi medido.

## Resultado local

O botão real passou a chamar o comando do projeto sem inventar metadata
financeira. O servidor valida tenant, produto, experimento, perfil e decisão
persistida e reutiliza o mesmo job sob lock. Os testes cobrem o contexto original,
outra identidade e caminhos anteriormente válidos. O formulário de custos registra
as sete parcelas e suas evidências antes de Plutus, no mesmo ciclo e teto.

O teste de recuperação usa quatro tomadas válidas de 10/10/10/2 segundos para
montar 32 segundos; tomadas e oito cortes editoriais são contratos diferentes.
Uma falha real de FFmpeg ocorre depois dos uploads. Outra instância remonta os
mesmos bytes por task, asset, URL e SHA-256: continuam exatamente quatro POSTs,
quatro polls e quatro uploads. Hash ou contexto divergente é recusado. O teste
é parametrizado com jobs diferentes e não avalia qualidade premium da candidata.
Outra regressão comprova que o processor reutiliza o plano do backend sem chamar
novamente a IA. Timeout HTTP 408 permanece incerto após reinício e não repete
POST; timeout de polling consulta a task aceita e conclui sem nova submissão.

A revisão do replay identificou que um novo aviso de aceite podia retirar uma
cobrança já liquidada do ledger. O registrador compartilhado agora é idempotente:
preserva data, contexto e custo da primeira task e recusa outro contexto ou valor.
Clipes já preservados não repetem eventos financeiros. As regressões cobrem dois
jobs, repetição e divergência sem alterar a conciliação original.

A suíte ampla de backend terminou com 5.873 casos registrados, zero falhas/erros
e 66 casos condicionais ignorados. O teste específico de migração/aplicação/
reaplicação do contrato foi executado separadamente com MySQL 5.7 real e passou;
a topologia temporária foi removida. No executor, os relatórios combinados somam
318 casos, zero falhas/erros e quatro testes condicionais dependentes de mídias
externas ignorados; os cenários locais opcionais de FFmpeg foram executados.
No frontend passaram 78 testes focados, verificação de tipos e build. O navegador
passou seis percursos na UI real/controller real em desktop, iPhone 15 Pro e Pixel 7,
com persistência e executores explicitamente simulados. Não houve geração paga.

Uma primeira invocação ampla de Maven usou execução no mesmo processo a partir
da raiz e excedeu a memória do cgroup, além de alterar a base dos caminhos relativos
das fixtures. A execução complementar preservou relatórios válidos, usou o fork
normal no diretório do módulo e cache de quatro contextos Spring. A única falha
funcional encontrada foi uma dependência direta entre módulos: ela foi corrigida
por uma porta obrigatória de política financeira do domínio, sem ampliar a regra
ArchUnit. A regressão arquitetural passou com 93 regras. A nova regressão do
formulário financeiro foi incluída no job existente de CI do Estúdio; os três
workflows alterados passaram no Actionlint.

O pacote real de evidências comerciais e seus testes passaram. Uma sucessora v77
reconcilia a base v75/main e v76/PR sem alterar atestações antigas; o resolvedor
continua indicando nenhuma republicação do frontend PDE. Scripts shell alterados
ou investigados passaram em `bash -n` e ShellCheck. O diff não tem erros de espaços;
classes e métodos Java alterados têm comentários de responsabilidade em português.

## Limite de produção ainda não resolvido

O recibo 128 continua válido como autorização de US$ 15 novos totais. Entretanto,
`FinancialCodexRunner.reviewVideoCycle` executa o subprocesso Codex e lê o uso de
tokens depois que ele termina; `AgentTaskService` também aplica o custo depois
da resposta. O wrapper OAuth protege sessão, não dólares. A reserva integral
do Estúdio não limita essas inferências nem as revisões futuras. Não foi encontrada
prova de um limite prévio compartilhado em todos os executores envolvidos.

Por isso, o backend persiste `SINGLE_VIDEO_SHARED_LIMIT_NOT_ENFORCED` e impede
iniciar Plutus ou o render direto para recibos desse escopo. Essa contenção foi
testada com identidades diferentes e não depende de configuração ou metadata
enviada pelo navegador. Ela não equivale a implementar o executor limitado:
o caminho pago e o aceite final de um vídeo real continuam pendentes.

Não há prova de insuficiência de saldo da conta inteira. US$ 3,84/4,80 do preflight
46 são um snapshot parcial vencido. Nenhum zero sintético ou custo histórico foi
tratado como saldo disponível do novo vídeo. Em nova leitura MCP, perfil 69 segue
DRAFT e ciclos 42–46 seguem sem tarefa ou job. Nenhuma inferência, geração ou
revisão paga foi disparada nesta continuação.

Também não há revisão independente do PR 5793: a identidade autenticada é a autora
`paulofor`, única colaboradora listada no repositório. Não é permitido usar sua
própria aprovação como revisão independente. O merge, deploy e qualquer produção
dependente permanecem sem execução enquanto não houver revisão válida e controle
de consumo efetivamente comprovado. Não prometer retomada automática sem mecanismo
ativo e elegível. Adotar as correções técnicas somente após os checks e essa
revisão; não declarar prontidão premium, venda ou ganho comercial a partir dos testes.
