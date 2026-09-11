# Fonte revisada — Feedback de IA: objetivo errado vs. método errado

Data da revisão: 2026-09-11

## Evidência encontrada

O artigo peer-reviewed **“Neural Value Alignment: Human-AI Collaboration Under Goal-Action Ambiguity”**, publicado online em 24 de agosto de 2026 no *IEEE Transactions on Cybernetics*, propõe separar dois tipos de desalinhamento durante colaboração humano-IA.

- **Reward prediction error (RPE)** representa discrepância ligada ao resultado/objetivo.
- **State prediction error (SPE)** representa discrepância ligada ao estado/transição, isto é, ao modo como a ação é executada.
- Em uma tarefa experimental com EEG, os autores mostraram que RPE, SPE e sua coocorrência podiam ser decodificados corticalmente em diferentes contextos.
- Em simulações, combinar RPE e SPE acelerou o alinhamento entre humano e IA, inclusive com decodificação imperfeita.

A divulgação institucional da KAIST, publicada em 9 de setembro e repercutida em 10 de setembro de 2026, destacou o mesmo princípio como uma forma de distinguir “o objetivo está errado” de “a forma de agir está errada”.

Fontes primárias e institucionais:
- https://pubmed.ncbi.nlm.nih.gov/42636135/
- https://doi.org/10.1109/TCYB.2026.3722605
- https://www.eurekalert.org/news-releases/1143282

## Hipótese interpretativa

A utilidade do achado para UX não depende de usar EEG. O princípio pode ser transportado para feedback explícito de baixa fricção: quando o usuário rejeita uma ação do agente, distinguir **erro de objetivo** de **erro de método** pode permitir uma recuperação mais precisa do que um feedback genérico como “não gostei”.

Essa transposição é uma hipótese de design; o artigo não testou um produto comercial nem uma interface de feedback textual.

## Aplicação possível

No customer-agent ou em agentes operacionais do Marketing Hub, após uma rejeição, undo ou correção, testar duas rotas simples:

1. “O objetivo que entendi estava errado.”
2. “O objetivo estava certo, mas a forma de executar estava errada.”

A primeira rota deve revisar intenção, critérios e resultado desejado. A segunda deve preservar o objetivo e revisar plano, ferramenta, sequência ou apresentação.

## Resultado real no produto

Ainda não há resultado observado no Marketing Hub. A aplicação precisa ser validada por experimento próprio, medindo taxa de recuperação, número de re-prompts, abandono e conclusão da tarefa.
