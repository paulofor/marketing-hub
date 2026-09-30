# Fonte revisada — agentes de compra sob custo de informação e objetivo vago

**Rodada:** 2026-09-30  
**Coleção:** neuromarketing  
**CardKey:** `ia-gatekeeper-de-compra`

## Evidência usada

O preprint **“Shopping by algorithm: How agentic AI deploys human heuristics as a surrogate consumer”**, submetido ao arXiv em 23/09/2026, avaliou oito LLMs comercialmente implantados de três provedores em duas linhas principais de estudo, testes de robustez e validações, totalizando 16.400 sessões.

O trabalho introduz o ambiente Tool-Lab, em que atributos de produtos ficam atrás de chamadas de ferramenta com custo de aquisição. Sob informação sem custo, pistas como preço “logo abaixo” e enquadramento promocional raramente desviaram a escolha. Quando a aquisição passou a ter custo e o objetivo foi vago (“find the best deal”), os agentes reduziram a busca e omitiram atributos diagnósticos necessários para calcular preço por unidade, como peso ou centavos, produzindo escolhas subótimas semelhantes a heurísticas humanas. Quando o objetivo foi específico (“find the lowest price per ounce”), a aquisição dos atributos diagnósticos e a qualidade da escolha foram preservadas em maior grau.

Os autores interpretam o resultado como evidência de que a vulnerabilidade a pistas de marketing pode emergir da combinação entre arquitetura de informação, custo de busca e especificidade da delegação, não apenas de um viés fixo do modelo.

## Hipótese interpretativa

Em jornadas de compra mediadas por agentes, ofertas cujos atributos críticos estão fragmentados, difíceis de recuperar ou ambíguos podem ser avaliadas de forma incompleta. Da mesma forma, pedidos vagos do usuário podem induzir o agente a encerrar a busca antes de obter os dados realmente necessários para comparar opções.

## Aplicação possível no Marketing Hub

Evoluir o `AgentReadableOfferAudit` para verificar dois pontos separadamente:

1. se preço total, preço unitário quando aplicável, quantidade, entregáveis, condições, limitações e outras variáveis diagnósticas estão explícitas e facilmente recuperáveis;  
2. se o agente esclarece qual critério o usuário quer otimizar antes de comparar alternativas quando a intenção estiver vaga.

A aplicação deve favorecer clareza e decisão informada, não manipulação de agentes.

## Limites relevantes

- É um preprint e ainda não passou por revisão por pares.
- Os experimentos usam ambiente controlado e custos artificiais de aquisição; não equivalem a uma loja real nem a vendas observadas.
- Modelos e políticas de tool use mudam rapidamente, reduzindo a validade temporal de resultados específicos.
- A definição de escolha “ótima” depende do objetivo normativo do experimento; usuários reais podem valorizar qualidade, marca, prazo ou outros critérios além de preço por unidade.
- O estudo não demonstra efeito do ajuste de arquitetura de informação sobre CTR, CPL, conversão ou receita do Marketing Hub.

## URLs originais

- https://arxiv.org/abs/2609.28372
