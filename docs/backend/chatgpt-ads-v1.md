# Operação ChatGPT Ads v1

Entrada: `/chatgpt-ads`. Backend: `docs/swagger/chatgpt-ads-swagger.yaml`.
Responsável: Hermes, no container `growth-operator-worker` existente. Não há outro serviço para manter.

## Conexão

1. O titular cadastra a conta em https://ads.openai.com/ e verifica país, moeda, fuso,
   habilitação e faturamento. O Hub não cadastra conta, cartão ou crédito automaticamente.
2. Em https://ads.openai.com/settings, gerar a chave **Advertiser API**. Não é a chave da API de
   modelos, de Codex ou de conversões. Armazenar no secret `OPENAI_ADS_API_KEY` do repositório.
   Nunca colar a chave na conversa ou em arquivo versionado.
3. O secret `CHATGPT_ADS_WORKER_TOKEN`, com pelo menos 32 caracteres aleatórios, autentica
   exclusivamente os requests internos do Hermes. Ambos são provisionados pelos workflows
   versionados, nunca por uma imagem ou publicação manual.
4. A publicação do backend instala o token via configtree; a do Hermes instala os arquivos
   `worker-token` e, quando disponível, `advertiser-key` no diretório protegido
   `/home/operator/.codex/chatgpt-ads`. O volume persistente individual já existente preserva recibos.
   Credenciais ausentes não quebram outros trabalhos de Hermes; o canal permanece desconectado.
5. Clique em **Verificar conexão**. O executor consulta `/ad_account`; a tela mostra somente
   nome, moeda e estado. Trocar a conta exige nova autorização das preparações existentes.

A integração assina método, rota, corpo, horário e nonce com HMAC-SHA256. As respostas também são assinadas e vinculadas ao nonce, impedindo alteração dos comandos em trânsito. O segredo interno
não trafega no HTTP operacional. Assinatura não criptografa conteúdo: HTTPS continua recomendado
para a confidencialidade do painel e do transporte. A API externa usa HTTPS e não segue redirects.
Logs e recibos removem campos sensíveis e eco da chave; exceções preservam stack operacional
sanitizado. Nenhuma chave entra no banco ou na UI.

## Percurso

- Crie um experimento **ChatGPT Ads**, preservando o histórico de outros canais. Não converta uma
  campanha que já foi publicada. Selecione sua peça e prepare contextos, moeda, teto total, lance
  por clique e janela; nenhum desses valores recebe padrão comercial.
- Copy e imagem devem coincidir exatamente com o criativo aprovado. Adaptação ao formato exige
  revisão própria; não basta cortar a copy de Instagram. O teste inicial é Brasil, imagem única,
  clique com lance fixo; vídeo pode estar no destino.
- O preflight produtivo vale no máximo sete dias; alterações no material ou nos limites exigem nova conferência. O run produtivo precisa dos gates de página, compra/entrega, medição e
  `CHATGPT_ADS_READINESS_CONFIRMED`, com evidências. O gate do canal confere conta, imagem/limites
  oficiais, moeda, teto, janela, contexto e rastreamento próprios. Preparar a prova não ativa mídia.
- Registre a autorização própria. **Publicar pausada** cria campanha → conjunto → imagem → anúncio.
  Aguarde a revisão da OpenAI. **Ativar teste autorizado** reconfere conta, material, teto e lance
  efetivos no provedor e a homologação atual. Campanha é ativada por último.
- Hermes consulta a fila a cada 30 segundos. Estado ativo não significa primeira impressão.
  Insights usam horas completas e podem ter atraso. Primeira impressão muda o run; gasto e
  término acionam pausa, além do limite total e encerramento nativos do provedor.
- **Pausar campanha** pode ser usado durante ativação. Operações de ativação não enviadas são
  canceladas; resposta tardia causa pausa compensatória. Falha de pausa fica visível para recuperação.
- Se um callback falhar, o recibo local é reenviado antes de chamar a API novamente. Depois de corrigir
  a causa, **Retomar operação preservada** usa a mesma operação e Idempotency-Key.

## Medição e limites da primeira entrega

Impressões, cliques e gasto são preservados com moeda e horário; ausência é desconhecida, não zero.
Parâmetros UTM e `oppref` vão ao destino. Compras, entrega, reembolso e contribuição continuam no
relatório canônico do experimento. Não se cria compra a partir de clique nem de um insight externo.
Não somar BRL a USD sem câmbio rastreável; esta integração não faz conversão automática.
O leitor canônico de aquisição projeta o gasto em BRL no cockpit, custo e aprendizado,
uma única vez, separado das métricas históricas da Meta. Em outra moeda, a tela preserva
o valor nativo e informa a pendência de câmbio; totais e contribuição ficam desconhecidos.
Após a pausa, Hermes coleta uma fotografia cujo intervalo cubra a confirmação da parada.
Isso preserva gasto e motivo para o fechamento, sem chamar clique de compra.

Pixel e Conversions API não estão implementados nesta v1. Assim, não há otimização por compra na
OpenAI nem promessa de atribuição individual de vendas ao canal. A integração usa campanha de
cliques; vendas do experimento permanecem evidência distinta. Recursos de múltiplas imagens,
feeds, conversões otimizadas e alterações externas de orçamento não são operados por esta versão.

## Homologação local

- Backend: `ChatgptAdsServiceTest`, `ChatgptAdsAuthenticationTest` e regressões de experimento/run.
- Executor: `ChatgptAdsWorkerTest` e suíte existente, usando servidor HTTP local e chaves sintéticas.
- Banco: `CHATGPT_COMPOSE_PROJECT=<projeto-exclusivo> python3 infra/testing/chatgpt-ads/run-mysql.py`.
- Sandbox com memória limitada: execute `npm run typecheck` e depois `vite build --config vite.learning-cycles-local.config.ts`, que omite sourcemaps. O CI gera o bundle produtivo completo.
- Tela: Vite com `vite.learning-cycles-local.config.ts`, porta 4187; executar
  `node infra/testing/chatgpt-ads/browser.cjs`. Todas as chamadas são interceptadas por fixtures.
- Nenhum desses testes consulta conta real, gera cobrança, cria campanha real ou simula demanda.
