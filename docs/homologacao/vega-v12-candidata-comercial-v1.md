# Homologação local — Vega v12 como candidata comercial única

## Objetivo

Alinhar o experimento #92 à versão
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`, preservando a v7 publicada até que a mesma v12
passe por homologação e promoção. Nenhuma etapa local publica imagem, contrato, campanha ou gasto.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Trocar apenas os textos da tela | rápida | banco e runtime continuariam divergentes | rejeitada |
| Marcar a v12 como publicada na migração | coloca a versão no catálogo imediatamente | contorna homologação e pode expor entrega incompleta | rejeitada |
| Criar a candidata v8 e promover somente após gates | preserva histórico, mesma identidade e rollback | exige contrato, integridade e testes coordenados | escolhida |

## Matriz definida antes da execução

| Área | Caminho feliz | Validação/falha | Evidência esperada |
| --- | --- | --- | --- |
| Banco MySQL 5.7 | cria uma v8 `CANDIDATE` ligada ao #92 | reaplicação não duplica; candidata já promovida não é sobrescrita | v7 inalterada, snapshot v12 ainda nulo |
| Contrato PDE | v12 entrega R$ 67, checkout, hero #42 e kit próprio | qualquer caminho `musa-v7` reprova | testes do catálogo e arquivos materiais existentes |
| Vídeos | #41 AD e #42 LANDING_HERO prontos/aprovados | ausência, reprovação ou hero divergente bloqueia | política backend lista a causa |
| Promoção | comando prepara a candidata alinhada para `READY` | `CANDIDATE` não publica; divergência retorna HTTP 409 | nenhuma ativação ou campanha criada |
| Frontend | card mostra v8 e ação de preparação no momento correto | publicador permanece desabilitado antes de `READY` | teste React e navegação desktop/mobile |
| Jornada | anúncio → v8 → primeiro ajuste → oferta → checkout → acesso | QA segregado, falhas de URL/checkout permanecem bloqueadoras | Playwright com backend/contrato locais |
| Observabilidade | pendências aparecem juntas e em linguagem de negócio | JSON inválido registra slot, produto, versão e stack trace | teste de serviço/log e visão consolidada |
| Métricas | `experienceVersion` v12 e tráfego interno separados | nenhum evento local conta como venda ou tráfego humano | contrato de integração e testes locais |

## Critérios de conclusão

- v8 nasce candidata com rascunho v12 e sem `published_experience_json`.
- experimento #92 recebe o checkout já canônico de R$ 67, sem mudar orçamento ou status.
- contrato, fallback e arquivos públicos usam somente `/materials/musa-v12/` para o kit v12.
- backend impede publicação antes de URL, oferta, dois vídeos, kit e homologação coincidirem.
- frontend executa o comando oficial de preparação e não oferece publicação prematura.
- testes do backend, PDE, frontend, build, MySQL 5.7 e navegação local ficam aprovados.

## Resultado

Homologação local concluída em 16/09/2026:

- 49 testes do backend principal aprovados para slots, gates, visão consolidada, endpoint e
  igualdade semântica entre o contrato empacotado e a semente SQL;
- dois testes Liquibase aprovados no MySQL 5.7 real, incluindo idempotência e preservação da v7;
- testes do catálogo PDE aprovados e builds dos dois frontends concluídos;
- três testes React aprovados para a tela administrativa;
- seis cenários Playwright aprovados com backend e banco locais reais: Chromium desktop, iPhone
  15 Pro e Pixel 7;
- o primeiro ajuste da v12 continuou determinístico, sem fila de IA, e os eventos de QA ficaram
  segregados por `experienceVersion`;
- alteração posterior de URL, versão, artefato ou vínculo comercial apaga a evidência de
  validação e exige nova homologação.

A URL externa `v8.clubemusa.com.br` não foi publicada nem validada em produção. O gate mantém a
v12 como `CANDIDATE` e o experimento #92 como `PLANNED` até o PR, o deploy, a validação da URL e a
confirmação externa da campanha. Nenhum dado ou serviço de produção foi alterado.
