# ADR — integrar expectativas à biblioteca e aos ciclos existentes

Data: 25/11/2026. Estado: arquitetura adotada; homologação registrada no documento de aceite.

| Alternativa | Benefício | Risco/custo | Escolha |
|---|---|---|---|
| Apenas adicionar recomendações em prompts | Pouco esforço | Não compara alternativas nem confere resultado; dependente da interpretação do modelo | Insuficiente |
| Novo agente, pipeline e banco de experimentos | Especialização | Duplica memória, filas, autoridade e custo; exige migração e novos gates | Desproporcional |
| Protocolo + ferramenta determinística + leitura dos ciclos existentes | Hipóteses rastreáveis, contexto reutilizável, sem IA paga ou autoridade paralela | Prévia não persistida e julgamento criativo continua com os especialistas | Adotada |

A lacuna foi observada em `behavioral-core-v4.md` (orientação de surpresa segura), biblioteca de
Atena e `ResearchIntelligenceService` (roteamento audiovisual/curadoria sem protocolo transversal).
Não é incidente de pipeline nem alteração de testes de arquitetura. A biblioteca e os ciclos já
possuem a base necessária; o MCP confirmou seis recibos MEASURE no ambiente, sem provar relação
com surpresa. A cadeia 26 está RETIRED e a tela aponta catálogo v28; nenhuma execução histórica foi
migrada. Nenhum produto específico foi escolhido pelo pedido.

Aceite local definido em `docs/homologacao/expectativas-surpresa-v1.md`.
