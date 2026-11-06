# Estúdio: capacidades, tomadas e custo — 05/11/2026

## Escopo e preservação

Melhoria transversal solicitada em 05/11/2026; sem render pago, compra, ativação, mídia ou publicação comercial. #3925 pertence ao PR #5792 (aberto, HEAD ae5db6ea na consulta), não é duplicado. Vega projeto 14/perfil 69 não possuía jobs de render na consulta MCP em 05/11 às 15h UTC. Backend publicado: c37eb368506550dc8c4251254cc75d1987c7dffd. O monitor mostra Apolo no ciclo 46; isso não prova vídeo produzido. Preservar workspace e histórico alheios. Merge/deploy requerem confirmação específica, conforme pedido.

## Causa e alternativas

A chamada recusada informada pelo usuário (9ca43054-e345-450c-b68c-7bfc88966a3f, 09:53:30) pede 15s com Gen-4.5 como único elegível. MCP não retornou essa linha no trecho atual de logs; não inferir restrição da conta. Código confirma limites independentes no frontend/backend/executor, clamp silencioso e custo genérico de 12 créditos/s. A revisão do fechamento também confirmou liquidação indiscriminada ao encerrar job: agora resultado incerto e custo desconhecido preservam a reserva em CONSUMING, com pendência persistida, sem inventar custo zero. Banco confirma 15 cadastros, Seedance 2.5 apenas Pippit DRAFT, sem Kling 4/Aleph 2. Snapshot financeiro público não equivale a saldo consultado nesta execução.

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Ajustar apenas 15 para 10s | Pequena alteração | Outros modelos, custos e duração final continuam divergentes | Rejeitada |
| Importar todos como ativos | Catálogo amplo | Adapter, direitos e qualidade não comprovados; gasto indevido | Rejeitada |
| Contrato versionado compartilhado e gates reais | Mesma regra em todas as camadas; bloqueio antes do gasto | Exige compatibilidade e regressões cross-módulo | Adotada |

## Matriz local antes dos testes

| Grupo | Aceite |
| --- | --- |
| Caminho feliz | Gen-4.5 32s → tomadas válidas, montagem final 32s, áudio e legenda preservados |
| Validação | 15s direto bloqueado sem POST pago; mínimos e conjuntos Veo; Seedance 2.5 até 30s sem virar 2.0 |
| Integração | Router incompatível ou modelo inesperado bloqueado; catálogo DRAFT/HOMOLOGATION continua sem ativação |
| Financeiro | Tarifa/resolução/áudio/referências/mínimos/promoção; vencido e custo desconhecido bloqueados; reserva integral e tentativas incertas |
| Recuperação | Replay idempotente e nenhum retry pago após resultado incerto |
| Observabilidade | ID de execução, revisão, tomadas, duração cobrada/final, erro/recuperação e fontes auditáveis |
| Isolamento | Caso original, outro produto/ciclo e caminho antes válido, sem conteúdo MUSA genérico |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7; catálogo e plano antes de confirmação; nenhum click que gere custo |
| Dados e métricas | Doubles e mídias sintéticas locais segregados; zero chamadas de geração externa, nenhuma venda presumida |

## Fontes oficiais

- https://docs.dev.runwayml.com/guides/models/
- https://docs.dev.runwayml.com/guides/pricing/
- https://docs.dev.runwayml.com/assets/inputs/
- https://docs.dev.runwayml.com/api/
- https://docs.dev.runwayml.com/model-routers/generating/
- https://docs.dev.runwayml.com/model-routers/configuration/

Revalidadas em 05/11/2026. Disponibilidade pública não é homologação da integração. Tarifas do snapshot vencem em 12/11 e exigem renovação; promoção Kling termina em 30/11 UTC. Sem ranking de qualidade comprovado.

## Resultados observados

