# Recuperação de credencial de vídeo Meta v1 — 21/10/2026

## Evidência e decisão

Capella7/ciclo12/experimento105/run19: homologação11/11 aprovada e autorização
registrada pelo frontend. A conexão principal foi validada e o alcance/thumbnail
responderam200, mas o upload de criativo540 usou uma credencial dedicada vencida
em19/10 (190/463). Nenhuma campanha foi criada nessa tentativa. Não houve nova
inferência, alteração do vídeo aprovado ou aumento do limite financeiro.

Alternativas avaliadas: substituir manualmente a segunda credencial (repasse e
recorrência), ignorá-la sempre (regressão de conexões especializadas válidas), ou
recuperar somente sua expiração explícita pela principal da mesma conta. Adotada
a terceira, com limite de uma recuperação e auditoria do recibo rejeitado.

## Matriz local e critério de aceite

Todos os testes usam HTTP local, credenciais fictícias e nenhuma compra, mídia ou
chamada de IA. O caminho completo de campanha já existente permanece na suíte.

| Caso | Aceite |
|---|---|
| Expiração dedicada em duas contas distintas | Uma recuperação, três fases com principal, recibo400 preservado |
| Credencial dedicada válida | Preferência preservada, sem mudar conexão global |
| Sem dedicada | Caminho principal preservado |
| Renovação concorrente | Upload mantém sua identidade; nova conexão global não é sobrescrita |
| Credencial única expirada | Uma chamada, bloqueio |
| Duas credenciais expiradas | Duas chamadas, encerramento |
| Reprovação de permissão | Bloqueio sem trocar identidade |
| Protocolo oficial indisponível | Legado usa a mesma credencial |
| Campanha com vídeo e falha final | Suíte existente preserva callbacks, orçamento e bloqueio sem imagem substituta |

A regressão inicial teve nove casos: duas falhas e dois erros; os demais passaram.
Isso reproduziu recuperação ausente e uso instável da credencial durante concorrência.
Após a correção, os 215 testes da suíte completa do executor passaram, sem falhas,
erros ou testes ignorados. A revisão do PR preserva os recibos da validação local.
Nenhuma mudança visual foi necessária; a ativação publicada é conferida no painel.

## Aprendizado, limites e reversão

Capacidade: Hermes/publicação Meta. Hipótese: menos interrupções de upload por
credencial secundária antiga, sem perder a renovação concorrente. Métrica técnica:
recuperação concluída em uma tentativa e zero mutações da conexão global pelo upload.
Impacto em conversão e contribuição exige observação posterior; não há ganho de
vendas demonstrado por estes testes. Custos de IA zero na validação local.

Adotar somente com regressões verdes, PR integrado, workflows e runtime conferidos.
Reverter o commit se surgir troca de identidade, perda de auditoria ou repetição
ilimitada; não alterar credenciais nem apagar tentativas como recuperação.
