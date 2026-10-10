# Comunicação de Alcyone: preflight financeiro v1

## Evidência e causa — 10/10/2026

Produto 11, experimento 97, execução 44, plano comercial 34/v4 e revisão financeira
15: a tela oferecia solicitar Plutus, mas o comando respondia 409 antes de criar tarefa.
O contrato privado validado não contém `delivery.personalization`; a preparação financeira
declara IA personalizada como hipótese. O preflight confundia ausência com contradição.
Mira e Capella usam contratos comerciais com a declaração explícita, que devem continuar
protegidos. Os pareceres antigos de Atena/Plutus e a revisão independente da entrega
continuam sendo dependências próprias da atividade 4.1.

## Escolha proporcional

| Alternativa | Benefício | Risco/custo | Escolha |
| --- | --- | --- | --- |
| Reescrever o contrato privado histórico | Uniformiza o formato | Altera prova já homologada e aumenta o escopo | Preservar histórico |
| Remover a conferência da personalização | Libera o pedido | Aceita contradições em ofertas comerciais | Rejeitada |
| Reconhecer a preparação privada e expor o mesmo preflight na tela | Avalia hipóteses sem confundi-las com venda aprovada | Exige identidade e limites privados explícitos | Adotada |

## Matriz local de aceite

| Caso | Verificação | Critério |
| --- | --- | --- |
| Contrato privado com campo ausente | Dois produtos sintéticos, API real e fila simulada | Contexto mantém ausência, hipótese e confirmação comercial pendente |
| Contradição explícita | Declaração falsa no contrato e hipótese verdadeira | Tela bloqueia; comando 409; nenhuma inferência |
| Segregação e autoridade | Outro produto, marcador ausente, campanha habilitada, LIVE/TEST | Sem compatibilidade indevida ou chamadas externas |
| Contrato comercial antes válido | Declaração verdadeira e fontes completas | Caminho anterior preservado |
| Concorrência e recuperação | Mesmo parecer solicitado novamente, callback simulado e reinício MySQL | Uma execução por revisão; custos e histórico preservados |
| Usabilidade | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados | Pendência visível sob demanda, botão consistente, sem erro ou transbordamento |
| Observabilidade e economia | Contexto persistido na fila, custo desconhecido, cenários condicionais | Projeção não é demanda nem autorização comercial |

Fixtures permanecem no harness existente de planos financeiros, com IDs sintéticos
95121–95123, MySQL 5.7 isolado e Plutus simulado. Nenhum dado de teste entra no produto
comercial. Os testes locais comprovam comportamento; redução de tempo parado e impacto
em contribuição precisam ser medidos nas próximas passagens reais.

## Resultado local

- Backend: 4.421 testes, zero falhas/erros; 37 casos condicionais de outras topologias
  foram ignorados. O fluxo financeiro desta mudança foi executado com MySQL real.
- Worker financeiro: 64 testes sem falhas; frontend: 43 testes, tipos e build aprovados.
- Integração do harness existente: 65 verificações de API, 104 de navegador, 87 de
  preparação e 97 de decisão de margem. Dois contratos privados encaminhados uma vez
  cada e um contraditório recusado antes da fila; zero chamadas reais de modelo.
- MySQL 5.7: aplicação, reaplicação, recuperação histórica, rollback e restauração
  passaram. Reinício manteve revisões, pareceres e custos; Compose e volumes removidos.
- Chromium desktop e emulações iPhone/Pixel passaram. Esta evidência não representa
  testes em aparelhos físicos ou no motor Safari.
- Recursos do pacote, atestações comerciais, `bash -n`, ShellCheck e diff conferidos.

Artefatos locais não comerciais: `artifacts/product-financial-plan/diagnostic-1`.

## Aprendizado reutilizável

Capacidade: preparação financeira anterior a Íris. Hipótese: expor o preflight e reconhecer
o contrato privado reduz pedidos condenados a falhar, sem flexibilizar gates comerciais.
Métrica de adoção: solicitações válidas chegam uma única vez à fila; conflitos são recusados
antes do consumo. Reverter a compatibilidade se qualquer fixture comercial contraditória
for aceita. Não há evidência nova de demanda, compras ou lucro nesta validação.
