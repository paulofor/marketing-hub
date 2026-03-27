# Guia rápido — Aprendizado automático do experimento

Este manual explica como solicitar, revisar e reaproveitar os aprendizados gerados automaticamente pelo Marketing Hub após a conclusão de um experimento.

## 1. Solicitando a leitura automática

1. Acesse **Marketing Hub → Experimentos** e abra o experimento desejado.
2. Localize o card **“Aprendizado automatizado do experimento”** (logo acima do card de relatório objetivo).
3. (Opcional) Informe no campo “Quem está solicitando?” o nome ou time responsável.
4. Clique em **“Solicitar leitura”**. O card mostrará o status da solicitação:
   - **Na fila / Processando**: o AI Worker está trabalhando.
   - **Concluído**: o resumo já pode ser consultado.
   - **Falhou**: leia o motivo mostrado em vermelho e tente novamente.

> Dica: não é necessário acompanhar manualmente. Assim que o worker terminar, o card é atualizado e o aprendizado passa a aparecer no histórico do experimento e no nicho correspondente.

## 2. Interpretando o resumo

Quando a leitura estiver pronta, o mesmo card exibirá:

- **Resumo executivo** com o estágio do funil, métrica primária e o sinal (ex.: “CPL 28% acima do alvo”).
- **O que funcionou** e **Bloqueios**: textos curtos apontando os principais achados.
- **Próximo teste recomendado**: direciona o backlog com o racional do worker.
- **Dicionário do experimento**: lista os insights por Dor, Resultado, Mecanismo, Prova e Oferta, sempre com a evidência utilizada.
- **Recomendações de backlog**: mostra até três ações priorizadas com estágio, métrica e prioridade sugerida.

Todo aprendizado fica registrado e pode ser reprocessado a qualquer momento (desde que não exista outra solicitação em andamento).

## 3. Reaproveitando no nível do nicho

1. Abra **Marketing Hub → Nichos → [seu nicho]**.
2. Role até os blocos:
   - **Banco de aprendizados**: traz o dicionário consolidado com os melhores insights do nicho. Cada item indica a fonte (experimento) e a evidência usada.
   - **Recomendações para o backlog**: lista pronta para priorização, derivada automaticamente das últimas leituras.
3. Utilize essas informações para:
   - Criar novas hipóteses (botão “Gerar Hipóteses” recebe as dores/resultados mais fortes).
   - Ajustar playbooks de teste (cada insight mostra qual parte do framework reforçar).
   - Registrar aprendizados finais após uma rodada de experimentos.

## 4. Boas práticas

- Solicite uma nova leitura sempre que encerrar um ciclo (ex.: ao desligar campanhas ou concluir a análise de um estágio do funil).
- Use o campo “Quem está solicitando?” para facilitar auditorias e filtros futuros.
- Priorize ações com prioridade **HIGH** no backlog automático — elas já vêm amarradas à métrica primária e ao estágio do funil.
- Caso o worker falhe, reenvie a solicitação. Os logs ficam disponíveis no próprio card e no painel de solicitações do AI Worker.

Com esse fluxo o conhecimento deixa de depender da memória do time e passa a ser parte viva do produto.
