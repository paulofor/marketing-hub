# Recuperação Vega: ciclo 2 / experimento 92 / processo 75 v6

Matriz definida antes da validação local em 13/09/2026. Nenhuma evidência sintética será usada como venda, aprovação humana ou autorização comercial.

| Área | Critério |
| --- | --- |
| Caminho feliz | Processo pai → ciclo exato → briefing de duas peças baseado em Íris 402 e teto 11 → projetos próprios no Estúdio → preflight → produção somente com gate financeiro → ativos distintos e revisões → integração e homologação; autorização comercial permanece humana. |
| Validações/falhas | Não aceitar referência financeira alheia, outra versão/experimento, campo vazio, vídeo antigo ou reprovado; preservar histórico e falha do preflight. |
| Integrações | Frontend e APIs locais com dependências simuladas; na operação, comandos apenas pela UI oficial, confirmação posterior por MCP. |
| Observabilidade | Identificar fonte de cada briefing, papel AD/LANDING_HERO, projetos/jobs/ciclos, custos e bloqueio concreto. |
| Métricas | Dois candidatos tecnicamente válidos dentro de USD 20 total; primeiros resultados/inícios e checkout são sinais separados de vendas líquidas. Nenhum teste em métricas humanas. |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; validar navegação e campos. Safari real fora do ambiente. |
| Financeiro | Preflight sem cobrança primeiro; considerar custo total incluindo revisões/áudio; não comprar crédito nem acionar mídia/cobrança. |
| Conclusão | Cada objetivo exige evidência própria. Espera por venda, aprovação humana ou acesso externo será informada como pendência real. |

Alternativas de continuidade: (1) registrar o briefing a partir de Íris já concluída: baixo esforço/custo, preserva o contrato, exige adaptação rastreável; (2) pedir novo briefing a Íris: útil se o contrato estiver insuficiente, adiciona custo/tempo e risco de repetir dados; (3) derivar um novo blueprint das peças históricas: acelera referências visuais, exige revalidação integral e aumenta risco de transportar promessas/versões antigas. Escolha inicial: (1), porque a tarefa 402 já define mensagem, demonstração, CTA e limites da versão v12. As duas peças serão novas e segregadas do experimento 91.

Uma rodada local completa sem defeito basta. Havendo correção de código após falha, duas rodadas completas consecutivas após a última correção.

## Causa confirmada no Estúdio e alternativas

O endpoint existente `POST/PATCH /api/sales-videos/projects[/id]` já persiste `campaignKey`, `hookText` e `scriptText` pelo controller SalesVideoController e VideoProjectService. O problema está no frontend: `buildBriefingFromProject` usava o gancho inteiro como dor e o canal como público; a gravação reconstruía o gancho e o roteiro. O projeto histórico #1 mostra múltiplos prefixos repetidos; #3 contém “Valor antigo nao padronizado”. Dois testes locais reproduziram a perda antes da correção. O formulário também limitava campanhas às duas sugestões antigas.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Preservar e editar gancho/roteiro já suportados pela API; sugestões de campanha livres | Corrige o ponto de perda e permite a versão atual | Baixo; exige regressão de salvamentos sucessivos | Escolhida |
| Persistir novos campos para público, dor, promessa e mecanismo | Editor comercial mais amplo | Migração e sincronização de múltiplos campos; médio/alto | Demanda futura |
| Separar a edição da narrativa em um comando próprio, mantendo-a somente para leitura no formulário geral | Evita alteração acidental e torna a intenção explícita | Mais uma tela e mais cliques para revisar a mesma peça; médio | A edição explícita no próprio formulário atende com menor esforço |

A edição do projeto apresenta gancho e roteiro reais, sem mostrar público/promessa inferidos do canal ou da métrica. O modo de criação conserva os campos de apoio para redigir a primeira versão e permite texto final explícito. Nenhuma geração, reserva, aprovação financeira ou publicação é acionada ao salvar. Essa correção usa o endpoint existente; o ajuste de orientação no backend é descrito abaixo.

## Escopo adicional da matriz

