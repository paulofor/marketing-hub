# Matriz local — Microexperiência comercial Rigel v2

## Objetivo e decisão

O gargalo é a divergência entre experiência, oferta, prova e gate de landing. A opção escolhida é
evoluir a mesma superfície para `kit-whatsapp-pronto-pde-v2`, preservando degustação, checkout,
acesso e entrega no motor PDE, sem criar uma segunda landing.

Métrica futura: três pagamentos aprovados, atribuídos e entregues satisfatoriamente em até 15
contatos qualificados e consentidos. QA, visualização, clique e checkout iniciado não contam como
venda.

## Matriz ponta a ponta

| Área | Caminho feliz | Validações e falhas | Evidência local |
| --- | --- | --- | --- |
| Contrato | produto, versão, layout, experimento, CTA e R$ 349 coincidem | checkout bloqueado diante de qualquer divergência | testes dos dois backends e do frontend |
| Oferta | pagamento único, escopo e prazo aparecem antes do checkout | sem oferta quando fornecedor, checkout HTTPS ou binding falhar | contrato HTTP e jornada Playwright |
| Degustação | uma resposta, uma pergunta e três follow-ups sem chamada externa | serviço vazio, combinação inválida e PII não avançam | eventos segregados e resultado determinístico |
| Prova | quatro provas fiéis e aprovadas aparecem sem depoimento ou automação inventada | ausência de prova não desliga o gate | contrato v2, gate de asset e tela |
| Processo | briefing, microvalor, entrega e primeira aplicação são compreensíveis | prazo não inicia sem pagamento e briefing completos | conteúdo versionado e jornada visual |
| Acesso | pós-compra fica em rota secundária e token continua segregado | não interrompe a sequência de venda | `/access` e retomada autenticada |
| Checkout e entrega | checkout de teste abre, acesso QA não registra receita e entrega exige conteúdo | URL insegura, etapa fora de ordem e entrega incompleta bloqueiam | backend, SMTP descartável e E2E |
| Observabilidade | PAGE_VIEW, TASTING_STARTED, VALUE_MOMENT, PAYWALL_VIEWED e CHECKOUT_STARTED têm versão | preview não grava e QA usa `mh_test` | ledger local e assertions de eventos |
| Dados de teste | MySQL, e-mail `teste+...@sandbox.local` e `mh_test=1` | nenhuma métrica humana, contato, gasto ou publicação | topologia local descartável |
| Dispositivos | desktop Chromium, iPhone 15 Pro e Pixel 7 sem overflow | CTA, prova, escopo e acesso utilizáveis em touch | três projetos Playwright |

Se uma rodada revelar defeito, a contagem final reinicia. Depois da última correção, duas rodadas
completas e consecutivas devem passar.
