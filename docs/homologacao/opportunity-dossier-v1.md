# Matriz de homologação — Dossiê de Oportunidade v1

Objetivo: comprovar que oportunidades são pesquisadas e avaliadas antes de originar um Plano Comercial, sem misturar orçamento ou métricas de produtos ativos.

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
|---|---|---|---|
| Cadastro | Argos registra público, dor, referência, vantagem de IA e proposta | título, público, dor, referência e vantagem são obrigatórios | dossiê auditável em `RESEARCHING` |
| Executor Argos | cadastro abre ciclo `pending` e tarefa na mesa | consumo duplicado é impedido pela reserva do ciclo | ciclo, tarefa e dossiê correlacionados |
| Evidências | fonte e resumo são anexados | URL/fonte e resumo obrigatórios | data e autor persistidos |
| Pareceres | Atena, Psique, Plutus e Hermes respondem | agente não solicitado e parecer duplicado são bloqueados | decisão, risco e recomendação por agente |
| Estados | pesquisa segue para parecer, teste e aprovação | transições inválidas são rejeitadas | histórico temporal no registro |
| Conversão | aprovação humana cria um novo plano | sem parecer completo, sem aprovação ou segunda conversão são bloqueados | vínculo `origin_dossier_id` e plano novo em rascunho |
| Financeiro | plano nasce sem realizado e sem orçamento implícito | custos/receita de outros planos não são copiados | segregação integral |
| Integrações | pesquisa concluída anexa evidências reais e encerra a tarefa de Argos | resultado vazio não fabrica evidência; falha bloqueia a tarefa | contrato Swagger, mesa, monitor e logs correlacionados |
| Frontend | lista, detalhe, cadastro, evidências, pareceres e conversão | loading, mensagens de erro e campos obrigatórios | verdade integral do backend |
| Dispositivos | fluxo em desktop, iPhone 15 Pro e Pixel 7 | sem overflow ou ação inacessível | screenshots/inspeção local |
