# Limites de mídia e IA nos ciclos — 07/10/2026

## Evidência e causa

No ciclo 9 de Mira, a tela mostrava “Teto total: R$ 0” durante a preparação
autorizada pelo usuário até US$ 10 de IA. O contrato `budgetLimitBrl`, o gate
`CYCLE_MEDIA_LIMIT` e o formulário de criação já tratavam esse valor como mídia.
O cabeçalho e outras mensagens omitiam o tipo de gasto, causando ambiguidade.
Não havia ausência da autorização de IA nem autorização de anúncios.

## Escolha e alcance

Esclarecer os rótulos existentes resolve a causa para todos os produtos, sem
alterar contratos ou permissões. Criar outro controle financeiro ampliaria o
escopo; manter o rótulo preservaria a confusão. O cânone de ciclos agora explicita
que mídia em reais, IA e produção de vídeos têm autorizações próprias. Nenhuma
autorização é inferida, convertida ou renovada por essa mudança de apresentação.

## Validação local

Os três testes existentes de página, comandos e janela passaram: 33 casos.
TypeScript, build, Prettier e revisão do diff passaram. A navegação real em
Chromium desktop, iPhone 15 Pro e Pixel 7 usou APIs simuladas e identificadores
diferentes de produção, preservando o ciclo histórico e a preparação sem gasto.
Os casos cobriram mídia zero e mídia histórica positiva, clareza do rótulo,
ausência de overflow e payload de preparação sem autorização implícita de IA.
Safari nativo não foi testado. Não houve escrita externa nessa validação.

O primeiro comando de testes omitiu `NODE_ENV=test` e foi corrigido para o
`npm test` já definido no projeto; não exigiu alteração de implementação.
Esse ajuste não modifica a candidata privada nem renova pareceres históricos.

## 09/10/2026 — decisão audiovisual ausente no resumo do ciclo

Mira, produto 10/ciclo 9/experimento 102, chegou a `VIDEO_BRIEF` após o Processo 4
concluído. Tela, API e MCP confirmaram `videoBudget=null`, mídia zero, nenhuma janela
e nenhuma tarefa ativa. O financeiro corretamente informava `AWAITING_LIMIT`, mas
`valueFlow.decisionNeeded=false` dizia que não havia decisão. O resumo novo tratava
somente implementação, pausa e autorização de mídia; faltava consultar o ledger
audiovisual já usado pelo financeiro e pela orientação do processo pai.

Foram comparados corrigir o registro de Mira, duplicar autorização no resumo e
reutilizar `LearningCycleVideoBudget.current`. A terceira opção preserva identidade,
histórico e ausência de efeitos externos, com esforço pequeno e cobertura entre
produtos. A projeção usa a autorização da versão e experimento exatos. Sem teto,
expõe a decisão, unidade, escopo e passagem para Íris/Plutus/Apolo; com teto, orienta
o briefing sem afirmar produção em curso. Pausa e trabalho delegado preservam
precedência, e ciclos encerrados não recebem decisão nova.

Matriz definida antes da validação:

| Caso | Aceite |
| --- | --- |
| Mira sem teto e produto independente | Decisão necessária, sem produção, orçamento inventado ou métrica comercial |
| Teto vigente | Reutilizar autorização; orientar briefing e avaliação, sem nova pergunta ou consumo |
| Teto de outra versão/experimento | Manter decisão; não herdar autorização |
| Ciclo encerrado, pausa e implementação | Preservar histórico e orientações anteriormente válidas |
| Integração com financeiro | Usar validador real do recibo; leitura nunca grava evento |
| Tela em desktop, iPhone e Pixel | Resumo e financeiro consistentes; dados de teste locais, sem chamadas pagas |

Hipótese de melhoria reutilizável: reduzir retornos ao AIHUB causados por decisões
ocultas. Métricas posteriores: recorrência desse conflito e tempo até decisão
registrada; sem medição comercial, não afirmar aumento de conversão ou vendas.
A mudança não libera a campanha: vídeos, checkout, anúncio, público, homologação,
teto de mídia e janela conservam os contratos e autorizações próprios.

Validação local: 76 testes de projeção, financeiro e orientação do processo passaram;
18 testes de interface passaram. O frontend local consumiu a projeção Java real de
uma fixture com IDs distintos de produção: Chromium desktop, iPhone 15 Pro e Pixel 7
confirmaram decisão visível, navegação ao financeiro, campo vazio e nenhuma escrita.
Safari nativo não foi executado. Spotless e revisão do diff concluídos; o helper
existente também passou em `bash -n` e ShellCheck. A checagem inicial de Spotless foi
reexecutada pelo helper do repositório, pois o parâmetro aceita expressão regular,
não lista de arquivos nem glob. Nenhum script precisou ser alterado.

Preparação operacional independente: o projeto de vídeo #10 foi salvo pela tela
como `DRAFT`, vinculado somente ao produto 10/experimento 102 e ao briefing #634 de
Íris. MCP confirmou ausência de perfil executor, ciclo de produção e nova tarefa
de IA. Não foi criado orçamento, vídeo final, aceite comercial ou campanha. A decisão
sobre teto audiovisual, mídia e duração permanece com o usuário.