- Worker: 306 testes, zero falhas/erros, 9 integrações opcionais ignoradas. Inclui doubles HTTP, Gen-4.5/15s sem POST pago, Router incompatível, Seedance 2.5, tarifas, idempotência e resultado incerto.
- Montagem real sintética com ffmpeg/ffprobe: 32s preservados; Veo 15s finais/16s gerados com corte explícito de 1s; streams de vídeo, áudio e legenda mantidos. Não comprova qualidade estética de render externo.
- Backend focado: 158 testes, incluindo 93 regras de arquitetura; reserva completa, outro produto/ciclo, custo desconhecido, fechamento incerto protegido, replay do mesmo dry run após corrigir custo e HTTP real com retorno 400 acionável.
- MySQL 5.7.44 local: migração aplicada e repetida sem erro; seis novas rotas sem gates ativados e histórico Pippit preservado. Schema real consultado via MCP para confirmar campos obrigatórios.
- Financial worker: 90 testes sem falhas; removidos orçamento MUSA fixo e tratamento de histórico desconhecido como custo zero. O ganho comercial continua hipótese, sem métrica de vendas inventada.
- Frontend: 50 testes relevantes aprovados, typecheck e build aprovados. Ajustes posteriores de texto e referência neutra têm regressão específica. Preview não consulta geração paga nem usa previsão integral de outro plano; resoluções vêm do catálogo. A referência de imagem 1953 também foi removida do estado genérico e é limpa na troca de contexto. A guia narrativa acompanha a duração informada, e metadados/guias genéricos não carregam MUSA.
- Navegador: Chromium desktop, iPhone 15 Pro e Pixel 7 passaram com controller/planner reais e demais integrações como doubles; tomadas, preços, Veo discreto, DRAFT bloqueado e contexto neutro. Capturas locais segregadas em `.codex/studio/browser-final`, sem material comercial publicado.
- Contratos do CI (14), empacotamento (12), resolvedor PDE (22) e pacote comercial de evidências aprovados; resolvedor retorna frontend PDE `none`. Atestação técnica Mira v76 preserva v74 e não sobrescreve v75 do PR #5792.
- Scripts: bash -n, ShellCheck e diff sem whitespace inválido. Auditoria AST de responsabilidade de classes/métodos Java sem lacunas nos arquivos alterados.

Regressão ampla do backend: 5.829 casos, dos quais 67 condicionais/historicamente desabilitados. Houve uma única divergência na expectativa antiga do texto de erro em ExperimentVideoAssetServiceTest; o bloqueio funcional de 15s já estava correto. A assertiva foi alinhada ao novo contrato e os 36 testes desse serviço foram reexecutados com sucesso. Os relatórios finais das 770 classes estão sem falhas ou erros; não se repetiu toda a matriz por uma mudança de assertiva. O MySQL do Estúdio, condicional na suíte geral, passou separadamente com a condição habilitada.

A compilação e o executor de testes foram separados na sandbox de 6 GiB, evitando manter o heap do compilador junto à suíte. Tentativas anteriores encerradas pelo sistema com código 137 não são registradas como testes aprovados. A configuração auxiliar de heap ficou somente na sandbox.

O CI do frontend agora executa as regressões do Estúdio, e o worker de vídeo passa a ser validado também no PR, antes da publicação da main. Essa lacuna foi comprovada na leitura dos workflows existentes. Actionlint aprovou os quatro workflows envolvidos, incluindo ShellCheck dos comandos embutidos. Não há novo executor nem caminho alternativo de publicação.

## Limites e adoção

Nenhuma chamada de geração, inferência paga de homologação, recarga, assinatura, mídia ou ativação foi executada. Os US$15 de Vega permanecem fora deste trabalho. Em 05/11 às 16h27 UTC, MCP reconfirmou zero jobs no perfil 69, ciclos 45/46 somente de preflight e backend c37eb368; PR #5792 continua aberto no HEAD ae5db6ea.

Novo modelo fica DRAFT/HOMOLOGATION até adapter, tarifa, licença e qualidade comprovados. A tarifa pública é um máximo previsto em créditos, não faturamento conciliado. Uma previsão integral ausente bloqueia antes de parecer/gasto; o novo contrato permite registrá-la com fontes, mas não fabrica custos nem completa automaticamente lacunas de produção.

Eventos preservam request, ID da task, resposta, URL, tamanho e SHA-256 de cada tomada. Falha parcial exige conciliação/reuso; este PR não cria armazenamento durável adicional dos bytes intermediários nem garante recuperação quando o provider já expirou a URL. Não há repetição paga automática.

