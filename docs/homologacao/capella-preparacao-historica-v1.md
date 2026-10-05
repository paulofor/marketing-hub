# Preparação Quartzo em referência encerrada

Consulta em 05/10/2026: Capella #7, Quartzo (`LOW_TICKET_DIGITAL_PRODUCT`),
processo #81/v1, cadeia #26, execução #40 e referência `experiment:94`.
Plano comercial #2; o backend não vinculou ciclo nem ficha de execução a essa referência.
O experimento está `PLANNED`, mas sua janela terminou em 02/10.
Não substituir essa identidade por #88 ou pela expansão #96.

## Evidência e escolha

Tela, API, MCP e logs: #40 apresenta 4/8 e pede renovar Plutus #62,
plano financeiro #9/revisão 5. No banco, #33/cadeia #23 concluiu as oito atividades
em 29/09; ocorrências #449/#450/#444/#445/#446/#447/#451/#452 e pareceres aprovados
Psique #563 e Têmis #564 estão preservados. O bloqueio compara provas históricas
com a validade financeira atual, sem proteger Quartzo pela janela do experimento.

| Alternativa | Benefício | Risco/custo | Esforço | Decisão |
| --- | --- | --- | --- | --- |
| Renovar economia e pareceres | Nova verificação | Consumo e reabertura de referência vencida | Alto | Não |
| Pausar só #40 | Contém a espera | Oculta conclusão e permite recorrência | Baixo | Não |
| Preservar prova e bloquear nova tarefa na fonte | Relatório fiel e proteção reutilizável | Exige distinguir conclusão passada de autorização atual | Médio | Escolhida |

## Matriz de homologação antes dos testes

| Área | Critério de aceite |
| --- | --- |
| 5.1.1–5.1.4: página, criativo, checkout/entrega e público | Reconhecer somente ocorrências concluídas da definição e referência consultadas |
| 5.1.5: economia | Preservar a conclusão histórica sem renovar Plutus; candidata aberta exige fontes, validade e margem |
| 5.1.6–5.1.7: Psique e Têmis | Preservar aprovação persistida e impedir nova tarefa antes de chamada paga; ausência nunca vira sucesso |
| 5.1.8: conclusão e retorno | Controle reconhece 8/8; repetição não cria tarefa, custo, evento ou novo retorno ao pai |
| Estados e identidade | Janela passada mesmo em PLANNED, estado terminal ou ciclo fechado bloqueiam mutações; outro produto/ciclo/experimento é recusado |
| Recuperação e concorrência | Retomada e lock preservados; prova ausente mantém objetivo pendente e encerramento sem sucesso |
| Fontes e observabilidade | Consulta histórica independe da publicação/economia atual; provas, tarefas e custo parcial ficam auditáveis |
| Interface | Bundle local com contratos produzidos pelo controller real em desktop, iPhone 15 Pro e Pixel 7; histórico legível, sem ações indevidas ou erro JavaScript |
| Integração e empacotamento | Unitários backend, regressões HTTP/persistência, formato e JAR; classes empacotadas correspondem à versão testada |
| Isolamento e métricas | Identidades sintéticas locais; nenhuma gravação QA em produção, modelo pago, campanha ou compra real |

Os cinco critérios da oferta permanecem nas provas: desejo e demonstração na página,
facilidade e uso em Psique, continuidade/condições em checkout e Têmis, economia em Plutus.
A repetição comercial depende de sucessor com hipótese e limites próprios.
Preparação não comprova venda, satisfação real, receita ou lucro.

## Resultados locais

- Regressão original HTTP: 4/8 na versão anterior; controller e gates reais agora
  retornam 8/8 e recusam os oito comandos. Sem a prova final ou com a economia não
  comprovada, o resultado permanece 7/8. Outro produto é recusado; candidata aberta
  mantém fingerprint e fontes vigentes.
- O mesmo contrato HTTP alimenta o motor real e sua API de conciliação: COMPLETED,
  8/8, mesma referência, nenhuma nova tarefa, um evento terminal na repetição e
  custo desconhecido mantido como desconhecido. A persistência JPA independente
  comprova as variantes de cinco e oito objetivos e preserva custo conhecido.
- Suíte completa backend: 3.963 casos, nenhuma falha/erro, 25 condicionais/desabilitados
  preexistentes. Após ampliar a composição, repetidas apenas as regressões pertinentes;
  ajustes nas fixtures respeitaram o construtor público, o encapsulamento do grafo
  e a cobertura NO_EXECUTIONS, sem modificar regras produtivas para passar.
- Os 45 testes dos painéis, tipos e build frontend passaram. O build backend contém
  4.213 classes idênticas às testadas e 747 recursos íntegros; nove testes do verificador
  passaram e o JAR inicializou 513 cartões do catálogo de comportamento dos agentes.
- MySQL 5.7 local: 19 cenários HTTP e 22 de ciclo de vida, com zero falhas e nenhuma
  escrita produtiva. Formatação, sintaxe e ShellCheck do runner consultado passaram.

As capturas e a verificação publicada serão vinculadas ao PR. Nenhuma chamada paga,
prorrogação de janela, aprovação retroativa ou campanha foi executada nesta validação.

## Evidência de navegador

Bundle local e contratos gerados pelo controller/motor reais passaram em desktop,
iPhone 15 Pro e Pixel 7, sem erro JavaScript, transbordamento ou mutação de API.
SHA-256 das capturas:

- desktop: `1747e346b977636f964318c37037fdc03ec7b668a3df9cc292d3179e25fd2365`.
- iphone: `276b83a54ad21e218da1bd1c332f60c1b855c0a414dda86649708080e8a2a5c6`.
- pixel: `6342d4087fc748450cd6ae948ed341ee7f8f5eb2d07301b5730e2253c4a32059`.
