# Limites claros na demonstração financeira — 31/10/2026

## Evidência e decisão

Capella, ciclo 19, mostrava `R$ 0` de mídia e teto audiovisual ausente em um bloco
genérico, seguido de links para outras páginas. Banco e endpoints confirmaram ciclo
aberto em planejamento: esses valores eram limites, não consumo. O parecer curto de
Plutus já estava integrado pelo PR 5758 e é reutilizado.

Comparação: ampliar apenas o texto mantém a procura entre telas; unificar todos os
orçamentos mistura finalidade e moeda; reunir três blocos com os formulários canônicos
mantém contexto, limites e decisões independentes. A terceira alternativa é adotada.

## Matriz de aceite local

| Caso | Verificação |
| --- | --- |
| Três finalidades | Preparação acumulada em US$, produção de dois vídeos em US$, anúncios em R$; explicações e escopo visíveis juntos. |
| Valores incompletos | Nulo não vira zero, teto não vira gasto, verba registrada não vira liberação comercial; sem total somando moedas ou duplicando revisões. |
| Decisões no lugar | Formulários existentes abrem na mesma tela; consulta não grava nem cria tarefa/campanha. |
| Gates | Ciclo encerrado ou etapa incompatível explica o bloqueio no próprio bloco; backend revalida o comando. |
| Concorrência e recuperação | Falha preserva formulário e chave da mesma decisão; nova entrada usa nova chave; aprovação atualiza as leituras do mesmo contexto. |
| Isolamento | Produto e ciclo exatos; outro produto e caminho antes válido cobertos; nenhum dado de produção é alterado pelos testes. |
| Usabilidade | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; sem transbordamento horizontal; botões com espera e confirmação explícita. |
| Conselho | Parecer real curto com números preservado; detalhes recolhidos e parecer ausente/desatualizado identificado. |

## Aprendizado reutilizável

A lacuna era de explicação e passagem da decisão, não ausência de orçamento global.
O backend passa a explicar cada finalidade e a etapa permitida. A mesma apresentação
e os controles de recuperação atendem outros produtos. A hipótese é reduzir erros de
autorização e navegação para fora da decisão; teste local comprova comportamento, não
vendas. Adotar se os casos acima passarem; reverter a apresentação se houver divergência
de escopo, sem apagar recibos ou alterar decisões financeiras existentes.

## Resultados locais

- 176 testes Java de demonstração financeira, parecer, orçamento audiovisual, autorização de mídia e arquitetura passaram, sem falhas ou skips.
- 35 testes de interface passaram: escopos separados, ausência, decisões na mesma página, histórico e recuperação. TypeScript passou.
- Jornada local do card até a demonstração, consulta de bloqueios e aprovação recuperável passou em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, sem transbordamento.
- O build no processo da sandbox encontrou limite real de 3 GiB no cgroup. O mesmo limite afetou o container local. TypeScript e build foram separados na validação para evitar concorrência de memória, preservando a configuração de produção (minificação e source maps). Não altera ferramentas compartilhadas nem usa deploy como teste.
- Dados e submissões de teste foram interceptados localmente; nenhum orçamento, tarefa, vídeo ou campanha real foi alterado.
- Build completo minificado e com source maps aprovado em 16,84 s após validação separada de TypeScript. Topologia Docker temporária removida.
