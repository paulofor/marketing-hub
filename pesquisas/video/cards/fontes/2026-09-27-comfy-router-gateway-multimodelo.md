# Fonte revisada — Comfy Router unifica acesso a modelos e provedores de mídia

Data da revisão: 2026-09-27

## Achado

A Comfy lançou o **Comfy Router** em 23/09/2026 e o produto continua **BETA, porém ativo** no Comfy Developer Platform. Ele oferece uma única API/SDK para chamar modelos de imagem, vídeo, áudio e 3D e permite separar a escolha do **modelo** da escolha do **provedor de inferência**.

No catálogo atual aparecem, entre outros, **Seedance 2.5, MiniMax H3, Kling 3.0/Kling 3.0 Turbo, Wan 3.0 Video e Grok Imagine Video 1.5**. Para alguns modelos, a mesma chamada pode ser executada por provedores diferentes, como fal, Runware, WaveSpeed e Higgsfield, além da própria Comfy.

A fila assíncrona aceita envio de muitos jobs e lida com limites de concorrência e erros transitórios. A Comfy declara retenção de inputs por 24 horas após upload e de outputs por 24 horas após geração.

## O que ainda não está disponível

No lançamento, o Router **não escolhe automaticamente o provedor**. O chamador informa explicitamente o provedor e, se ele estiver indisponível, a chamada falha naquele provedor. Políticas automáticas como `reliable`, `fast_start`, `fast_finish` e `lowest_cost`, fallback ordenado e roteamento por caso de uso estão no roadmap.

Também é importante distinguir o catálogo total do roteamento multi-provedor: mais de 200 modelos aparecem na plataforma, mas somente uma parcela possui alternativas de provedor no momento. Workflows ComfyUI ainda não são chamados pelo Router; essa unificação também está no roadmap.

## Preço, licenças e disponibilidade comercial

- Não há assinatura obrigatória do Router; o consumo usa créditos Comfy.
- O preço é por modelo/provedor e aparece no catálogo antes da execução.
- O Router é uma camada proprietária de serviço; as permissões comerciais dos outputs continuam dependendo do modelo, provedor e termos aplicáveis a cada chamada.
- BYOK existe apenas para modelos selecionados em Enterprise, mediante solicitação.

## Por que importa

Para um videomaker agêntico, o achado sugere separar a intenção de produção da infraestrutura de inferência. O harness pode representar uma necessidade como `image_to_video + reference_consistency + audio + 10s` e só depois selecionar `modelo + provedor`, sem espalhar SDKs e contratos diferentes pela lógica de produção.

O ganho potencial não é apenas conveniência: portabilidade entre provedores permite reagir a preço, disponibilidade, latência e rate limits sem reescrever o pipeline. Entretanto, como o roteamento automático ainda não está disponível no produto, essa política precisa continuar no próprio harness no curto prazo.

## Limitações

A camada de compatibilidade entre provedores pode não mapear todos os parâmetros do mesmo modo; produção deve falhar de forma explícita quando um parâmetro crítico não puder ser preservado. Como preços, limites e modelos mudam rapidamente, a decisão de rota deve ser observável e registrar modelo, versão, provedor, custo estimado, latência e parâmetros efetivamente aceitos.

## Fontes

- Comfy — Introducing Comfy Router: One API for Frontier Media Models: https://blog.comfy.org/p/introducing-comfy-router-one-api
- Comfy — Router / Developer Platform: https://comfy.org/platform/router
- GIGAZINE — cobertura de 27/09/2026: https://gigazine.net/gsc_news/en/20260927-comfy-router
