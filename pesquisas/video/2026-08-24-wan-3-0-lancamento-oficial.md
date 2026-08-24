# Monitoramento de IA para vídeo — 24/08/2026

## Wan 3.0: lançamento oficial da Alibaba

O **Wan 3.0**, da Alibaba, saiu do estágio de beta/preview e foi oficialmente lançado em **24 de agosto de 2026**. O modelo passa a ser tratado como **ATIVO / lançamento oficial**, com disponibilidade direta pelo Alibaba Cloud Model Studio e uso por API.

### O que mudou

- Status anterior: limitado / preview.
- Status atual: **ativo / lançamento oficial**.
- Geração de vídeo de até **30 segundos**.
- Entrada multimodal por **texto, imagem, vídeo, áudio e documentos**.
- Aceita materiais como **PPT, PDF, DOC, XLS e páginas web** como referência.
- Suporta **edição e extensão de vídeo**.
- Áudio nativo sincronizado.
- Pesos: **fechados / não disponíveis para self-hosting**.

### Preços oficiais

| Resolução | Preço aproximado |
|---|---:|
| 480p | US$ 0,05/s |
| 720p | US$ 0,10/s |
| 1080p | US$ 0,20/s |

Um vídeo de 30 segundos custa aproximadamente **US$ 3 em 720p** ou **US$ 6 em 1080p**.

### Comparação prática

O **Veo 3.1 Fast**, do Google, continua mais barato em 1080p, por aproximadamente **US$ 0,12/s**, e mantém excelente relação custo/qualidade, mas trabalha com clipes curtos de 4, 6 ou 8 segundos.

O **Seedance 2.5** continua mais forte quando a prioridade é controlabilidade, grande quantidade de referências multimodais e edição orientada por referências. A documentação de 1080p do Seedance ainda varia conforme o provedor/endpoint e deve ser verificada antes de cada recomendação.

### Por que importa

O Wan 3.0 entra entre as opções de melhor custo-benefício para **peças de 15–30 segundos**, especialmente publicidade, vídeos explicativos e conteúdos derivados de briefing, apresentação ou documento. A possibilidade de alimentar diretamente o modelo com documentos aproxima o sistema de um componente de **produção audiovisual agêntica**, e não apenas de um gerador texto→vídeo.

### Status resumido

| Sistema | Status | Melhor uso atual |
|---|---|---|
| Wan 3.0 / Alibaba | 🟢 Ativo / oficial | vídeos de 15–30 s, multimodal, documentos |
| Veo 3.1 Fast / Google | 🟢 Ativo / GA | tomadas curtas, 1080p, custo-benefício |
| Seedance 2.5 | 🟢 Ativo, dependente do provedor | controle, referências e edição |

## Fontes

- Alibaba Cloud: https://www.alibabacloud.com/blog/wan3-0-30-second-ai-video-generation-from-any-input_603452
- Reuters: https://www.reuters.com/business/retail-consumer/alibaba-launches-wan30-ai-video-model-after-10-billion-share-sale-2026-08-24/

---

Este arquivo faz parte do monitoramento recorrente de sistemas de IA voltados à criação, edição, compreensão e automação de vídeo.
