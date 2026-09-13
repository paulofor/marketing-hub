# Vega — contrato de entrada Gen-4.5

Data: 13/09/2026. Preservar produto 4, cadeia 14/v14, processo 75/v6,
ciclo de aprendizado 2, experimento 92 e versão
`musa-pde-entry-v12-primeiro-ajuste-aplicavel`. Projetos de vídeo 4/5, perfis 59/60.

## Causas confirmadas

A configuração `marketing-hub-campaign-final-v1` não existia na conta Runway.
O acesso ao executor estava liberado, o PR #5183 integrado e a intervenção
anterior em `RELEASED`. O plano de dois clipes de até dez segundos já aparecia
na aplicação publicada. A configuração versionada foi conferida contra a `main`,
validada na matriz local existente (448 testes, contratos e três navegadores) e
cadastrada sob a intervenção `4f22ad2cf2ce4d839bb5d34b1a6f7ce9`, escopo `app`.

O novo preflight do anúncio, ciclo de produção 14/preflight 7, foi solicitado
pela tela e retornou `PROVIDER_NO_ELIGIBLE_MODEL`. Não criou reserva, tarefa de
agente ou geração. O experimento e as tentativas anteriores permaneceram intactos.

O código enviava `negativePrompt` em todo request do Router e juntava o contexto
comercial inteiro em um prompt de até 20.000 caracteres. Simulações oficiais com
as mesmas entradas isolaram dois filtros: retirar somente `negativePrompt` passa
por `input_support`, mas ainda falha em `prompt_length`; um prompt curto com o
mesmo áudio desligado, resolução e duração passa quando `negativePrompt` está
ausente. Com o campo presente, continua falhando. O Gen-4.5 aceita no máximo
1.000 unidades UTF-16 no prompt; o schema universal do Router não garante a
compatibilidade de cada opção com todos os modelos.

Fontes: [schema oficial da API](https://docs.dev.runwayml.com/api/),
[dry run sem cobrança](https://docs.dev.runwayml.com/model-routers/generating/).
Recibos completos e consultas MCP: `artifacts/vega-status-20260913/`.
As simulações são evidência técnica, não aprovação de qualidade nem resultado comercial.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Rejeitar todo contexto longo e exigir reescrita manual | Preserva integralmente as entradas | Transfere retrabalho recorrente ao operador; esforço operacional alto | Não |
| Selecionar outro modelo com contrato mais amplo | Pode aceitar o contexto atual | Muda a escolha técnica, custo e requisitos de QA; esforço médio | Não |
| Montar direção visual por clipe, com prompt versionado e validação de tamanho | Preserva o modelo e separa direção visual de roteiro e auditoria | Alteração localizada; precisa impedir truncamento de regras | Escolhida |

## Matriz definida antes da correção

- Reproduzir localmente a rejeição com o factory, service, cliente e poller reais,
  backend/provedor simulados e os filtros observados no contrato oficial.
- Caminho feliz: fila `pending` → dois dry runs de 10s e 5s → callback `READY`,
  custo simulado coerente, SHA do payload, rotas e snapshot auditáveis.
- Retirar a restrição técnica não suportada sem remover as orientações de
  estabilidade, segurança visual e pós-produção do prompt.
- Não truncar silenciosamente direção visual que ainda ultrapasse 1.000 unidades
  UTF-16: bloquear antes do dry run, com código e causa auditáveis.
- Manter roteiro completo, contexto do projeto, identidade e contrato comercial
  no backend; o prompt do clipe contém somente a direção visual pertinente.
- Cobrir falta de configuração, quotas, teto insuficiente, resposta inválida,
  reservas, receitas existentes e proibição de geração durante o preflight.
- Executar a matriz compartilhada de backend, executor e frontend, com Chromium
  desktop, iPhone 15 Pro e Pixel 7 emulados; duas rodadas completas consecutivas
  após a última correção. Fixtures e APIs simuladas permanecem locais.
- Empacotar e conferir a imagem construída pelo Dockerfile do repositório, sem
  segredos, antes de qualquer troca do executor publicado. Homologar o callback
  de preflight usando a imagem local e dependências locais.
- Só então reaplicar a recuperação autorizada, conferir os dois projetos pela
  tela/MCP/logs e preparar a retomada vinculada à revisão exata validada. Não
  iniciar geração paga nem considerar o processo comercial concluído.

## Resultado

Duas rodadas completas consecutivas aprovadas: `gen45complete1` e
`gen45complete2`. Cada uma executou 252 testes de backend (mais dois testes
opcionais de navegador não habilitados nessa suíte), 160 de executor e 39 de
frontend: **451 testes executados por rodada**, sem falhas. Além dos testes,
passaram os contratos de duração/schema/teto, ArchUnit, conferência do diff,
Chromium desktop e iPhone 15 Pro/Pixel 7 emulados e o callback da imagem real
em rede Docker interna sem saída para provedores.

A imagem contém as mesmas 133 classes testadas, byte a byte, e o prompt
versionado. SHA-256 do JAR:
`754d24bc6e03443461f8b524c13b5c3bf36db5f61c9b3ae90374040ffc827ed9`.
A configuração da simulação usa IDs 91007/91014 e slug `qa-gen45`, sem chaves,
clientes ou receita reais. Os arquivos entram nos containers por `compose cp`,
pois o filesystem da engine não compartilha os bind mounts da sandbox. Todas
as topologias foram removidas com o projeto exclusivo, volumes e órfãos.

Reprodução: construir `video-management-service/Dockerfile`, preparar o
frontend local servido em 4173 conforme `infra/testing/runway-clip-plan`,
exportar `GEN45_IMAGE` e `GEN45_COMPOSE_PROJECT` da sandbox e executar
`bash infra/testing/runway-gen45/run-round.sh <rodada>`. Evidências detalhadas:
`artifacts/runway-access-recovery/gen45complete1/` e `gen45complete2/`.

Os quatro requests exatos dos projetos 4/5 foram aceitos pela API oficial em
`dryRun: true`, HTTP 200, modelo `gen4.5`: 120 créditos para 10s e 60 para 5s,
180 créditos estimados por vídeo. Prompts de 991/981 e 951/942 unidades UTF-16.
Isso é estimativa dos clipes, não custo total nem aprovação da produção.
O catálogo persistido confirma modelo ativo e gates técnicos/econômicos
verificados. Não houve geração nem reserva financeira.

Aplicação no executor e confirmação final pela tela ainda pendentes neste
registro de revisão local. O processo comercial não foi concluído.
