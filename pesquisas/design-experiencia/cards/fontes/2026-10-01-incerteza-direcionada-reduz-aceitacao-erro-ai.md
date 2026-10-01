# Fonte revisada — incerteza referencial e aceitação de erro em colaboração humano–IA

## Evidência encontrada

O preprint *Referential Uncertainty in Human--AI Collaboration* (submetido em 30/09/2026) estudou uma tarefa colaborativa em que um humano descrevia uma peça e um agente visual precisava escolher e posicioná-la. Uma distribuição de crença elicidada separadamente foi melhor calibrada (ECE 0,15) e discriminou melhor acertos de erros (AUROC 0,65) do que probabilidades brutas dos tokens de ação, que ficaram fortemente superconfiantes (confiança média 0,97; ECE 0,44). Em três modelos de visão-linguagem, os agentes pediram esclarecimento em apenas 3,5% a 16,7% dos turnos.

Em estudo humano controlado com N=210, participantes que recebiam apenas a mensagem padrão do agente aceitaram 78% dos movimentos errados e não distinguiam acertos de erros (AUC 0,50). Descrições precisas e, sobretudo, ressalvas bem direcionadas reduziram a aceitação de movimentos errados para 36%, preservando em grande parte a aceitação dos movimentos corretos. O próprio trabalho mostra uma condição crítica: uma ressalva produzida a partir de um sinal de incerteza mal calibrado pode piorar o resultado.

Fonte primária: https://arxiv.org/abs/2609.39518

## Hipótese interpretativa

Mensagens fluentes e sem ressalva podem funcionar como sinal implícito de certeza. Quando a incerteza é específica e corretamente localizada, ela torna a possibilidade de erro saliente e cria um ponto de revisão. O benefício depende da qualidade do sinal: alertas genéricos ou mal calibrados podem introduzir ruído e prejudicar a calibração do usuário.

## Aplicação possível

Em interfaces de recomendação e revisão do Marketing Hub, testar sinalização de incerteza somente quando houver evidência confiável de ambiguidade ou baixa confiança, acompanhada de uma ação concreta como revisar evidência, comparar alternativa ou confirmar o referente. Não transformar toda resposta em aviso e não usar incerteza como técnica de pressão.

## Resultado real observado

O resultado observado foi comportamental dentro da tarefa experimental: a aceitação de movimentos errados caiu de 78% para 36% com ressalvas bem direcionadas, enquanto a aceitação dos corretos foi em grande parte preservada. O estudo não mediu conversão, vendas, retenção comercial ou decisões de alto risco.
