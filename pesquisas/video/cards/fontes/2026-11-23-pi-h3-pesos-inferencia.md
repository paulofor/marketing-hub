# Fonte revisada — pi-H3 1.0: pesos e inferência audiovisual aberta

**Data de verificação:** 23/11/2026. **Data do anúncio técnico:** 10/11/2026. **Evento elegível, ainda não registrado no radar:** 21/11/2026, lançamento dos adaptadores pi-H3 1.0, código de inferência e integração ComfyUI.

**Autores / origem:** Hansheng Chen e colaboradores da Stanford University (EUA), com contribuições realizadas durante trabalhos na Luma AI. **Status:** ATIVO para download e execução própria; demonstração pública ativa. SEM API comercial oficial hospedada confirmada; código de treinamento AINDA NÃO DISPONÍVEL (prometido). Modelo de pesos derivados abertos, NÃO licença permissiva MIT ou Apache 2.0.

## O que foi publicado de fato
- Os quatro adaptadores de MiniMax H3 em 2 ou 4 etapas, em duas modalidades: DWD (Decoding Without a Decoder) e VAE convencional. DWD prediz os pixels diretamente na última etapa e evita a etapa de decodificação VAE de vídeo, reduzindo latência.
- Pesos no Hugging Face `Lakonik/pi-H3`; inferência no LakonLab v0.3; integração `Lakonik/ComfyUI-piFlow`. Requer pesos-base MiniMax H3 (o exemplo usa `multimodalart/MiniMax-H3-Pruned`), não é um modelo independente de base.
- Entrada por texto, quadro inicial e/ou quadro final; saída vídeo + áudio estéreo sincronizado. Exemplos em 768p, 24 fps, até 15 s. Quantizações INT8 ConvRot disponibilizadas para ComfyUI.

## Desempenho: separar benchmark do lançamento
O blog original divulgado em 10/11 e atualizado com a release 1.0 de 21/11 apresenta, para **pi-H3 0.9**, mediana de sete solicitações em uma NVIDIA B200, 1344×768, vídeo com áudio:
- 5 s de vídeo: 3,81 s com 2 etapas; 7,30 s com 4 etapas.
- 10 s: 9,16 s com 2 etapas.
- 15 s: 18,58 s com 2 etapas.
- No mesmo gráfico, o Sol-H3 4 etapas leva 9,48 s para o vídeo de aproximadamente 5 s, mas usa números oficiais próprios medidos em GB200, e não um experimento completamente pareado.
- Os tempos excluem carregamento do modelo, compilação, aquecimento e codificação MP4. Os números de velocidade publicados são da **0.9**, NÃO medição independente da 1.0. Os autores declaram melhora de detalhe e coerência na 1.0 sem validar de maneira independente.
- Em equipamento de consumidor, não esperar 3,8 s: teste público comunitário em RTX 5080 citou 84,11 s para 5 s a 1280×736 (configuração específica).

## Licença e restrições essenciais
Licença dos adaptadores: **MiniMax H3 Community License Agreement**, como derivados do H3; NÃO é open source permissivo. A licença original delimita o território aplicável excluindo EUA, União Europeia, Reino Unido e Coreia do Sul; o Brasil não aparece entre os territórios excluídos, mas o serviço e a distribuição precisam respeitar as demais condições. O texto exige autorização escrita prévia para determinados produtos/serviços comerciais com receita anual acima de US$ 20 milhões, regras de marca, termos a terceiros e salvaguardas de uso. Não se pressupõe autorização global por apenas ser possível baixar os arquivos. Não foi feita análise jurídica.

## Comparação e escopo
- MiniMax H3 original (China): ATIVO com pesos públicos sob licença comunitária e variantes hospedadas. pi-H3 muda **inferência e decodificação**, não cria novo modelo fundacional de geração.
- Pika H3 Flex (EUA/China): ATIVO em serviço, com componentes derivados do H3; métodos e disponibilidade distintos (seis passos anunciados); qualidade relativa não determinada.
- Apple FastFrames (EUA): ATIVO apenas para pesquisa com pesos e licença **não comercial**; foco em vídeo interativo, sem equivalência direta de teste com pi-H3.
- Kling 4.0 e Seedance 2.5 (China): ATIVOS por APIs comerciais; maior facilidade de integração hospedada, mas são sistemas diferentes e não devem ser comparados só por latência em uma GPU.
- OpenAI Sora 2 (EUA): DESCONTINUADO, API encerrada em 24/09/2026.

## Oportunidade, hipóteses e riscos
Oportunidade: prévias rápidas de publicidade e clipes curtos com áudio sincronizado, em infraestrutura própria; comparação de custo por vídeo aprovado, controle de consistência da marca, qualidade da fala em PT-BR e taxa de refações. Teste de 20 prompts iguais em 2-step e 4-step DWD versus uma API comercial, contabilizando custos de GPU B200/H100 ou equipamento local, memória, aquecimento, erros e refações. Hipótese: 4-step pode manter mais detalhe enquanto 2-step otimiza velocidade; hipótese NÃO validada. Riscos: equipamentos de datacenter, restrições territoriais, licença comercial, consistência de personagens e áudio, resultado possivelmente pior em prompt complexo, inexistência de preço oficial por vídeo e API oficial.

## Fontes primárias verificadas em 23/11/2026
- https://hanshengchen.com/blog/pi-h3/ — anúncio 10/11, release 21/11, benchmark 0.9, 1.0, método e limitações.
- https://huggingface.co/Lakonik/pi-H3 — arquivos, documentação, quatro variantes, integração, licença, status de provedores.
- https://huggingface.co/Lakonik/pi-H3/blob/main/LICENSE.md — restrições territoriais e comerciais.
- https://github.com/Lakonik/LakonLab/blob/main/docs/piH3.md — implementação.
- https://github.com/Lakonik/ComfyUI-piFlow/blob/main/docs/piH3.md — workflow ComfyUI.
- https://www.reddit.com/r/comfyui/comments/1y23y5p/2step_pih3_out_and_working_with_sulphur_3_and/ — teste comunitário isolado de desempenho em RTX 5080 (não benchmark controlado).

**Força da evidência:** ALTA para disponibilidade pública e termos da licença; MODERADA para tempos de medição do autor na 0.9; LIMITADA para qualidade 1.0 comparada a concorrentes, custo comercial e resultados PT-BR.
