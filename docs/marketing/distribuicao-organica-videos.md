# Distribuição orgânica de vídeos

## Objetivo comercial

Criar uma fila central de distribuição orgânica para reaproveitar vídeos de produto em YouTube Shorts, Instagram Reels e TikTok, preservando vínculo com produto, criativo, status de publicação, link publicado e métricas posteriores.

## Estratégia escolhida

Foram avaliadas três alternativas:

- Publicar direto em todas as redes de uma vez: maior impacto, mas alto risco por OAuth, auditoria e diferenças de API.
- Documentar o processo sem sistema: baixo esforço, mas não cria operação escalável.
- Criar primeiro o núcleo operacional com fila, status e métricas: melhor equilíbrio para começar pelo YouTube sem fingir que Instagram/TikTok já estão liberados.

A opção implementada é o núcleo operacional.

## Primeira fase

- YouTube como canal inicial.
- Instagram Reels modelado para entrar após conexão Meta/Instagram profissional.
- TikTok modelado como rascunho enquanto o Direct Post não estiver aprovado.
- Publicação externa real deve ser feita por conector/worker oficial consumindo a fila, nunca por automação manual fora do repositório.

## O que precisa para publicar no YouTube

- Criar projeto no Google Cloud.
- Habilitar YouTube Data API v3.
- Criar credencial OAuth.
- Configurar consent screen e escopo `https://www.googleapis.com/auth/youtube.upload`.
- Conectar o canal do produto no Marketing Hub.
- Depois disso, implementar o executor oficial de upload para consumir `/api/social-distribution/publications/pending`.

## Evidências de documentação oficial

- YouTube Data API permite upload de vídeos com `videos.insert`.
- Instagram Content Publishing API permite publicação de vídeos/Reels para contas profissionais elegíveis.
- TikTok Content Posting API permite upload/rascunho e Direct Post, mas Direct Post público depende de aprovação/auditoria.

## Evolução: plano de crescimento mensurável

A tela de Distribuição Orgânica passa a organizar ciclos por produto com
público, hipótese comercial, objetivo, CTA e campanha UTM. As pautas distinguem
Shorts de vídeos longos, registram pilar e etapa do funil e recebem uma URL
rastreável gerada pelo backend.

O gate humano permanece obrigatório: aprovar uma pauta libera seu vínculo com
uma publicação, mas não a coloca na fila. O desempenho do ciclo separa alcance,
aquecimento e conversão por meio de visualizações engajadas, recorrência,
sessões, leads, checkouts, vendas e receita atribuída.
