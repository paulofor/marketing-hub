# Homologação local — falhas posteriores ao PR #5209

Data: 16/09/2026.

## Diagnóstico

- Customer Agent Worker e Meta Ad Approver Worker recusaram corretamente a atestação Rigel v9,
  pois `ProductCatalogServiceTest.java` já possuía conteúdo diferente do SHA-256 registrado.
- A matriz de ciclos MySQL 5.7 chegou a 18 cenários aprovados e falhou ao exigir `flowId` nos
  eventos `ADOPT_BASELINE` e `MEASURE` de um ciclo deliberadamente movido para o grafo v5. Esse
  grafo histórico não declarou IDs nas arestas; ação, estágio de origem e estágio de destino
  continuavam preservados.
- O mesmo erro de ciclos já aparecia no run `35070680133`, anterior ao PR #5209. A alteração de
  leitura sem lock não criou essa divergência.

## Decisão entre alternativas

1. Atualizar o hash da v9 apagaria a imutabilidade da evidência e foi rejeitado.
2. Inventar IDs no grafo histórico melhoraria a aparência, mas reinterpretaria o passado e foi
   rejeitado.
3. Criar uma atestação v10 após revalidar o Rigel e validar explicitamente os campos existentes no
   grafo antigo preserva auditoria e compatibilidade. Esta foi a alternativa aplicada.

## Validações

- Backend PDE: 179 testes, sem falhas ou erros.
- Backend principal: 3.168 testes, sem falhas ou erros; 17 ignorados.
- Customer Agent Worker: 113 testes, sem falhas ou erros; um ignorado.
- Meta Ad Approver Worker: 98 testes executados, sem falhas ou erros e um ignorado.
- Empacotador de evidências comerciais: 12 testes, sem falhas.
- Pacotes reais de Psique e Têmis: 124 arquivos e 15 manifestos cada, gerados com sucesso.
- Testes direcionados do backend: `SalesFlowResolverTest` e `OpalaAdoptionRoutingTest` aprovados.
- Matriz de persistência MySQL 5.7 aprovada: REST, contexto do ciclo, migração, reaplicação,
  idempotência e verificação do diff.
- Spotless do backend aprovado; duas divergências de formatação já presentes no merge foram
  normalizadas.

Nenhuma campanha, publicação, gasto ou dado produtivo foi alterado.
