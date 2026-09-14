# Radar IA para vídeo — 2026-09-14

## Resumo executivo

Três movimentos merecem registro nesta rodada:

1. **Socialive Catalyst** entrou em **early access limitado** em 14/09/2026 como uma camada de orquestração para produção de vídeo empresarial. O diferencial não é um novo renderer, mas o fluxo `prompt → plano de projeto → assets/regras → geração → produção/aprovação/publicação`, com lógica de decisão e failsafes antes de gastar geração.
2. **Amazon Nova Reel** está oficialmente em estado **Legacy** no Amazon Bedrock e tem **EOL em 30/09/2026**. Não deve ser tratado como opção estratégica nova, apesar da qualidade histórica e dos workflows ainda documentados pela AWS.
3. **fal H3 Max/Director** continua tecnicamente muito competitivo em velocidade, mas sai do preço promocional: o Director passa para **US$0,08/s** e H3 Max/Turbo passam aos preços de lista a partir de 15/09. A mudança reduz bastante a vantagem econômica observada no lançamento, embora Turbo continue barato frente a vários concorrentes.

## 1. Socialive Catalyst — lançamento de uma camada de orquestração audiovisual

**Status:** 🟡 Early access limitado. Clientes selecionados começam a receber acesso em setembro; disponibilidade geral é planejada para outubro de 2026. Não há API pública específica nem preço público específico do Catalyst no anúncio.

A Socialive anunciou em 14/09/2026 o Catalyst, descrito como um mecanismo de criação de vídeo por prompt que orquestra capacidades já existentes da plataforma. O sistema começa por desenvolver um **plano de projeto** em diálogo com o usuário e reúne informações, assets, requisitos de marca e direção criativa **antes** da geração. A empresa também afirma que usa **decision logic** e **rule-based failsafes** para reduzir aleatoriedade, revisões e gerações desnecessárias.

Capacidades declaradas para o Catalyst incluem:

- criação por linguagem natural;
- geração de ideias, tópicos, roteiros, visuais e clipes;
- transformação de documentos e mídia existente em vídeo;
- automação de templates e requisitos de marca;
- reaproveitamento de conteúdo longo em novos ativos;
- visão futura de encaminhar conteúdo por compliance/aprovação e publicar em sistemas empresariais.

### Por que isso importa

O desenho reforça uma tendência recorrente neste radar: o valor competitivo está migrando do modelo isolado para o **harness** que decide o que gerar, com quais referências, sob quais regras e em que momento do fluxo.

Para o Marketing Hub, a parte mais útil é o padrão **planejar antes de gerar**. Um preflight pode resolver objetivo, oferta, CTA, assets obrigatórios, claims, invariantes de marca, formato, duração e orçamento antes da primeira chamada cara ao modelo. Isso é uma hipótese operacional; a Socialive ainda não publicou benchmark independente provando redução de custo ou aumento de produtividade.

**Fonte oficial:** https://socialive.us/resources/socialive-catalyst-announcement

## 2. Amazon Nova Reel — encerramento confirmado

**Status:** 🟠 Legacy / encerramento anunciado. **EOL: 30/09/2026**.

A documentação oficial do Amazon Bedrock lista `amazon.nova-reel-v1:0` e `amazon.nova-reel-v1:1` como **Legacy**, com fim de vida em 30 de setembro de 2026. Pelas regras de lifecycle da AWS, modelos Legacy não devem receber novas integrações: novos clientes não podem começar a usá-los e clientes existentes precisam migrar antes do EOL; a migração não é automática.

Isso é importante porque Nova Reel teve valor histórico como opção integrada ao Bedrock para texto/imagem → vídeo, controles de câmera e vídeos multi-shot de até 2 minutos no Reel 1.1. Esses méritos históricos não mudam o fato de que ele **não é mais uma escolha adequada para nova arquitetura**.

