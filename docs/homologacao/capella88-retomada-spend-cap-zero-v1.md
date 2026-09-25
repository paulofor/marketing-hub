# Matriz de homologação — retomada Capella #88 sem `spend_cap=0`

Data: 25/09/2026. Escopo: corrigir a retomada financeira do experimento #88 sem
alterar R$ 20/dia, teto acumulado de R$ 125, término em 29/09/2026, parada em
R$ 50 sem resultado ou compra e encerramento em cinco compras.

| Dimensão | Caso | Aceite |
| --- | --- | --- |
| Caso original | Campanha sem `spend_cap`, teto menor que o mínimo da conta | payload da campanha contém diário e pausa, mas omite `spend_cap`; conjunto recebe teto de 12.500 centavos |
| Validação | Campanha já possui `spend_cap` positivo | falha antes de qualquer escrita Meta; campanha permanece pausada |
| Retomada | Diário já migrou para a campanha após tentativa parcial | completa somente o teto do conjunto, confirma os dois níveis e ativa uma vez |
| Falha Meta | Meta rejeita orçamento ou readback diverge | callback preserva corpo oficial e compensação confirma campanha pausada |
| Integração | Destino, Insights e conta Meta simulados | gasto anterior não vira zero; moeda, mínimo, término e URL são confirmados antes da ativação |
| Observabilidade | Código `100/1885099` | erro permanece auditável sem token e existe teste que rejeita `spend_cap=0` |
| Segregação | Identificadores sintéticos e servidor local | nenhum ID produtivo, chamada real, campanha ou gasto durante os testes |
| Produção | Autorização refeita pela tela após deploy | request conclui, Meta confirma campanha ativa, R$ 20/dia e teto R$ 125; backend reconcilia o processo |

Critério final: testes unitários e build do worker aprovados, diff revisado, PR
integrado, deploy concluído e estado produtivo confirmado na Meta e no backend.
Ativação técnica não comprova compra, receita ou lucro.