Critério de adoção: manter as regressões locais/CI, atualizar o snapshot antes de 12/11 e somente publicar após revisão e confirmação específica. Reverter pelo fluxo versionado caso contrato/adapter divirjam; não apagar histórico, liberar reserva incerta nem trocar modelo silenciosamente. Qualidade premium, economia por material aprovado e impacto em vendas dependem de homologação/comportamento real posteriores, com autorização própria.

## Correção da primeira rodada de CI

No HEAD e56bb20, jobs MySQL 5.7 e a validação da biblioteca falharam ao compilar: a fixture Docker copiava apenas `pom.xml/src` do módulo, deixando a fonte compartilhada fora da imagem. Não foi falha transitória do runner nem erro do SQL. A compilação local da aplicação e a migração específica haviam passado; a lacuna estava no empacotamento dessas fixtures.

O Dockerfile comum e os 31 contextos de build em 17 descritores agora preservam o layout do monorepo e recebem a mesma fonte/recurso compartilhados; as duas outras fixtures Maven do backend foram conferidas e alinhadas. O cenário físico da Rigel passou localmente com compilação, migração, reaplicação e retomada; o cenário da biblioteca também passou localmente, incluindo rejeição de schema parcial, unicidade e retomada. Quatro testes de contrato previnem a omissão e rodam no CI de vídeo. Docker Compose confirmou os 17 descritores e Actionlint/ShellCheck aprovaram os comandos pertinentes. A correção segue no mesmo PR; a rodada com falha permanece como auditoria, e o resultado final deve ser o do novo HEAD.

## Idempotência do comando genérico de reprocessamento

A revisão final identificou que `SalesVideoJobService.retry` clonava os metadados financeiros do render sem novo preflight; o limite de tentativas era a única regra da política. A consulta MCP não encontrou renders Runway persistidos com `retry_of_job_id` em 05/11, portanto não se atribui gasto duplicado ao histórico. A vulnerabilidade foi comprovada pelo caminho executável, e a política agora impede retry genérico de RENDER/RETRY Runway, inclusive na elegibilidade exibida à UI. Nova geração deve seguir o ciclo governado com conciliação e reserva próprias; reuso/callback/acabamento de material existente não gera outra tomada.

O worker também recusa clones legados antes do planejador/POST pago. Regressões cobrem duas identidades, zero chamadas externas nesse bloqueio, caminhos antes válidos de pós-produção/roteiro/outro provider e limite de tentativas. Os 3 testes da política e os 69 do serviço de jobs passaram, além da suíte completa do worker (306 casos, 9 opcionais ignorados).

## Regressão completa das telas consumidoras

O check de processos automáticos também executa a suíte completa do frontend. No HEAD 5d4564a7, encontrou três expectativas antigas: a página vazia presumindo duração longa de MUSA, um projeto de teste sem categoria explícita esperando validação LONG_FORM e o marcador orgânico contendo MUSA. O comportamento novo estava correto; as fixtures foram corrigidas para contexto neutro e categoria explícita, preservando as validações de duração.

A reprodução local confirmou os três casos. Após os ajustes, os 1.041 testes dos 210 arquivos do frontend passaram integralmente. Navegação do Estúdio e plano orgânico passaram a integrar também o check inicial do frontend, junto ao catálogo e ao preview, para antecipar a detecção dessa classe de regressão. Não foi restaurado conteúdo MUSA implícito nem alterado comportamento para satisfazer a expectativa antiga.

A continuação local da homologação de processos também passou após as unidades já validadas: contratos de deploy/retomada, imagem do worker Node 22 sem privilégios, MySQL 5.7, persistência da jornada, fluxo HTTP, ciclo de vida, reinício e navegação desktop/iPhone/Pixel, incluindo fila/reservas e planejamento inicial. Foi usado Node 22 isolado no workspace e rsync instalado na sandbox; a instalação compartilhada de Node/npm/npx permaneceu íntegra. A topologia temporária foi removida ao terminar.

O gate anterior ao render também exige `financialDecision=APPROVED` no ciclo persistido. A indicação de aprovação nos metadados enviados pelo cliente não substitui o parecer de Plutus. A regressão aceita o ciclo aprovado e bloqueia o mesmo contexto sem decisão persistida, antes de qualquer geração; testes relacionados de perfil, job, preflight, ciclo, HTTP e arquitetura passaram após o ajuste.
