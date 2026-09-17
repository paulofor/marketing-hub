# Vega #92 — recuperação da homologação comercial #441

## Evidência observada

- A tarefa #441, **Homologar experiência comercial · Vega**, ficou `BLOCKED` depois de Psique devolver
  uma decisão `ADJUST` tecnicamente fundamentada.
- O bloqueio não era a falta de criativo, checkout ou acesso: o validador rejeitou a resposta porque
  ela não declarou os cartões `RI1-...` de pesquisa recebidos em `researchIntelligence`.
- A própria revisão apontou melhorias comerciais concretas para a v12: CTA antes do vídeo, quatro
  escolhas claras, preço único de R$ 67, acesso por 90 dias, ausência de assinatura e privacidade
  mais escaneável no celular.

## Alternativas avaliadas

| Alternativa                                             | Benefício                                  | Risco                                                         | Decisão   |
| ------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------- | --------- |
| Relaxar o validador                                     | Desbloqueio imediato                       | Permite recomendações sem rastreabilidade de pesquisa         | Rejeitada |
| Inserir cartões automaticamente após a resposta         | Menor alteração no prompt                  | Atribui pesquisa ao parecer sem o modelo declarar como a usou | Rejeitada |
| Instruir Psique a citar os cartões e manter a validação | Preserva a auditoria e evita a recorrência | Exige ajuste do contrato de prompt e teste preventivo         | Adotada   |

Para a primeira dobra, publicar a candidata antes da homologação e manter o vídeo como porta de
entrada foram rejeitados. A solução adotada torna a jornada gratuita e as condições comerciais
visíveis antes do vídeo, que permanece opcional.

## Correção local

- Psique recebe uma instrução versionada para citar em `evidence` pelo menos um cartão `RI1-...` de
  cada coleção entregue, inclusive em decisões `ADJUST`; o validador continua bloqueando citação
  ausente ou inventada.
- A Vega v12 apresenta, antes do vídeo, o CTA **Começar meu ajuste gratuito**, as quatro escolhas,
  a continuidade de sete dias por R$ 67 em pagamento único, acesso por 90 dias e ausência de
  renovação. A privacidade mantém o resumo visível e os detalhes expansíveis.
- O simulador Pepper de homologação preserva a versão recebida no UTM. Assim, uma compra da v12 não
  recebe indevidamente a jornada v7.

## Matriz e resultados

| Dimensão              | Resultado local                                                                                                |
| --------------------- | -------------------------------------------------------------------------------------------------------------- |
| Contrato de Psique    | 114 testes do worker aprovados, com 1 cenário previsto ignorado                                                |
| Build                 | Typecheck e build do frontend PDE aprovados                                                                    |
| Caminho comercial v12 | CTA, condições, primeiro ajuste gratuito e vídeo opcional aprovados                                            |
| Pagamento e entrega   | Pepper simulado, idempotência, acesso de 90 dias, sete missões, retomada e três materiais protegidos aprovados |
| Falha e reembolso     | Reembolso idempotente revoga acesso e materiais corretamente                                                   |
| Métricas              | Tráfego de QA permanece fora das métricas humanas                                                              |
| Dispositivos          | Chromium desktop, iPhone 15 Pro e Pixel 7 aprovados                                                            |
| Limpeza               | MySQL 5.7 e todos os containers/volumes temporários removidos                                                  |

A rodada integral inicial revelou o contrato Pepper de teste fixado na v7 e a asserção desatualizada
de privacidade. Ambos foram corrigidos; a regressão dirigida posterior concluiu **6 de 6 cenários**
nos três dispositivos.

## Limite operacional

Nenhuma tarefa paga, campanha, publicação, cobrança ou alteração de produção foi executada. A tarefa
#441 histórica permanece auditável como bloqueada. Depois de a alteração local passar por Pull Request
e deploy, uma nova tentativa da atividade poderá registrar a evidência de pesquisa e devolver o
`ADJUST` comercial válido; só após as correções verificadas por Psique poderão seguir Têmis,
consolidação, autorização humana e ativação externa.
