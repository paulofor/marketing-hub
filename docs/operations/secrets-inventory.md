# Inventario central de secrets por servico/deploy

Data de criacao: 2026-07-18

Este inventario existe para evitar containers subirem com variaveis sensiveis vazias ou configuracoes incompletas. Ele registra nomes, responsabilidade e validacao operacional, nunca valores reais de credenciais.

## Regra operacional

- Nunca versionar valores reais de secrets em Markdown, compose, `.env`, logs, prints ou mensagens.
- Todo deploy com receita, lead, e-mail, IA, pagamento, anuncio ou checkout deve ter os secrets conferidos antes de publicar.
- Variavel sensivel obrigatoria vazia deve bloquear o deploy, mesmo que o health HTTP do container responda `UP`.
- Quando possivel, preferir arquivo montado somente leitura em `/run/secrets/...` ou secret do provedor de deploy em vez de valor direto em `.env`.
- Toda mudanca de secret em producao deve preservar backup do `.env` remoto antes da alteracao.
- Depois do deploy, validar o fluxo de negocio que depende do secret, nao apenas o container.

## Checklist pre-deploy

1. Confirmar host, usuario SSH, pasta de deploy e compose usado.
2. Conferir se todas as variaveis obrigatorias do servico existem no ambiente remoto.
3. Conferir que nenhuma variavel obrigatoria esta vazia quando o transporte/provedor estiver habilitado.
4. Recriar apenas os containers afetados quando a imagem nao mudou.
5. Validar health tecnico.
6. Validar acao comercial real: envio de e-mail, geracao de imagem, chamada OpenAI, pagamento, publicacao de anuncio ou login.
7. Registrar resultado e, se houver falha, corrigir causa-raiz antes de considerar concluido.

## Inventario