- Criar campanha própria para experimento 92 e versão v12; preservar produto/plano/experimento.
- Reabrir, alterar título e salvar duas vezes mantendo gancho/roteiro exatamente iguais.
- Editar texto final explicitamente; presets continuam disponíveis.
- Preflight isolado exibindo bloqueio sem chamada de geração; interfaces desktop/iPhone/Pixel.
- Regressões do Estúdio, ciclo, financeiro e card; contratos backend de orçamento, vídeo e preflight.
- TypeScript, build da imagem versionada, duas rodadas locais completas após a correção.

## Operação confirmada

- MCP confirmou marketinghubdb e backend `1a15c5944eca27e7556124387ade7452a71eac78`.
- Evento financeiro #11: Paulo registrou USD 20 em 13/09/2026 para as duas peças, na v12.
- Pela UI, evento #12 concluiu VIDEO_BRIEF e passou a CAMPAIGN_VIDEO, revisão 9.
- Perfis novos #59 (AD) e #60 (LANDING_HERO); roteiros #560/#561 com CTA privado correto, autoria operacional AIHUB e sem reusar o experimento 91. Aprovação de roteiro não é revisão humana independente do vídeo final.
- O monitor financeiro mostra snapshot Runway antigo, expirado após consumo. Não se inferiu saldo atual nem ausência de créditos: exige preflight oficial novo.

## Imagem do Estúdio e coordenação

A correção do Estúdio foi validada localmente antes da aplicação autorizada. Os cenários de navegador consumiram a imagem construída de `frontend/Dockerfile`, com todas as APIs interceptadas por dependências locais. Nenhum endpoint produtivo foi usado pelos testes. A matriz ampliada, incluindo a orientação no backend, passou novamente em duas rodadas completas; os totais finais estão abaixo.

Imagem local `marketing-hub/frontend:vega92-narrativa-v1`, construída sobre a revisão base `1a15c5944eca27e7556124387ade7452a71eac78`, com fonte corrigida nesta worktree. Identidade local `sha256:d4a354cd43aec7c96d860bc21f33630b43759f5e985de0f64b77fd0fd8af1c1d`. Nenhuma rodada local autorizou produção paga, QA humano ou publicação comercial.

Intervenção `6f6602b97d734d819e32dd23fb60772d`, escopo APP, confirmada ACTIVE sem transação em curso. A primeira carga conservou o frontend anterior: a verificação estrita de ID detectou a mudança de identidade entre image stores. A equivalência deve ser comprovada pelo contrato portátil de `scripts/agent-image-bundle.mjs` antes da aplicação, incluindo todas as camadas e configurações funcionais.

Evidências locais em `artifacts/vega-video-brief-recovery/`.

## Bloqueio externo e projeção do processo

Os projetos #4 (AD/perfil59) e #5 (LANDING_HERO/perfil60) foram criados pela UI. Preflights #12/#13 terminaram `PROVIDER_PREFLIGHT_ONLY_BLOCKED`, código `PROVIDER_ROUTER_CONFIG_MISSING`, HTTP404 para `marketing-hub-campaign-final-v1`. Ambos preservam job/tarefa nulos e custo zero. O histórico #9–#11 teve sucesso com a receita distinta `product_ugc@2026-06` no experimento91; isso não comprova o Router atual, nem permite transportar o vídeo/tela antiga para a v12.

