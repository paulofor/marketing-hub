# Fonte revisada — PixVerse R2 e estado persistente em mundo audiovisual

Data da revisão: 2026-09-23
Coleção: video
CardKey: video-estado-persistente-eventos-temporais

## Achado

A PixVerse lançou o R2 em 22/09/2026 como segunda geração de seu real-time world model. Diferentemente de um gerador de clipes independentes, R2 mantém uma sessão contínua: texto, referências, áudio e ações alteram o estado do mundo e continuam influenciando o que acontece depois. A empresa afirma maior coerência em sessões longas, memória dentro da sessão e capacidade de aceitar novos inputs enquanto a geração continua em tempo real.

O PixVerse Game Engine já roda sobre R2, e uma coleção de mundos R2 está aberta para exploração. Isso coloca o modelo além de uma demonstração fechada, mas ainda não foi localizada documentação pública de API específica, preço específico ou pesos abertos para R2.

## Status e disponibilidade

- PixVerse R2: ATIVO/LIMITADO.
- Lançamento público: 22/09/2026.
- Acesso: mundos públicos podem ser explorados; o Game Engine da PixVerse usa R2.
- API específica do R2: não localizada na documentação pública consultada.
- Preço específico: não localizado.
- Pesos/licença open-weight: não localizados.
- O ecossistema geral PixVerse possui API, CLI, Agent e Canvas, mas isso não deve ser confundido com disponibilidade pública de uma API R2 dedicada.

## Comparação

Runway GWM Worlds 2 continua em Research Preview. Ele gera vídeo contínuo em 720p/24 fps com áudio 48 kHz, usa contexto persistente + eventos temporais e permite controle contínuo de câmera, mas a própria Runway reconhece memória de longo prazo ainda imperfeita.

R2 parece mais produtizado na camada de experiência, porque já alimenta o PixVerse Game Engine e tem mundos acessíveis ao público. Ainda assim, não deve ser tratado como backend de produção convencional enquanto API, preço e garantias operacionais não forem documentados.

## Implicação para o Marketing Hub

R2 reforça que o estado audiovisual pode deixar de ser reexplicado a cada tomada. Porém, para publicidade, o estado comercial crítico deve continuar fora do modelo como fonte canônica.

Fluxo candidato:

`scene_state canônico -> ação/evento -> geração contínua -> revisor audiovisual -> atualização apenas do estado confirmado`

A memória interna do world model pode melhorar continuidade, mas não deve ser a única fonte de verdade para produto, preço, figurino, cenário, claims, CTA ou identidade visual.

## Riscos e limites

- Persistência declarada é principalmente evidência do fornecedor; ainda falta benchmark independente de memória longa.
- Não há API pública específica do R2 verificada nesta rodada.
- Um mundo que “lembra” também pode persistir um erro; revisão externa continua necessária.
- World models em tempo real ainda trocam fidelidade por latência e não substituem renderers offline de alta qualidade em todos os casos.
- Para anúncios convencionais, a complexidade de um world model pode não compensar frente a um pipeline de cenas discretas.

## Fontes revisadas

- PixVerse — R2, 22/09/2026: https://pixverse.ai/en/blog/pixverse-introduces-r2-real-time-world-model
- PixVerse — página principal/modelos: https://pixverse.ai/
- Runway — GWM Worlds 2, 03/09/2026: https://runway.com/research/introducing-gwm-worlds-2
