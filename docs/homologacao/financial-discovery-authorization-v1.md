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

- 217 casos Java relevantes/arquitetura no backend: zero falhas/erros; uma verificação opcional
  não executada nessa suíte. A persistência essencial foi executada separadamente em MySQL 5.7.
- Executor financeiro: 64 testes, zero falhas/erros. Frontend: 43 testes (financeiro e planejamento), typecheck e build aprovados.
- API/JPA/MySQL reais e Plutus simulado: 84 verificações em Chromium desktop, iPhone 15 Pro e
  Pixel 7. Aceite gravado e relido, meta de 30% e cenários negativos preservados, contexto
  estruturado no parecer, replay sem nova execução e rejeição de identidade/teto/duração indevidos.
- A primeira integração detectou sobreposição de stubs na fixture nativa; a relação sintética foi
  corrigida no mecanismo existente. O navegador passou a recusar qualquer origem externa e
  conserva tela/texto em falha. Nenhuma chamada paga ou dado produtivo foi gerado nos testes.
- Bundle das atestações comerciais vigente validado; revisão local do diff e comentários Java
  conferidos. A melhoria técnica não demonstra demanda, contribuição realizada ou lucro.


## Passagem adicional comprovada antes da publicação

O frontend de Capella retornou 409 ao tentar reaproveitar a peça estática. Comparação do histórico
confirmou a imagem #693/596 e pareceres #694/#695 íntegros, encobertos pela revisão audiovisual
#744, que contém mais artefatos. Três regressões falharam antes da correção: caso original,
outra identidade e reprovação posterior da mesma imagem. A importação passa a usar o conjunto
estático exato e mantém hashes, status e aceite humano. Acrescentado teste de que seleção privada
não pode ser promovida a aprovação comercial, conforme o cânone existente.

Foram comparados repetir revisões pagas (custo e mesma causa), limpar flags históricas (perde
contexto) e resolver a identidade da mídia na fonte compartilhada. A terceira preserva evidências,
recuperação e independência. Não foi criado outro agente nem efetuada inferência para esse ajuste.


Resultado da passagem adicional: 13 testes do transporte visual aprovados, incluindo reprovação
`STATIC_ASSET`/`MIXED` e preservação de ajuste `FINAL_VIDEO`. No conjunto local consolidado,
217 casos Java relevantes/arquitetura, zero falhas/erros e uma condição opcional não executada.
O banco confirmou que o aceite real #619 é `PRIVATE_PREPARATION`; ele permanece privado e
não será usado como licença comercial. As 20 artes de entrega já aprovadas do plano continuam
referências disponíveis para a preparação comercial própria.


A orientação da importação também passou a exibir o motivo devolvido pelo backend em vez
de afirmar genericamente que faltam pareceres. Navegação com HTTP 409 simulado confirmou
12 verificações em desktop, iPhone e Pixel, sem origens externas ou inferência. Os 43 testes
da interface e o novo typecheck/build passaram; backend reempacotado depois do ajuste.
