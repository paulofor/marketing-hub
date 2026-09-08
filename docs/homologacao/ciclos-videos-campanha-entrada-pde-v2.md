# Homologação local — vídeos nos ciclos PDE v2

## Escopo e matriz definida antes dos testes

O BPM v2 delega criação ao Estúdio existente e confere evidências no backend. Não gera mídia paga,
não chama providers e não modifica a operação de Vega, Mira ou experimentos reais.

| Área | Critérios da rodada completa |
| --- | --- |
| Caminho feliz | Aprendizado → planejamento → ajuste → briefing → AD → LANDING_HERO → integração → homologação → autorização → publicação → medição → decisão |
| Evidência | Dois ativos reais distintos, prontos, aprovados, com áudio e URLs válidas; anúncio elegível; contrato PDE com identidade e versão exatas |
| Falhas | Ausência, vídeo reprovado, finalidade errada, produto/experimento divergente, versão ou URL trocada e aprovação revogada bloqueiam avanço |
| Retrabalho | Falha preserva histórico e retorna à correção; sucessor recebe conhecimento sem reaproveitar aprovação vencida |
| Compatibilidade | BPM v1 e referências históricas continuam intactos; v2 em novos ciclos PDE; idempotência e concorrência preservadas |
| Integrações | API e persistência locais reais; Estúdio, provedores, agentes, publicação e ativos usados como dependências de teste explícitas |
| Observabilidade e métricas | Orientação clara, eventos com referências e causa; funil e receita separados de reprodução; dados de teste segregados |
| UI | Formulários, links, erros, carregamento e ausência de overflow em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Migração | MySQL 5.7: aplicação, idempotência, reversão, reaplicação e preservação do processo anterior |
| Regressão | Testes backend/frontend relevantes, TypeScript, build, formatação e revisão do diff |

Uma rodada completa sem defeito conclui a homologação. Após qualquer correção revelada pela
matriz, executar duas rodadas completas consecutivas sem falha. Safari nativo e geração por
provider pago não são simulados como aprovações reais.

## Ajustes confirmados durante a homologação

A primeira rodada completa passou com API/MySQL reais, 2.476 testes backend executados,
frontend e três dispositivos. A revisão adicional usando o próprio `PdeProductionSlotService`
reproduziu um bloqueio indevido: publicar completa `experienceVersion` e `layoutKey`, enquanto
uma assinatura do rascunho ainda incompleto tratava esses campos como mudança comercial.
O teste falhou com HTTP 409; a conferência agora compara a representação normalizada do
publicador. Identidade explicitamente divergente, conteúdo alterado e JSON inválido continuam
bloqueados com orientação de correção. A assinatura identifica metadados e contratos; não
alega download ou inspeção dos bytes do provider.

A migração também retira v1 do catálogo operacional, preservando suas definições e ocorrências.
O verificador físico exige uma única versão atual (v2 publicada, v1 no histórico), inclusive
depois de rollback e reaplicação.

A matriz final usa `--video-matrix`: executa os testes dos ciclos, BPM, posição na cadeia,
slots PDE, vídeo/criativo e gate multiagente; frontend, API, browser e migração continuam completos.
A regressão ampla anterior não substitui as duas rodadas finais após essa correção.

## Reprodução local

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-d6fcf2ee-3705-4d3a-b76a-de46fcc84cb2-1bd42872e1 \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh --video-matrix
```

## Resultado final

Duas rodadas completas e consecutivas aprovadas, com os 18 arquivos de implementação idênticos
(SHA-256 conferido) entre elas.

| Controle | Rodada 1 | Rodada 2 |
| --- | ---: | ---: |
| Etapas da matriz local | 18/18 | 18/18 |
| Testes backend executados | 207 | 207 |
| Testes frontend | 515 | 515 |
| Contratos específicos do gate de vídeo (incluídos no backend) | 29 | 29 |
| Grupos REST com MySQL 5.7 | 13 | 13 |
| Jornadas com interface real e API local | 12 | 12 |
| Dispositivos | Desktop, iPhone, Pixel | Desktop, iPhone, Pixel |
| Erros no navegador / chamadas externas da jornada | 0 / 0 | 0 / 0 |
| Aplicação, rollback, reaplicação e idempotência | Aprovados | Aprovados |
| Remoção da topologia | Confirmada | Confirmada |

Cada relatório backend contém 209 casos, com duas exclusões preexistentes de
`VideoCreativeControllerTest`; nenhum contrato novo ficou ignorado. A suíte frontend mantém avisos
legados do jsdom/conexões locais simuladas, sem falha de teste; os banners globais de conta nas
capturas vêm dos dados vazios da fixture e não representam o estado das contas produtivas.
ShellCheck do runner, sintaxe Bash, Actionlint do workflow Liquibase com a política ShellCheck do
repositório e revisão do diff também passaram. A chamada exploratória ao Actionlint global padrão
não é check desta entrega: a versão instalada 1.7.12 não aceita `queue`, para o qual o repositório
já possui instalador de revisão específica; nenhum workflow foi alterado por causa desse diagnóstico.

Evidências: [resultado estruturado e assinaturas](evidencias/ciclos-videos-campanha-entrada-pde-v2/resultado.json),
[desktop](evidencias/ciclos-videos-campanha-entrada-pde-v2/desktop-videos.png),
[iPhone](evidencias/ciclos-videos-campanha-entrada-pde-v2/iphone-videos.png) e
[Pixel](evidencias/ciclos-videos-campanha-entrada-pde-v2/pixel-videos.png).

Nenhum experimento produtivo, campanha, gasto, vídeo pago, PR, commit ou deploy foi criado.
O job existente `validate-learning-sales-cycles` no workflow Liquibase também executará a fixture
incremental quando o usuário publicar o PR.

## Operação após a publicação

1. Abrir **Cadeias de Valor → Ciclos de aprendizado e vendas** e iniciar um ciclo para o experimento
   planejado do produto. Referências históricas como #91 começam pela conciliação; ocorrências
   anteriores continuam na definição original.
2. Depois do ajuste útil, seguir **Definir os dois vídeos**: registrar objetivo, CTA, métrica,
   variável principal e referência do teto. A autorização do Estúdio permanece própria.
3. Usar **Produzir no Estúdio** com produto/experimento informados na tela; selecionar os ativos
   `AD` e `LANDING_HERO` prontos nas atividades respectivas.
4. Aprovar os ativos e o criativo pelo fluxo oficial; vincular o vídeo de entrada em `heroVideos`
   no contrato em rascunho da versão PDE; registrar a evidência independente e técnica solicitada.
5. Homologar o conjunto, autorizar e publicar pelos caminhos oficiais. Divergência retorna à
   correção; a comparação aceita a normalização canônica e bloqueia troca real de conteúdo.
6. Medir o funil e a receita deste experimento e preservar referências dos vídeos no aprendizado
   para o sucessor. Publicar o BPM não produz automaticamente os dois vídeos.
