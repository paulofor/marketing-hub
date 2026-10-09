# Capella: bloqueio compreensível do kit privado — 09/10/2026

## Estado e causa comprovados

Produto 7, cadeia 26, ciclo 5, experimento 98, execução 47. A autorização
`CAPELLA_CYCLE_5_REVIEW_AI_AUTHORIZATION_USD_10_REQUEST_3290` foi registrada pela
tela de edição do produto: até US$ 10 para novas revisões desta preparação, sem
mídia ou vídeos pagos. Custos históricos permanecem separados e são estimados.

A homologação técnica #649 aceitou nove percursos. Psique aprovou #660 (aderente)
e #662 (recuperação), mas #664 rejeitou a apresentação de segurança: a composição
era bloqueada, enquanto o cabeçalho anunciava resultado pronto. O hash da captura
já constava na homologação técnica. A falha vem do título fixo em `prototype.html`,
da ausência de apresentação por estado e do teste SAFETY que verificava apenas
ausência de resultado e efeitos externos. A causa histórica específica não estava
registrada; não é legítimo escolher entre origem visual e ação externa por inferência.

## Alternativas e escolha

| Alternativa | Benefício | Risco e esforço | Aderência |
| --- | --- | --- | --- |
| Ajustar apenas o título fixo | Mudança pequena | Continua sem motivo e próximo passo; pode prejudicar o resultado válido | Parcial |
| Inferir a causa no navegador | Exibição por estado | Dupla fonte de verdade e falsa precisão para sessões históricas; esforço médio | Insuficiente |
| Apresentação do backend e fixture explícita | Título, motivo e ação ligados ao registro; reutilizável entre produtos | Acrescenta contrato e verificações pontuais; esforço limitado | Escolhida |

A condição SAFETY nova é declarada pelo executor e persistida antes do briefing.
Registros sem causa específica oferecem revisão interna com essa ausência explícita.
A próxima ação abre a revisão do mesmo ciclo sem gerar composição, inferência ou
gasto. O pacote, os controles e os caminhos aprovados permanecem sob validação.

## Matriz de aceite definida antes dos testes

| Percurso | Entrada e integração | Resultado exigido | Observação |
| --- | --- | --- | --- |
| Aderente original | MySQL 5.7, backend e compositor reais, contatos sintéticos | 36 arquivos, imagens decodificáveis, personalização, cópia e hash do download | Desktop, iPhone 15 Pro e Pixel 7 |
| Recuperação antes válida | Campo ausente, imagem HTTP 503, reabertura | Erro claro, mesma composição, mesmo hash e retomada | Três dispositivos |
| Segurança original | Origem visual não comprovada | Interrupção principal, nenhum pacote, causa e revisão do mesmo ciclo; estado preservado ao reabrir | Desktop e Pixel |
| Segurança de integração | Pedido de efeito externo | Motivo específico e orientação distinta, sem reserva ou efeito externo | iPhone e controles API |
| Outro produto e versão | Perfil barber-v1, IDs 8018/7018/9018 | Mesmos aceites sem exceção por nome/ID e sem cruzar dados | Nove combinações adicionais |
| Registro anterior sem causa | SAFETY sem condição declarada | Ausência explícita, revisão interna; nunca atribuir causa específica | API e unidade |
| Contrato corrompido | Relatório sem motivo, ação de outro ciclo ou perda do bloqueio | Consumidor recusa antes do modelo; nenhuma aprovação falsa | Unidade com relatório real |
| Autoridade e limites | Consentimento, isolamento, callbacks, concorrência, quotas, revogação | Preservar limites, idempotência, auditoria e falta de efeitos externos | Controles existentes ampliados |
| Passagem local | Psique → Têmis → gate | Consumidores reais com modelos simulados, resultados aceitos pelo contrato | Dois contextos segregados |

Eventos são `AGENT_VALIDATION` e `mh_internal_test`, inelegíveis como venda.
Modelos são simulados na sandbox; `providerCalls=0`, gasto de mídia e efeitos
externos permanecem zero. Medir duração de cada percurso (limite 600 s), nove
capturas por contexto e zero contradições em SAFETY. Compras e contribuição
comercial permanecem desconhecidas sem conciliação: aprovação técnica não é venda.

## Validação e limites

Executar o runner existente `infra/testing/private-kit/run-local.sh` com o projeto
Compose exclusivo da sessão e preservar os relatórios e capturas. Rodar também as
unidades alteradas, formatação e testes de contrato. Após correção, repetir apenas
as verificações afetadas por falhas observadas. Como a apresentação funcional muda,
`CHANGE_PER_CYCLE_V1` exige ciclo e experimento sucessores. A versão corrigida retorna
à homologação e às revisões independentes; pareceres antigos não recebem aprovação
retroativa. Publicação comercial, mídia e produção paga de vídeos continuam fora
da autorização. A navegação da ação interna deve ser confirmada na tela publicada.

Rodada local concluída em 09/10/2026: 18 percursos reais (nove por perfil), seis
callbacks de Psique e dois fluxos Têmis/gate com modelos simulados; nove grupos de
controles aprovados. Backend: 15 testes direcionados e arquitetura aprovados;
worker: suíte de unidades aprovada, além da integração real do runner. Capturas e
relatórios locais em `artifacts/capella-safety-local/`. MySQL temporário removido
com volumes e recursos da topologia isolada. Zero chamadas pagas na validação local.
