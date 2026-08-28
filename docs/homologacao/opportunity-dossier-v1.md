# Matriz de homologação — Dossiê de Oportunidade v1

Objetivo: comprovar que oportunidades são pesquisadas e avaliadas antes de originar um Plano Comercial, sem misturar orçamento ou métricas de produtos ativos.

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
|---|---|---|---|
| Cadastro | Argos registra público, dor, referência, vantagem de IA e proposta | título, público, dor, referência e vantagem são obrigatórios | dossiê auditável em `RESEARCHING` |
| Executor Argos | cadastro abre ciclo `pending` e tarefa na mesa | consumo duplicado é impedido pela reserva do ciclo | ciclo, tarefa e dossiê correlacionados |
| Evidências | fonte e resumo são anexados | URL/fonte e resumo obrigatórios | data e autor persistidos |
| Estratégia | Atena reserva a fila exclusiva e decide se o dossiê factual sustenta uma estratégia | Psique, Plutus ou Hermes tentando decidir o dossiê são bloqueados | decisão, risco, recomendação, request/response bruto e modelo de Atena |
| Retomada | lease inativa retorna uma única vez à fila | segunda expiração termina bloqueada, sem loop | tentativa, erro e horários persistidos |
| Estados | pesquisa segue para parecer, teste e aprovação | transições inválidas são rejeitadas | histórico temporal no registro |
| Conversão | aprovação humana após Atena cria um novo plano | sem estratégia completa, sem aprovação ou segunda conversão são bloqueados | vínculo `origin_dossier_id` e plano novo em rascunho |
| Financeiro | plano nasce sem realizado e sem orçamento implícito | custos/receita de outros planos não são copiados | segregação integral |
| Integrações | pesquisa concluída anexa evidências reais e encerra a tarefa de Argos | resultado vazio não fabrica evidência; falha bloqueia a tarefa | contrato Swagger, mesa, monitor e logs correlacionados |
| Frontend | lista, detalhe, cadastro, evidências, pareceres e conversão | loading, mensagens de erro e campos obrigatórios | estados aguardando, trabalhando, bloqueado e concluído atualizados a cada 15 segundos |
| Dispositivos | fluxo em desktop, iPhone 15 Pro e Pixel 7 | sem overflow ou ação inacessível | screenshots/inspeção local |
