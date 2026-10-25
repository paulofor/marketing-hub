# Modelagem por referências — registros operacionais

## 25/10/2026 — preservar o contexto do público na descoberta

A criação do piloto de Vega pela tela retornou HTTP 500, sem persistir modelagem nem iniciar
tarefa. O log do backend identificou `Data too long for column 'target_audience'`. A consulta
MCP confirmou 278 caracteres no público de Vega, `product.target_audience LONGTEXT` e
`product_discovery_cycle.target_audience VARCHAR(191)`. Capella, Mira e Alcyone tinham,
respectivamente, 187, 163 e 171 caracteres; as descobertas históricas recentes inspecionadas
tinham público ausente. Isso confirma uma incompatibilidade na passagem de contexto, não
uma exigência para o usuário simplificar sua descrição.

Comparadas três alternativas: cortar o texto (perde contexto), movê-lo para outro campo
(duplica significado e contratos) e alinhar o armazenamento da descoberta ao produto.
Escolhida a terceira: migração incremental para LONGTEXT, mapeamento JPA explícito e
contrato de entrada sem o limite antigo de 191 caracteres. Dados anteriores são preservados.

A lacuna de homologação também foi corrigida: a dependência simulada da descoberta não
persistia `target_audience`. Agora a matriz usa a coluna no MySQL 5.7 e compara o conteúdo
integral com duas identidades e públicos de 278 e 600 caracteres. A reprodução local anterior
à migração retornou o mesmo HTTP 500; públicos curtos e ausentes permanecem no aceite.

Critérios da correção: preparação e replay sem tarefas; texto integral no banco; início
concorrente sem duplicação; fontes e isolamento mantidos; reinício sem perda; interface
desktop/iPhone/Pixel. A correção não modifica foco, orçamento ou experimento de Vega e não
autoriza pesquisa paga. Logs locais em `artifacts/market-reference-v1/audience-*` e matriz
em `artifacts/product-modeling/local`. Nenhum resultado de teste comprova demanda ou vendas.

Validação local concluída: 289 testes unitários/arquitetura, matriz MySQL 5.7 com conteúdo
integral das duas identidades, público curto/nulo, replay, concorrência, adoção e reinício;
navegação desktop/iPhone/Pixel aprovada. A reprodução anterior falhou como esperado; a rodada
após a correção passou. Compatibilidade: 661 arquivos e 158 manifestos conferidos. Topologia
temporária removida com volumes; nenhum gasto externo ou evento comercial de teste.
