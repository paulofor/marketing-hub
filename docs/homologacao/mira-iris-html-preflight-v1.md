# Mira — preflight do HTML de Íris v1

## Objetivo

Impedir consumo de modelo quando a landing ainda não possui checkout ou contrato de tracking e
permitir a materialização somente com contratos comerciais reais e métricas de teste segregadas.

## Matriz ponta a ponta definida antes da validação

| Área | Cenário | Aceite |
| --- | --- | --- |
| Caminho feliz | prova aprovada + checkout HTTPS + `IRIS_LANDING_INSTRUMENTATION_V1` | backend e worker liberam `html`; preço, CTA e URL permanecem literais |
| Validação | checkout ausente | backend bloqueia antes de criar/entregar tarefa ao modelo e informa a ação faltante |
| Validação | contrato de tracking ausente ou divergente | backend e worker bloqueiam com custo de modelo zero |
| Falha segura | HTML contém script, handler inline ou destino diferente | aplicador governado rejeita a candidata |
| Integração | HTML usa seções e CTA canônicos | runtime publicador injeta os quatro coletores sem coletor concorrente |
| Observabilidade | bloqueio de preflight | histórico BPM preserva a causa; nenhuma nova telemetria de modelo é criada |
| Métricas | navegação interna com `mh_test=1` | `mh_internal_test` impede persistência como tráfego humano |
| Dados | experimento #93 | nenhuma leitura, checkout ou compra de QA conta como resultado comercial |
| Regressão | checkout ou HTML surgem depois da mensagem | `communicationContract` não reabre; preço, CTA, versão ou prova alterados reabrem |
| Navegadores | desktop Chromium, iPhone 15 Pro e Pixel 7 | validação visual será executada na candidata somente após existir checkout canônico |

## Limite operacional vigente

O experimento #93 ainda não possui checkout comercial nem slot PDE público ativo. A homologação
produtiva deve parar no preflight barato; criar pagamento, publicar a entrega e autorizar a landing
são decisões humanas distintas da aprovação audiovisual de R$ 80.

## Evidência local

- Backend: 3.604 testes aprovados, sem falhas ou erros; Spotless aprovado.
- Communication Agent Worker: 37 testes aprovados, sem falhas ou erros; Spotless aprovado.
- Contratos cobertos: bloqueio sem checkout, bloqueio sem instrumentação, caminho feliz com ambos,
  defesa duplicada no worker, prompt sem script e estabilidade do hash diante das saídas posteriores.
- Validação visual da candidata: corretamente não executada antes do checkout e do slot público;
  esse bloqueio preserva a segregação de dados e impede publicar apenas para testar.
