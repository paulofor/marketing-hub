# Fonte revisada — FastH3-Omni: pesos abertos

Revisado em 2026-10-25. Data do lançamento comprovado: 2026-10-20.
Autores: Hao AI Lab/FastVideo e Nuva Lab. Status: ATIVO para download/autoimplantação; API hospedada é outra modalidade.

## Evidências
1. FastVideo publicou o checkpoint final BF16, variantes quantizadas (FP8/NVFP4), ComfyUI, MLX e código para inferência e treinamento. Corrige a incerteza da pesquisa de 20/10.
2. O FastH3-Omni herda referências de imagens, vídeos e áudios do MiniMax H3 e gera vídeo e áudio estéreo sincronizado. A documentação indica 5–15 segundos a 24 FPS, até nove imagens, três vídeos e três áudios, observando limite de doze referências combinadas.
3. A destilação passa de cinquenta para oito avaliações do transformador. O ganho de latência de 5,6–11,9 vezes refere-se a 27 workloads do fabricante usando oito GB200, não a CPUs nem a GPUs de consumo. Receitas de RTX 5090/4090 e DGX Spark usam quantização/offload e frequentemente 480p.
4. Pesos sob a **MiniMax H3 Community License** (não Apache/MIT), com limites comerciais/territoriais e atribuição. Licença do framework e contratos das APIs são separados. Restrições incluem EUA, UE, Reino Unido e Coreia do Sul e autorização adicional para determinadas receitas; verificar o texto contratual antes de operação ou veiculação.

## Hipótese
Testar se o renderizador de oito etapas melhora custo por tomada aprovada com personagens, produto e voz autorizados, preservando sincronismo e identidade. Medir latência, VRAM, qualidade, falhas e direitos de uso. Não assumir qualidade igual ao H3 base nem preço fixo por GPU.

## Fontes primárias
- https://haoailab.com/blogs/fastvideo-omniref-update/
- https://huggingface.co/collections/FastVideo/fasth3-omni
- https://huggingface.co/FastVideo/FastVideo-FastH3-Omni-8-Step-V1
- https://github.com/hao-ai-lab/FastVideo
- https://nuvalab.ai/fasth3/
