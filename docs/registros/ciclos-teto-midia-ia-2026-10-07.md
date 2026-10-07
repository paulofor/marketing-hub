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