Não encontrei na documentação oficial atual um sucessor Amazon-branded de geração de vídeo apresentado como substituição direta do Nova Reel. Para novas integrações, o Marketing Hub deve manter o renderer desacoplado do provider e priorizar modelos ativos com horizonte de suporte mais claro.

**Fontes oficiais:**
- https://docs.aws.amazon.com/bedrock/latest/userguide/model-card-amazon-nova-reel.html
- https://docs.aws.amazon.com/en_en/bedrock/latest/userguide/model-lifecycle-legacy.html

## 3. fal H3 Max / H3 Max Turbo / Director — preço promocional termina

**Status:** 🟢 Ativos. Director é público via WebRTC; H3 Max/Turbo são APIs serverless de geração. O modelo-base MiniMax H3 permanece separado e tem pesos abertos sob a licença da MiniMax.

A página oficial da fal registra o fim da promoção de lançamento de 75%:

| Endpoint | Preço promocional | Preço de lista |
| --- | ---: | ---: |
| H3 Max Turbo 768p | US$0,01/s | **US$0,04/s** a partir de 15/09 |
| H3 Max 768p | US$0,02/s | **US$0,08/s** a partir de 15/09 |
| H3 Max Director | US$0,02/s | **US$0,08/s** a partir de 14/09 |

O Director tem piso de cobrança de 60 segundos, portanto uma sessão mínima ao preço de lista passa a custar **US$4,80**. A fal continua declarando geração acima de tempo real para H3 Max Turbo em vários tamanhos e resoluções; por exemplo, no benchmark do próprio fornecedor, 15 s em 768p levam 8,44 s de inferência no Turbo. Esses números são medições do fornecedor e não equivalem a latência ponta a ponta em qualquer aplicação.

Mesmo depois do aumento, o H3 Max Turbo ainda permanece competitivo em custo para workflows que valorizam geração muito rápida. O ponto para o Marketing Hub é não projetar orçamento futuro usando o preço promocional observado nas primeiras semanas de setembro.

**Fontes oficiais:**
- https://fal.ai/minimax-h3-max
- https://fal.ai/h3-max-director

## Comparação operacional

| Sistema | Status | Papel principal | Integração | Observação atual |
| --- | --- | --- | --- | --- |
| Socialive Catalyst | 🟡 early access | harness/orquestração empresarial de vídeo | sem API pública específica | planeja e aplica regras antes da geração |
| Amazon Nova Reel | 🟠 Legacy / EOL 30/09 | geração de vídeo no Bedrock | API ainda existente para clientes elegíveis | não iniciar integração nova |
| H3 Max Turbo | 🟢 ativo | geração rápida com áudio | API fal | preço de lista 4× o promocional a partir de 15/09 |
| H3 Max Director | 🟢 ativo | stream contínuo dirigível | WebRTC/API fal | preço de lista US$0,08/s e piso de 60 s |

## Implicação para o Marketing Hub

A mudança mais reaproveitável desta rodada não é escolher Socialive como fornecedor. É incorporar ao próprio harness o padrão:

`briefing → preflight → plano de cenas → validação de regras/assets/orçamento → escolha do renderer → geração → avaliação`

Isso reduz dependência de qualquer modelo específico e também protege contra eventos como o encerramento do Nova Reel ou mudanças bruscas de preço como as do H3 Max.

## Card criado

Foi criado um único card porque apenas o padrão de **planejamento pré-geração com failsafes** é suficientemente reutilizável para orientar Apolo em execuções futuras. O encerramento do Nova Reel e o aumento de preço do H3 são fatos operacionais importantes, mas são temporais e não justificam, por si só, um card de orientação de produção.

- `cardKey`: `video-planejamento-pre-geracao-failsafes`
- JSON: `pesquisas/video/cards/2026-09-14-planejamento-pre-geracao-failsafes.json`
- Fonte revisada: `pesquisas/video/cards/fontes/2026-09-14-planejamento-pre-geracao-failsafes.md`
- SHA-256 da fonte: `dfe6e8b89a40703d75d974761146b5bd7cd79078b065185f0a64bf021a54b7f1`
