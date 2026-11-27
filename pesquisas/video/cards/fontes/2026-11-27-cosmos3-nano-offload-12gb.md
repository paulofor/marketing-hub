# Cosmos 3 Nano — inferência de vídeo em GPU de 12 GB

Revisão: 2026-11-27.

## Achado
Tutorial NVIDIA de 24/11, repercutido em 27/11, documenta pacote de streaming de pesos via RAM. RTX 5070, FP8: 720p sem áudio em 2.235,7 s e 9,16 GiB de VRAM; com áudio em 2.256 s e 10,92 GiB.

## Mecanismo
Pesos em RAM, transferência de blocos do transformer via PCIe durante inferência; Diffusers e quantização FP8.

## Aplicação
Prototipagem audiovisual local, simulação de objetos, avaliação de independência de APIs, especialmente com dados não sensíveis ou controlados.

## Evidência
Tutorial e benchmarks do fabricante, documentação oficial de pesos; não reproduzidos independentemente.

## Hipótese
Medir custo e tempo de geração local de 240p/480p para prévias, comparar qualidade e custo de cenas aprovadas com serviço hospedado.

## Riscos e limites
Latência alta, RAM e PCIe, custo de hardware, qualidade publicitária não avaliada; não é tempo real.

Pesos ativos desde 31/05, licença OpenMDW 1.1 com uso comercial declarado; hosts testados com ~128 GB de RAM (não mínimo comprovado); 189 quadros a 24 fps; benchmark exclui carga e MP4. Sem preço de API verificado.

## Fontes
- https://developer.nvidia.com/blog/how-to-run-nvidia-cosmos-3-on-local-ai-devices-using-hugging-face-diffusers/
- https://huggingface.co/nvidia/Cosmos3-Nano
- https://huggingface.co/docs/diffusers/main/api/pipelines/cosmos3