A documentação [Runway Model Router](https://docs.dev.runwayml.com/model-routers/configuration/) exige configuração acessível na organização usada pela integração. Esta sandbox não dispõe de chave Runway, o host191 não tem o arquivo canônico da credencial e o executor publicado está em 177.153.62.107, fora da lista SSH do helper disponibilizada nesta solicitação. MCP segue acessível para dados e logs. Foi solicitado acesso seguro ao ambiente, sem segredos pelo chat.

A conferência revelou recorrência de `LOOP-BPM-DECISAO-HUMANA-COMO-EXECUCAO`: a orientação financeira cobria apenas VIDEO_BRIEF. Após a transição para CAMPAIGN_VIDEO, um preflight bloqueado continuava aparecendo como WAITING_ACTIVITY no pai. Comparação de alternativas: (1) projetar no backend o último preflight da peça/versão exatas, mantendo o contrato userAction: baixo esforço, auditável, escolhida; (2) persistir um alerta de domínio no callback do fornecedor: associação direta útil para notificações, mas exige novo contrato e sincronização de alertas resolvidos; (3) generalizar a projeção para todas as etapas e integrações do motor: maior cobertura futura, com esforço e risco de regressão superiores ao necessário para este impedimento. A primeira usa a auditoria existente e preserva a responsabilidade do backend.

Matriz ampliada **antes dos testes desta correção**: bloqueio do anúncio e da demonstração; isolamento por produto/experimento/versão/papel e marco temporal; tentativa nova ativa prevalece sobre falha antiga; ausência de auditoria não inventa bloqueio; pausa reconhece intervenção sem tratar trabalho ativo como humano; API/card/copiar contexto exibem ação oficial e 0/4 objetivos. SQL executado em banco local, testes de orientação e regressões de automação. A contagem das duas rodadas completas foi reiniciada após a última correção.

A rodada ampliada identificou vazamento de estado no test double recém-adicionado: o reset do ciclo não limpava os retornos sintéticos de preflight. A fixture agora limpa ambos no mesmo comando; nenhuma mudança produtiva adicional foi necessária. A contagem de duas rodadas foi reiniciada após essa correção.

## Resultado final da homologação local ampliada

Rodadas completas consecutivas `final2` e `final3`: **341 testes backend (incluindo ArchUnit e SQL) e 71 testes frontend por rodada, sem falhas ou testes ignorados**. Em cada rodada, os scripts REST verificaram oito grupos financeiros e oito grupos do preflight, além de nove cenários do SQL canônico no MySQL5.7. Navegação de briefing, edição do Estúdio e bloqueio do processo passaram em desktop/iPhone/Pixel. No fluxo novo, a API e o ledger BPM são reais; apenas o fornecedor é simulado. O contexto copiado contém o código, o motivo e a evidência do impedimento. TypeScript, Spotless, Swagger e diff aprovados.

A verificação do pacote comprovou **3.941 classes idênticas às testadas e 415 recursos externos íntegros**, além do smoke dos catálogos dentro do JAR executável. O JAR incorporado pela imagem backend também teve o SHA256 comparado ao pacote validado. Imagem construída exclusivamente por `backend/ads-service/Dockerfile`. Não houve alteração de changelog.

A inspeção prévia do Compose publicado confirmou as mesmas 27 entradas de configuração do backend, volumes e portas. Valores sigilosos são preservados somente na memória do processo remoto; nenhum arquivo de segredo foi exportado. A intervenção APP continua protegida, sem reativação por tempo ou encerramento da sessão.

Alternativas de continuidade externa: configurar o Router da organização existente (menor mudança e esforço, exige acesso seguro, recomendada); produzir pela receita Product UGC que já teve sucesso (exige novas referências da v12 e nova verificação de adequação, não autoriza reusar o vídeo antigo); criar uma rota de captura determinística da experiência com narração (boa fidelidade para demonstração, mas exige integração própria de custos/produção/QA no executor). Não foi escolhido fallback pago sem comprovar custo, contrato e prova visual. A conta precisa ser acessível antes de ajustar a primeira alternativa.

## Inicialização e retorno preservado

A primeira transferência do backend falhou sem trocar containers; a repetição concluiu e comprovou equivalência portátil de camadas e configuração. Na aplicação, `docker compose --wait` interpretou o healthcheck inicial como falha antes da prontidão. O retorno foi acionado e a versão anterior apresentou a mesma demora. MCP confirmou inicialização normal de ambas, em 95,403 e 97,844 segundos, sem erro de aplicação. A comparação descarta regressão funcional da correção como causa desta espera.

A aplicação passou a usar o contrato existente `deploy/bin/backend-health.sh`, idêntico no repositório e no host: verifica processo, reinícios e duas respostas HTTP consecutivas antes de confirmar prontidão. O teste local `scripts/test-backend-health-wait.sh` passou. Nenhum código ou configuração produtiva adicional foi alterado para alongar ou enfraquecer os critérios de saúde.

## Resultado publicado e pendência real

Em 13/09/2026 às 01:55 UTC, a conferência publicada passou em desktop, iPhone e Pixel: card com **“Produção do anúncio bloqueada”**, responsável pela integração, botão para o projeto #4, contexto com evidência do preflight e navegação de retorno. Nenhuma escrita ocorreu nessa conferência; nenhum erro de página ou API foi observado. O backend conciliou e persistiu `WAITING_HUMAN`, 0 concluídas e 4 restantes, com o motivo real. Produto, cadeia, processo75v6, ciclo2, experimento92 e versão v12 foram preservados.

| Entrega | Comprovação / limite |
| --- | --- |
| Briefing dos dois vídeos | Evento12, revisão9, VIDEO_BRIEF → CAMPAIGN_VIDEO, baseado em Íris402 e orçamento11. |
| Roteiros e projetos | Perfis59/60, roteiros560/561 e projetos4/5, criados pela UI. Narrativa reaberta sem alteração implícita. Ainda não são vídeos produzidos/aprovados. |
| Preflight do anúncio | Ciclo12, auditoria5, bloqueio do Router. Job/tarefa nulos; custo conhecido USD0. |
| Preflight da demonstração | Ciclo13, auditoria6, mesmo impedimento. Job/tarefa nulos; custo conhecido USD0. |
| Processo comercial | Run4 permanece0/4. Não houve venda, entrega, conciliação comercial ou encerramento fabricado. |
| Dependência externa | Disponibilizar acesso seguro à conta Runway/configuração ou ao executor pelo helper autorizado. Não enviar chaves pelo chat. Só depois comprovar preflight, custo total e gates da produção/revisão. |

Imagens saudáveis aplicadas no escopo autorizado, sem push ou PR:

- Frontend: `marketinghub-frontend:vega92-narrativa-v1`; ID publicado `sha256:0985554bbbd61265671635fcd9c6b44fbd5f6cb9c7d03adb3b7f6e2dbd8ad445`; retorno `marketinghub-frontend:before-vega92-narrativa-v1`.
- Backend: `marketinghub-backend:vega92-preflight-v2`; ID publicado `sha256:8b2d07b505b314c3af0f83a14c7483ff0269129c4d72bc4f4b200764aebc3bc8`; retorno `marketinghub-backend:before-vega92-preflight-v2`.
- A configuração e os volumes do backend foram preservados. O proxy foi recarregado após a prontidão.
- As evidências selecionadas e os hashes das fontes validadas ficam no [registro versionado](vega-videos-briefing-estudio-v1-evidencias.json). Logs e screenshots completos permanecem em `artifacts/vega-video-brief-recovery/`.

A homologação técnica desta intervenção foi encerrada. A produção dos vídeos permanece impedida por acesso/configuração externos; isso não deve manter o card anunciando execução. A revisão local validada deve ser registrada por `prepare-resume` para retomada automática dos quatro publicadores após integração na main. Não há autorização para retomá-los antes dessa comprovação, nem PR solicitado nesta tarefa.

## Oportunidade comercial, ainda como hipótese

Prioridade alta e esforço baixo de roteiro: demonstrar o primeiro ajuste real usando itens que a cliente já possui, com ocasião, aplicação e critério de autoavaliação visíveis. Evidências: contrato Íris402 e hipótese do ciclo2. Validar primeiros resultados por início, continuidade ao checkout e vendas líquidas; produção de vídeo não comprova receita.

Atividade com valor marginal a rever: transcrever manualmente a mesma comunicação aprovada em briefing, perfil e projeto. A repetição acrescentou risco de divergência sem criar valor para a cliente. Como melhoria futura, preencher rascunhos a partir do contrato persistido, preservando revisão explícita. Esforço médio, impacto operacional esperado; medir tempo até a primeira peça aprovada, retrabalho e custo por peça. Nenhuma nova automação comercial foi incluída fora da recuperação.
