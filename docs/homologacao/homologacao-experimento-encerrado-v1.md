# Homologação de referência encerrada

Data: 04/10/2026. Capella #7, tipo `LOW_TICKET_DIGITAL_PRODUCT`, cadeia #26,
processo #122/v7, execução #39, `experiment:88`. Pai definido #118/v12, atividade
`preflight`, sem instância pai nem ciclo vinculados à execução #39.

## Evidência e causa

Tela, endpoints e MCP em 04/10/2026 confirmaram `INVALIDATED`, janela 25–29/09,
execução automática `WAITING_INPUT` com 3/4 provas (#483–#485, run #17), sem tarefas.
A homologação anterior #17/processo #58 completou 4/4 em 23/09; o run #12 encerrou
em 25/09. Novas tentativas #16 e #17 foram criadas depois do encerramento.
O log de `ExperimentTechnicalPreflightActivityExecutor` registrou a espera financeira.
A revisão #9/revisão 5 e parecer #62 venceram; esse fato é real, mas não é o primeiro
impedimento. O motor valida publicação/ciclo e o preflight valida provas/economia,
sem conferir o encerramento do experimento da referência quando não há ciclo.

## Escolha

| Alternativa | Benefício | Risco | Esforço | Escolha |
| --- | --- | --- | --- | --- |
| Renovar Plutus | Atualiza economia | Consumo inútil e tentativa histórica indevidamente prolongada | Médio | Não |
| Pausar só #39 | Libera a fila | Outras referências repetem o defeito | Baixo | Não |
| Conferir ciclo de vida no contrato compartilhado | Fecha a causa e preserva prova histórica | Exige regressões de retomada | Médio | Sim |

A proteção cobre homologação técnica e seu pai; não impede operação de entrega,
conciliação ou aprendizado de resultados históricos. Não altera oferta, preço,
janela, orçamento ou status comercial do experimento. Encerrar a execução automática
não comprova a atividade financeira nem autoriza retorno de sucesso ao pai.

## Matriz definida antes dos testes

| Área | Critério |
| --- | --- |
| Caminho válido | Experimento planejado ou pausado em janela válida conserva homologação e economia obrigatória |
| Encerramento | INVALIDATED/VALIDATED/INCONCLUSIVE/FINISHED/FAILED ou data final passada bloqueiam antes de nova tentativa, alteração de gate ou Plutus |
| Datas | Último dia UTC permanece válido; ausência de data não equivale a encerramento nem dispensa os gates financeiros |
| Identidade | Mesmo comportamento para IDs diferentes; produto divergente bloqueia; aprendizado permanece disponível |
| Histórico | Consultas preservam gates, custos e três instâncias concluídas; quarta continua não comprovada |
| Motor | Conciliação fecha espera sem disparo; preserva retorno de tarefas iniciadas; repetição não duplica evento nem conclusão do pai |
| API | Leitura expõe impedimento; POST de criação/preflight/renovação/homologação recusa referência encerrada antes de escrita |
| Interface | Resposta backend governa comandos e mensagem em desktop, iPhone e Pixel; histórico continua consultável |
| Observabilidade | Motivo persistido e evento CONTEXT_CLOSED; nenhum consumo ou receita sintéticos na produção |
| Empacotamento | Unitários backend, contratos/frontend e build local; revisão do diff antes do PR |

Cinco pontos da oferta preservados: desejo/prova/condições permanecem na versão
contratada; facilidade e compra/entrega nas provas existentes; repetição rentável
exige novo ciclo com limites próprios. Esta correção não cria oferta nem comprova
vendas, lucro ou aumento de conversão. Custo/latência de homologação paga: não medidos
nesta entrega, pois não houve chamada de modelo. Casos independentes são sintéticos.


## Verificações locais

Rodada completa backend: 623 classes de teste, com 25 testes previamente desabilitados.
As duas falhas da primeira rodada foram uma configuração de mock do novo teste de
persistência e a fixture histórica que permitia renovar `INVALIDATED`. Corrigidas;
o teste concorrente agora cobre `PAUSED` (uma renovação) e `INVALIDATED` (nenhuma).
Após corrigir a apresentação do resumo, os testes de processos/preflight passaram.
Resultado consolidado: 3.940 testes, 3.915 aprovados, 25 já desabilitados e nenhuma falha.
A suíte frontend completa passou (866 testes), assim como tipos/build e 57 regressões
focadas após o ajuste do resumo. O motor persistiu CLOSED com 3/4 provas e um único
evento, sem tarefa nova nem retorno de sucesso ao pai. HTTP recusou os quatro comandos
com 409 sem mutação, inclusive quando a referência encerrada ainda não possui gates.
Esse caso falhou antes do ajuste da ordem de validação, orientando indevidamente a
executar preflight, e foi incluído na regressão HTTP. Os demais gates técnicos e
financeiros continuam obrigatórios.

O bundle local, com respostas produzidas pelos testes HTTP e de persistência,
passou em desktop, iPhone 15 Pro e Pixel 7: sem erro JavaScript, transbordamento
horizontal ou comando de escrita; resumo e controle indicam encerramento com 3/4.
O empacotamento comprovou 4.212 classes idênticas às testadas, 747 recursos íntegros
e inicialização de 513 cartões do catálogo no JAR executável.

SHA-256 das capturas locais:

- Desktop: `70d8ead46e3719fdf1dc6ed3e53d944a2a089beefbc2c3df9fa22997fbd2fc7a`.
- iPhone 15 Pro: `79c19ae8228352b02f4959bf13ddceeb4273dfb1a1eb66d1aa3248602ada7f19`.
- Pixel 7: `4b12054d6b10e9b42af3b8d6d6d5871814430072282f36ac664edc4443fc4272`.

A consulta da ficha oficial `/api/products/7/execution-profiles/v1` retornou lista vazia.
Não foi criada ficha nem deduzido outro formato. O contrato cadastrado Quartzo e os
vínculos existentes foram preservados. Produção só será considerada confirmada após
PR/merge, workflows e conferência do estado persistido/tela.

## Inicialização da fixture MySQL

O run de CI `37237551461` detectou a ausência de `ExperimentRepository` no contexto
isolado `ProcessAutomationLocalApplication`. A aplicação real fornece esse bean;
a fixture exclui a descoberta JPA e precisa declarar suas dependências simuladas.
O teste rápido `ProcessAutomationFixtureWiringTest` reproduziu localmente a mesma
`UnsatisfiedDependencyException` antes da correção e inicializou depois dela.

Foram comparadas três opções: tornar a dependência produtiva opcional enfraqueceria
o gate; simular todo o contexto retiraria o coordenador real da homologação;
registrar o repositório na fixture e testar sua inicialização preserva ambos os
contratos com ajuste pequeno. Essa última opção foi adotada. A verificação usa os
métodos de beans da própria fixture e a resolução de construtor real do Spring,
para que novas dependências ausentes falhem antes da rodada MySQL completa.

Rodada local `capella39` aprovada em Node 22.23.3 e MySQL 5.7: unitários completos,
worker, tipos/build, contratos, imagem temporária, persistência, 19 cenários HTTP,
ciclo de vida, concorrência, reinício e quatro matrizes de navegador em desktop,
iPhone e Pixel. Os cenários incluem versões retiradas e reservas projetadas sem
chamada paga. A sandbox precisou instalar `rsync`; as etapas já aprovadas foram
preservadas e a execução continuou da primeira etapa pendente. Não houve alteração
dos scripts do runner. `bash -n` e ShellCheck passaram nos scripts examinados.
Containers, rede e volumes temporários do projeto exclusivo foram removidos.
