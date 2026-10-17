# Aceite financeiro de descoberta v1

Matriz definida antes da implementação e dos testes em 17/10/2026. Caso original:
Capella, produto 7, ciclo 12, experimento 105, R$ 100 por cinco dias; margem de 30%
continua obrigatória para continuidade/escala. Fixtures usam outros IDs e fontes sintéticas.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Diminuir a margem ou aumentar vendas projetadas | Edição simples | Apaga a decisão e fabrica viabilidade | Rejeitada |
| Registrar só uma observação | Preserva histórico | O gate determinístico continua bloqueando Plutus | Insuficiente |
| Aceite estruturado, limitado ao experimento | Preserva meta, cenários e limites; permite parecer independente | Exige validação de identidade e regressões | Adotada |

| Área | Aceite local |
| --- | --- |
| Caminho feliz | Cenários desfavoráveis permanecem; aceite explícito libera análise de descoberta, nunca escala |
| Economia | Receita líquida e entrega antes de CAC positivas; custo ausente, tentativas inválidas e dupla dedução bloqueiam |
| Limites | Teto/duração positivos; mídia projetada e publicada não excedem o aceite; nenhuma renovação de orçamento |
| Identidade | Produto, plano, ciclo e experimento compatíveis; rejeitar cópia para outro produto, tipo ou sucessor |
| Integração | Revisão imutável → preflight → Plutus simulado → gate Quartzo, com autorização e resultado rastreáveis |
| Recuperação | Solicitação repetida usa o parecer da mesma revisão; não produz inferência duplicada |
| Observabilidade | Diferenciar descoberta autorizada de rentabilidade projetada, manter cenários, fontes e custos |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7; aceite desmarcado por padrão e valores claros |
| Regressão | Caso original com IDs sintéticos, outro produto e caminho rentável antes válido |

Nenhuma chamada paga, campanha, compra ou dado comercial é criado pelos testes. Testes
comprovam comportamento e segregação, sem demonstrar demanda ou aumento de vendas.


## Resultado local

- 199 casos Java relevantes/arquitetura no backend: zero falhas/erros; uma verificação opcional
  não executada nessa suíte. A persistência essencial foi executada separadamente em MySQL 5.7.
- Executor financeiro: 64 testes, zero falhas/erros. Frontend: 17 testes, typecheck e build aprovados.
- API/JPA/MySQL reais e Plutus simulado: 84 verificações em Chromium desktop, iPhone 15 Pro e
  Pixel 7. Aceite gravado e relido, meta de 30% e cenários negativos preservados, contexto
  estruturado no parecer, replay sem nova execução e rejeição de identidade/teto/duração indevidos.
- A primeira integração detectou sobreposição de stubs na fixture nativa; a relação sintética foi
  corrigida no mecanismo existente. O navegador passou a recusar qualquer origem externa e
  conserva tela/texto em falha. Nenhuma chamada paga ou dado produtivo foi gerado nos testes.
- Bundle das atestações comerciais vigente validado; revisão local do diff e comentários Java
  conferidos. A melhoria técnica não demonstra demanda, contribuição realizada ou lucro.
