# Demonstração financeira no card de execução

Matriz definida antes dos testes. A entrega reúne análise, gráficos e decisão financeira no ciclo exato. Nenhum teste utiliza orçamento real ou dispara agentes.

| Critério | Validação local |
| --- | --- |
| Card em execução, pausa, bloqueio e histórico | Link oficial do backend com produto/cadeia/ciclo; leitura com erro não apresenta ação desatualizada |
| Capella e outro produto | Estimativas por experimento, isolamento de produto e contexto; sem identificação fixa no código |
| Preparação | Teto cumulativo, reserva explícita, confirmação humana e recibo persistido |
| Falhas | Valores inválidos, falta de confirmação, revisão obsoleta, contexto diferente e ciclo encerrado recusados |
| Recuperação | Mesma chave e mesmo conteúdo não duplicam decisão; chave reutilizada com outro conteúdo recusada |
| Integração de agentes | Recibo atual entregue no pending/lease do mesmo experimento; predecessores e outro produto não herdam autorização |
| Plutus | Parecer real e cenários do plano associado; ausência/ambiguidade/vencimento visíveis, sem fabricar conselho |
| Orçamentos | USD preparação, USD audiovisual e BRL mídia separados; leitura e aprovação não iniciam gastos |
| Observabilidade | Recibo, autor, justificativa, versão, fotografia de custos e data preservados; estimativas não são faturamento |
| Navegadores/dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; gráficos e formulário sem overflow |
| Dados de teste | Fixtures locais e banco efêmero; sem mutação ou campanha em produção |

Alternativas: somente link genérico (baixo esforço, perde contexto), duplicar todas as autorizações (alto risco de divergência), demonstração contextual com ledger e formulários existentes (adotada). O aceite é a navegação completa e a decisão recuperável, sem ampliar tetos de nenhum produto nesta entrega.

A lacuna do harness era o teto dependente de texto histórico. A nova decisão tem recibo no ledger do ciclo e segue aos executores; notas antigas são preservadas sem conversão automática. Impacto comercial depende de compras e contribuição medidas posteriormente.

## Resultados locais

- Navegação real dos componentes em Chromium desktop, iPhone 15 Pro e Pixel 7: card → demonstração → gráficos e parecer → aprovação → falha simulada → recuperação do mesmo recibo, sem overflow ou erro de JavaScript.
- Testes de controller/service, agregação SQL em H2, identidade, revisão, confirmação, recibos, custo desconhecido e recuperação. A decisão não altera estágio, mídia ou audiovisual.
- A matriz utiliza dados sintéticos (`productId=700`, `cycleId=1900`) em testes de navegador; nenhum orçamento real foi aprovado.
- As consultas reais confirmaram a ausência do link e novos custos após o histórico da conversa. Não são saldos conciliados: a cobertura parcial permanece visível e notas antigas não viram limites automaticamente.

A suíte ampliada de ciclos, execução e compatibilidade de Mira completou 785 casos sem falhas funcionais. O processo excedeu os 3 GiB da sandbox ao iniciar ArchUnit depois desses testes; a arquitetura foi isolada em uma execução separada. O build de produção do frontend, incluindo mapas de depuração, passou com limite de heap local. Não houve necessidade de modificar limites globais da imagem.

A nova atestação de compatibilidade de Mira preserva os manifestos históricos e não altera a experiência, a campanha ou o parecer comercial: somente `LearningCycleService` recebeu métodos adicionais de consulta e autorização administrativa. As transições preexistentes foram revalidadas pela suíte de ciclos.

Conferência final local: 92 regras ArchUnit aprovadas na execução isolada; 37 testes de interface aprovados (incluindo os caminhos anteriores do card), typecheck e build de produção completos. Pacote de revisão: 768 arquivos e 191 manifestos íntegros. Os novos testes da tela e do link foram incluídos no CI existente. A mudança não depende de novo container nem de migração de schema.
