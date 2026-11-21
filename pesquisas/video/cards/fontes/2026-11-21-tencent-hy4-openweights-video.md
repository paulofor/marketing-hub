# Fonte revisada — Tencent Hy4: pesos abertos para compreensão visual de vídeo

**Revisão:** 21/11/2026. **Evento:** pesos Hy4 publicados pela Tencent em 20/11/2026 (a família tinha acesso por API desde 04/11). **Origem:** Tencent Hy, China.

**Status:** ATIVO — pesos disponíveis em Hugging Face e código em GitHub para auto-hospedagem. É um modelo multimodal de compreensão (texto, imagens e vídeo), NÃO gera cenas de vídeo ou faixa de áudio.

## O que abriu
Hy4 inclui variante Instruct, FP8 e modelo base pré-treinado. Backbone mixture-of-experts com 770 bilhões de parâmetros totais e cerca de 49 bilhões ativos por token; encoder visual separado de ~1,23B e decodificador especulativo DFly2 de ~3,3B. Janela declarada de 1.048.576 tokens e ingestão de quadros de vídeo. A edição FP8 pesa perto de 0,79 TB, e BF16 perto de 1,55 TB apenas de arquivos; implantação prática requer servidor especializado com várias GPUs, não uma VPS modesta.

## Licenciamento e disponibilidade
A licença oficial **Tencent Hy License 1.0** não é Apache 2.0. Exige autorização por escrito para determinados negócios de model-as-a-service com receita bruta global acima de US$ 50 milhões por 12 meses OU produtos/serviços que usam o modelo com mais de 100 milhões de usuários ativos mensais; há exceções, inclusive para uso interno, conforme texto integral. Deve haver análise jurídica antes de comercializar. Não foi confirmado preço de hospedagem próprio nem foi executada a inferência.

## Comparação prática
WeMM-Video (Tencent WeChat Vision, China) já registrado em 20/11: ATIVO, pesos Apache 2.0 de 2B a 35B-A3B, compreensão conjunta de vídeo e áudio; mais viável para ensaios locais. Qwen4-Max (Alibaba, China): ATIVO em API proprietária, sem pesos abertos. Gemini Flash (Google, EUA): serviço de API ATIVO, pesos proprietários. Nenhuma comparação de qualidade foi reproduzida; Hy4 tem foco multimodal geral e não substitui WeMM na avaliação sincronizada da trilha.

## Aplicação ao Marketing Hub
Importante como sinal de abertura de pesos e mudança de licença em um modelo de fronteira, mas NÃO recomendado como primeiro piloto local: custo de infraestrutura desproporcional ao objetivo de triagem de anúncios. Verificar restrições de licenciamento e viabilidade antes de avaliar contratação de GPUs.

**Fontes primárias (consultadas em 21/11/2026):**
- https://huggingface.co/tencent/Hy4
- https://github.com/Tencent-Hunyuan/Hy4
- https://github.com/Tencent-Hunyuan/Hy4/blob/main/LICENSE

**Limitação:** datas e artefatos confirmados publicamente; sem testes de inferência, benchmark independente ou proposta comercial.