| Servico/deploy | Host | Compose/pasta | Secret/configuracao | Obrigatorio quando | Validacao pos-deploy | Impacto se ausente |
|---|---:|---|---|---|---|---|
| PDE Platform backend / Clube MUSA | `191.252.102.54` | `pde-platform/docker-compose.deploy.yml` | `PDE_PLATFORM_BACKEND_IMAGE` | sempre em deploy por imagem | container `pde-platform-backend` sobe com a imagem esperada | backend nao inicia ou sobe versao errada |
| PDE Platform frontend / Clube MUSA | `191.252.102.54` | `pde-platform/docker-compose.deploy.yml` | `PDE_PLATFORM_FRONTEND_IMAGE` | sempre em deploy por imagem | `https://clubemusa.com.br` responde e primeira dobra carrega | experiencia do lead fica indisponivel ou desatualizada |
| PDE Platform homologação / Clube MUSA | `191.252.102.54` | `pde-platform/docker-compose.homolog.yml` | `PDE_PLATFORM_BACKEND_IMAGE`, `PDE_PLATFORM_FRONTEND_IMAGE`, `PDE_AI_WORKER_IMAGE`, `PDE_PLATFORM_HOMOLOG_HERO_VIDEO_URL` | validar PDE antes de produção nas portas `5177` e `8097` | `http://191.252.102.54:5177` responde com `musa-pde-entry-v4-video-hero` e backend `:8097/actuator/health` saudável | alteração pode ir para produção sem validação visual/funil em ambiente isolado |
| PDE Platform backend / Clube MUSA | `191.252.102.54` | `.env` remoto do PDE | `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME`, `PDE_ACCESS_JDBC_PASSWORD` | quando usar banco externo para acessos | solicitar acesso, reiniciar backend e confirmar persistencia do acesso | lead solicita acesso, mas estado/login pode falhar |
| PDE Platform backend / Clube MUSA | `191.252.102.54` | `.env` remoto do PDE | `PDE_MAIL_TRANSPORT=ses`, `PDE_MAIL_AWS_REGION`, `PDE_MAIL_FROM` | producao com envio real por SES | chamada de magic link retorna `deliveryStatus: SENT` | lead nao recebe link e abandona entrada do produto |
| PDE Platform backend / Clube MUSA | `191.252.102.54` | `.env` remoto do PDE | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` quando temporario, `AWS_REGION` | `PDE_MAIL_TRANSPORT=ses`; o compose de deploy bloqueia `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` vazios | teste real de primeiro acesso pelo dominio publico | SES falha mesmo com dominio verificado |
| PDE Platform backend / Clube MUSA | `191.252.102.54` | `.env` remoto do PDE | `PDE_GOOGLE_CLIENT_ID` | login Google habilitado | botao Google inicializa sem erro de client id | login social quebra ou some |
| PDE Platform frontend / Clube MUSA | `191.252.102.54` | runtime/env do frontend | `VITE_GOOGLE_CLIENT_ID`, `VITE_MUSA_CHECKOUT_URL` | login Google ou checkout habilitados | mobile mostra login/checkout correto; paywall abre checkout Pepper ativo | lead nao consegue entrar ou comprar |
| PDE Platform AI Worker / Clube MUSA | `191.252.102.54` | `pde-platform/docker-compose.deploy.yml` | `OPENAI_API_KEY_HOST_FILE=/root/infra/openai-token/openai_api_key` montado em `OPENAI_API_KEY_FILE=/run/secrets/openai_api_key`; `OPENAI_API_KEY` apenas como fallback | orientacoes da Consultora MUSA nos 7 dias | arquivo fisico existe no host, worker processa pendencia de IA e registra resultado no backend | cliente recebe PDE sem apoio personalizado da IA, reduzindo valor percebido e ativacao |
| MarketingHub backend principal | `191.252.181.168` | `deploy/docker-compose.yml` | `OPENAI_API_KEY_FILE` ou `OPENAI_API_KEY` | fluxos com OpenAI/modelos oficiais | acao de IA responde e logs nao indicam chave ausente | pipeline nao gera ativos comerciais |
| MarketingHub backend principal | `191.252.181.168` | `deploy/docker-compose.yml` | `OPENAI_API_KEY_HOST_FILE` | uso de arquivo montado | arquivo existe no host e monta em `/run/secrets/openai_api_key` | backend sobe, mas chamadas de IA falham |
| AI Worker | host do worker conforme deploy ativo | `ai-worker/docker-compose.yml` | `OPENAI_API_KEY_FILE` ou `OPENAI_API_KEY` | qualquer etapa de IA do worker | job de IA conclui e registra request/response | campanhas, landings e ativos param na fila |
| AI Worker | host do worker conforme deploy ativo | `ai-worker/docker-compose.yml` | `GOOGLE_API_KEY`, `GOOGLE_SEARCH_ID` | etapas que usam busca Google | etapa dependente de busca retorna evidencias | hipotese sem evidencia ou falha de pesquisa |
| Gerador/imagens via backend principal | `191.252.181.168` | `deploy/docker-compose.yml` | `OPENAI_API_KEY_FILE` ou `OPENAI_API_KEY` | `http://191.252.181.168:5173/ai/image-generator` | gerar imagem e confirmar versoes `original`, `web` e `mobile` | PDE fica sem visual forte ou usa asset pesado |
| Video Management | `177.153.62.107` ou host configurado | `deploy/docker-compose.yml` | `GEMINI_API_KEY_HOST_FILE=/root/infra/gemini-token/gemini_api_key`, `LUMA_API_KEY_HOST_FILE=/root/infra/luma-token/luma_api_key`, `KLING_API_KEY_HOST_FILE=/root/infra/kling-token/kling_api_key`, `HEYGEN_API_KEY_HOST_FILE=/root/infra/heygen-token/heygen_api_key`, `RUNWAY_API_KEY_HOST_FILE=/root/infra/runaway-token/runaway_api_key`, arquivos internos `GEMINI_API_KEY_FILE`, `LUMA_API_KEY_FILE`, `KLING_API_KEY_FILE`, `HEYGEN_API_KEY_FILE`, `RUNWAY_API_KEY_FILE`, `VIDEO_PROVIDERS_HEYGEN_AVATAR_ID`, `VIDEO_PROVIDERS_HEYGEN_VOICE_ID` | providers VEO, Luma Ray, Kling, HeyGen ou Runway habilitados | job do provider avanca sem erro de autenticacao e `/api/status` nao revela credenciais | videos de venda nao renderizam ou fallback de criativo fica indisponivel |
| Facebook Ads Worker | `191.252.120.96` ou host configurado | `facebook-ads-worker/docker-compose.yml` | credenciais Meta/tokens armazenados no backend, `GHCR_TOKEN` para build/deploy quando aplicavel | publicacao/sincronizacao Meta | publicar campanha de teste controlado ou validar chamada Graph | campanha nao publica ou metricas nao sincronizam |
| Lead Portal backend | host conforme deploy ativo | `lead-portal/docker-compose.yml` | `LEAD_PORTAL_STORAGE_SECRET_ACCESS_KEY` | armazenamento privado de assets habilitado | upload/leitura de asset protegido | pre-venda perde midias e provas visuais |
| Lead Portal Payments Service | host conforme deploy ativo | compose/deploy do servico | `MERCADO_PAGO_ACCESS_TOKEN` | checkout Mercado Pago habilitado | criar preferencia de pagamento em sandbox/producao conforme ambiente | lead quer comprar, mas checkout falha |
| Email Service | host conforme deploy ativo | compose/deploy do servico | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` e flags SMTP/TLS | envio transacional pelo servico de e-mail | envio para caixa de teste ou sandbox SMTP | notificacao comercial nao chega |

## PDE Platform / Clube MUSA

O caso que motivou este inventario foi o Clube MUSA. O container estava tecnicamente ativo, mas o envio de magic link falhava porque as credenciais AWS usadas pelo Amazon SES estavam vazias no runtime.

Validacao minima apos qualquer alteracao no PDE:

```bash
curl -sS https://clubemusa.com.br/api/pde/access/magic-link \
  -H 'Content-Type: application/json' \
  --data '{"email":"teste+<jobId>@sandbox.local"}'
```

Resultado esperado em ambiente com envio real habilitado: `deliveryStatus` igual a `SENT`.

## Politica de defaults

Defaults sensiveis em arquivos versionados devem ser tratados como legado a remover. Para novos servicos, secrets obrigatorios nao devem ter valor real como fallback. Use falha explicita, placeholder vazio ou montagem via arquivo/secret.

## Alternativas avaliadas

1. Inventario em README de cada modulo: simples, mas espalha a verificacao e aumenta a chance de esquecimento.
2. Inventario central versionado: melhor para operacao, auditoria e prevencao de recorrencia, sem expor valores.
3. Script automatico de auditoria de secrets: mais forte, mas exige padronizar todos os deploys antes.

Decisao atual: manter inventario central agora e evoluir depois para auditoria automatica de `.env` remoto sem imprimir valores.
