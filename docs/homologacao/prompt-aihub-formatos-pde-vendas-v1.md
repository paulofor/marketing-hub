# Prompt AIHUB: formatos de PDE, valor e vendas

Data: 14/09/2026. Escopo: orientação compartilhada pelo botão **Prompt para AIHUB**.

## Investigação e decisão

`ProductProcessContextCopy.tsx` importa o Markdown
`frontend/src/pages/product/prompts/process-aihub-help.v1.md` e acrescenta o contexto oficial.
Cópia, prévia e alternativa manual compartilham esse texto. O histórico está em
`prompt-aihub-aperfeicoamento-agentes-v1.md`. A neutralidade de formato já está prevista em
`docs/canonical/cadeia-produtos-pde-canon.v1.md`; faltava explicitá-la no pedido ao AIHUB,
com o exemplo Quartzo e a prioridade comercial solicitada.

O catálogo distingue `PDE`/Opala de `LOW_TICKET_DIGITAL_PRODUCT`/Quartzo. A nova orientação
usa PDE como experiência de produto digital (Product Digital Experience), preservando
códigos e vínculos. Escolher o formato da experiência não reclassifica o produto.

| Alternativa                                 | Benefício                                                                | Risco                                                                 | Esforço | Aderência e decisão                           |
| ------------------------------------------- | ------------------------------------------------------------------------ | --------------------------------------------------------------------- | ------- | --------------------------------------------- |
| Registrar apenas no cânone                  | Regra central documentada                                                | O prompt copiado depende de leitura posterior para orientar a decisão | Baixo   | Parcial para o pedido de incluir nos prompts  |
| Atualizar o prompt compartilhado e o cânone | Todos os pedidos recebem a diretriz; cópia e prévia permanecem coerentes | Requer validar conteúdo e preservação do contexto                     | Baixo   | Escolhida; cumpre o pedido no fluxo existente |
| Criar variantes de prompt por tipo          | Permite instruções específicas para cada experiência                     | Duplicação e divergência futura entre variantes                       | Médio   | Desnecessária para uma regra transversal      |

A mudança orienta valor real, produtos incríveis e comunicação eficaz para gerar vendas
e receitas. Inclui escolha fundamentada do formato, fidelidade da comunicação, entrega
comprovada e métricas pertinentes. Mantém correção sistêmica, aperfeiçoamento dos agentes,
consulta às pesquisas e as regras de publicação.

O backend já expõe contexto em `businessprocess.execution`, `businessprocess.automation.v1`
e `businessprocesschain.learningcycle.v1`; a alteração é texto estático, sem chamada de IA,
novo endpoint, mudança de Java ou persistência de negócio. Nenhum produto é reclassificado.

## Continuidade da revisão de imagens

A base recebida (`d490e823f`) já contém a migração sistêmica para
`gpt-image-2.5-sunburst`, com registro em
`gpt-image-2-5-sunburst-migracao-sistemica-v1.md`. A
[documentação oficial do modelo](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst),
consultada nesta solicitação, confirma o identificador e sua posição como modelo mais capaz
de geração e edição de imagens. Esta tarefa verifica o contrato estático existente;
os testes e mudanças da migração anterior não são contados como execução desta tarefa.

## Matriz definida antes dos testes

| Critério                   | Validação local                                                                                                                    |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Caminho feliz              | Prompt copiado e prévia incluem objetivo comercial, definição de PDE, diversidade de formatos e exemplo Quartzo                    |
| Contratos e contexto       | Contexto oficial preservado, inclusive troca de produto/ciclo; sem reclassificação ou mudança de oferta autorizada pelo exemplo    |
| Validações e falhas        | Carregamento, clique duplicado, permissão negada, cópia manual e retentativa                                                       |
| Integrações                | Build real com APIs simuladas; nenhuma chamada paga, mutação de processo ou conexão produtiva                                      |
| Observabilidade e métricas | Confirmação após cópia, erro visível, logs, capturas e textos colados; testes não equivalem a receita ou cumprimento por um modelo |
| Segregação                 | IDs sintéticos, ausência de segredos e de contexto residual de outro produto/ciclo                                                 |
| Navegadores e dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados, HTTP e contexto seguro; sem transbordamento                                    |
| Regressão                  | Suíte de contexto, prompt, painéis e cards, cópia de atividades, TypeScript, build, Prettier e diff                                |
| Modelo de imagens recebido | `scripts/validate-canonical-image-model.sh` verifica os defaults ativos e o contrato Sunburst                                      |

Rodada prevista: `bash infra/testing/process-context-copy/run-round.sh pde-vendas-20260914-1`.
Uma rodada completa sem defeitos basta; defeito corrigido exige duas rodadas completas
consecutivas sem falhas após a última correção. A validação usa dependências locais e
test doubles, sem topologia Docker ou dados comerciais.

## Resultado

A primeira rodada completa passou sem defeitos; não foi necessário repetir a matriz.

| Verificação                                         | Resultado                                                                                                                                      |
| --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Testes do frontend                                  | 107/107 aprovados em 10 arquivos                                                                                                               |
| TypeScript e build Vite                             | Aprovados                                                                                                                                      |
| Contexto, prompt e cópia de atividades no navegador | 6/6 combinações por suíte; 18/18 no total                                                                                                      |
| Integridade do prompt                               | Texto compartilhado integral nas seis cópias, uma única vez, seguido de um único contexto oficial                                              |
| Conteúdo novo                                       | Prioridade de vendas e receitas, Product Digital Experience, diversidade de formatos, exemplo Quartzo e preservação da classificação presentes |
| Isolamento e falhas                                 | Troca de produto/ciclo, permissões, alternativa manual, retentativa e carregamento aprovados; nenhuma mutação ou conexão externa inesperada    |
| Contrato de imagens da base recebida                | Scanner Sunburst aprovado, sem alterar a migração anterior                                                                                     |
| Formatação e diff                                   | Aprovados                                                                                                                                      |

Evidências locais em `artifacts/process-context-copy/pde-vendas-20260914-1/`: logs de
instalação, testes, tipos, build e formatação; `results.json`, capturas e textos colados
nos diretórios `browser/`, `aihub-browser/` e `activity-browser/`;
`prompt-integrity.json` com hashes e extensão das seis cópias integrais. O template possui
13.820 caracteres após remover a quebra final; as cópias completas possuem 22.548 ou
22.612 caracteres, conforme a origem local da URL.

Inspeção visual de `aihub-browser/desktop-http-card.png` e
`aihub-browser/iphone-http-preview.png`: confirmação legível, comandos dentro do card e
prévia com quebra de linha, sem transbordamento horizontal. Servidores e navegadores
temporários foram encerrados pelo runner; nenhuma topologia Docker foi criada.

Limites: dispositivos móveis emulados no Chromium, sem Safari físico. Os testes comprovam
a entrega do prompt e a preservação do contexto, não o cumprimento das instruções por um
modelo nem aumento de vendas. Nenhuma chamada paga, produção de imagem, mudança de produto,
commit, push, PR ou publicação foi feita nesta tarefa. Permanecem os avisos preexistentes
de dependências, API CJS do Vite e tamanho do bundle; instalação pelo lockfile sem alterar
versões. As alterações estão locais.
