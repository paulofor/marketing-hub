# Mira — compatibilidade com o escopo e replay de Atena

A mudança compartilhada distingue proibição de comparação, obrigação e capacidade implementada
na mesma candidata. O replay preservado só se aplica à falha técnica específica, com contexto,
identidade e bytes originais conferidos; não desbloqueia rejeições funcionais. O CI identificou
corretamente seis fontes divergentes da prova anterior: consumidor, protocolo, seus testes,
validador do worker e workflow do backend. A v75 permanece imutável.

Manter a fonte antiga impediria a correção; afrouxar a prova removeria a proteção de integridade.
Foi escolhida uma sucessora de compatibilidade, após regressões pertinentes. A v80 parte da v75
vigente na main. As versões v76–v79 pertencem ao PR 5793 ainda não integrado e não são copiadas,
alteradas ou consideradas publicadas. A composição futura precisa conciliar suas provas com
os arquivos finais, preservando as referências históricas de ambas as entregas.

Verificações locais desta compatibilidade, sem provedores, callback produtivo ou escrita remota:

- 104 casos Java do backend: protocolo de entrada real de Mira v3, comparação implementada,
  identidade incompatível, serviço/controller privados, política de correção, 21 controles de
  evidência operacional e preparação da recuperação com a auditoria integral de 899.
- 108 casos Java do executor: contrato completo, capacidade implementada, matriz comum,
  retomada HTTP com duas identidades, callback perdido e resposta integral preservada.
- 39 testes do empacotador de evidências e resolvedor de publicação, incluindo recusa de hash
  divergente, imutabilidade e isolamento de candidata.

Todos passaram, sem casos ignorados. Na primeira invocação de recuperação foi fornecido o
arquivo privado de exportação SQL, cujo formato não é o DTO de auditoria esperado. O teste
recusou essa entrada. Somente essa suíte foi repetida com o DTO integral já preservado da API
oficial e passou; nenhum dado foi reescrito e nenhuma asserção foi removida.

A v80 atualiza os seis hashes comprovadamente alterados, inclui a política compartilhada e
suas regressões, referencia a v75 com hash e registra esta prova. Mantém experiência
`mira-private-candidate-v3`, fingerprint do frontend, `publishedByManifest` v25 e
`automaticDeployOnMerge=false`. Sua inclusão isolada deve retornar `frontend-version=none`.
O bundle comercial e o catálogo separado de evidências operacionais devem continuar válidos.

Não há nova homologação de produto, aprovação de Psique/Têmis, versão comercial, orçamento,
ativação, publicação, mídia ou alegação de aumento de vendas. A evidência desta atestação é de
compatibilidade local do harness, não de experiência premium nem de comportamento de mercado.
