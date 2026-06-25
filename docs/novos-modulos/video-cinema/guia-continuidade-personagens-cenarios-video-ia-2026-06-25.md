# Guia — Continuidade de personagens e cenários em vídeos gerados por IA

- **Data de atualização:** 2026-06-25
- **Pasta:** `docs/novos-modulos/video-cinema/`
- **Objetivo:** orientar a escolha de provedor e o fluxo de produção para criar sequências em que personagens, figurinos, objetos e cenários permaneçam reconhecíveis entre cenas.
- **Escopo:** vídeos narrativos, publicidade, social video e provas de conceito do módulo **Video Cinema**.

---

## 1) Recomendação executiva

Para o requisito específico de **continuidade entre cenas**, a recomendação operacional é:

1. **Kling VIDEO 3.0 Omni** como primeira escolha quando for necessário usar um único provedor para prender personagens, objetos e elementos do cenário durante a geração.
2. **Runway** quando o projeto exigir um fluxo de direção mais controlado, com criação de imagens-chave, biblioteca de referências, storyboard, geração plano a plano e montagem.
3. **Veo 3.1** quando a prioridade for integração via API, vídeo vertical, áudio nativo, resolução elevada, extensão de vídeo e uso de até três imagens de referência.

> A escolha de Kling como primeira opção não representa uma garantia absoluta de continuidade nem um ranking universal. É uma decisão baseada na combinação atual de `Element Reference`, vinculação de sujeito, referências por imagem ou vídeo, `Start Frame`, `Start & End Frames`, multi-shot e clipes de até 15 segundos.

Nenhum provedor elimina totalmente o risco de mudanças no rosto, roupa, proporção corporal, posição de objetos ou arquitetura do cenário. A continuidade depende tanto do modelo quanto do **processo de produção**.

---

## 2) Comparação objetiva

| Necessidade principal | Recomendação | Motivo |
|---|---|---|
| Reutilizar o mesmo personagem em cenas diferentes | **Kling VIDEO 3.0 Omni** | Permite criar e vincular Elements a partir de múltiplas imagens ou vídeo de referência |
| Manter personagem, objeto e cenário dentro de uma sequência curta | **Kling VIDEO 3.0 Omni** | Oferece element binding, start frame, multi-shot e controle de storyboard |
| Criar um curta com direção plano a plano | **Runway** | Gen-4 Image References, character plates, environment plates e fluxo de edição |
| Gerar imagens-chave consistentes antes de animá-las | **Runway** | Referências nomeadas e reutilizáveis para personagem, local, objeto e estilo |
| Integração programática e vídeo vertical com áudio | **Veo 3.1** | API, 9:16, áudio nativo, referências, primeiro/último frame e extensão |
| Produção longa e previsível | **Pipeline híbrido/provider-agnostic** | Reduz dependência de um único modelo e permite escolher o melhor provedor por plano |

### Veredito

- **Assinar apenas um serviço para testar continuidade:** Kling VIDEO 3.0 Omni.
- **Produzir um projeto narrativo com maior controle editorial:** Runway.
- **Construir integração de backend/API:** avaliar Veo 3.1 e Kling, mantendo abstração de provider.

---

## 3) Por que o Kling é a primeira escolha para este caso

A documentação do Kling VIDEO 3.0 informa suporte a:

- text-to-video;
- image-to-video;
- primeiro e último frames;
- áudio nativo;
- multi-shot;
- `Start Frame + Element Reference`;
- coreferência com múltiplos personagens;
- duração flexível entre 3 e 15 segundos;
- referências de Element por múltiplas imagens ou por vídeo.

O recurso mais importante é o **Element Reference**. Um personagem pode ser criado como elemento usando:

- um vídeo curto com múltiplos ângulos; ou
- de 2 a 4 imagens de referência.

Depois, o elemento pode ser vinculado à geração para reforçar sua identidade durante movimentos de câmera e progressão da cena.

### Quando usar multi-shot

Multi-shot é útil para uma ação curta e contínua, por exemplo:

```text
Plano 1: personagem abre a porta da sala.
Plano 2: câmera acompanha o personagem entrando.
Plano 3: close no personagem olhando para um objeto sobre a mesa.
```

Para cenas críticas de publicidade ou narrativa, ainda é preferível gerar os planos separadamente. Isso permite selecionar, reprovar e regenerar apenas o plano que apresentou desvio.

---

## 4) Quando o Runway pode ser melhor

O Runway é especialmente forte como **ambiente de produção**, e não apenas como gerador final.

O Gen-4 Image References permite usar uma ou mais imagens para criar novas composições preservando características de personagens, objetos, estilos e locais. As referências podem ser nomeadas, salvas e reutilizadas. A documentação recomenda até três referências por geração.

O fluxo ideal no Runway é:

```text
referência mestre do personagem
  -> character plates
  -> referências do cenário
  -> imagem-chave de cada plano
  -> image-to-video
  -> montagem e revisão
```

A própria documentação recomenda criar:

- **character plates:** imagens neutras do personagem em diferentes ângulos, enquadramentos e roupas;
- **environment plates:** imagens do ambiente em diferentes ângulos e contextos.

Essa abordagem é mais trabalhosa, mas oferece maior previsibilidade para filmes curtos, anúncios e sequências com continuidade espacial.

---

## 5) Onde o Veo 3.1 entra

O Veo 3.1 é uma alternativa importante para integração programática. A documentação da Gemini API informa suporte a:

- vídeo em `16:9` ou `9:16`;
- image-to-video;
- até três imagens de referência para preservar a aparência de uma pessoa, personagem ou produto;
- geração com primeiro e último frames;
- extensão de vídeos gerados pelo próprio Veo;
- áudio nativo;
- 720p, 1080p e 4K, conforme modelo, duração e recurso utilizado.

Para continuidade de cenário, o Veo pode ser usado com uma imagem inicial já aprovada e com frames de conexão. Para continuidade rigorosa de personagem ao longo de muitos planos independentes, deve ser comparado em POC com Kling e Runway antes da decisão final.

---

## 6) Princípio fundamental: não gerar cada cena somente por texto

O fluxo abaixo tende a causar drift visual:

```text
prompt da cena 1 -> vídeo
prompt da cena 2 -> vídeo
prompt da cena 3 -> vídeo
```

Mesmo repetindo a descrição, o modelo pode interpretar novamente:

- rosto;
- idade aparente;
- cabelo;
- figurino;
- altura e proporções;
- acessórios;
- móveis;
- portas, janelas e paredes;
- iluminação e horário do dia.

O fluxo recomendado é:

```text
bíblia visual
  -> referências mestres
  -> imagens-chave aprovadas
  -> animação plano a plano
  -> frames de ligação
  -> montagem final
```

---

## 7) Bíblia de continuidade

Antes de gerar o primeiro vídeo, criar uma pasta de ativos fixos.

### 7.1 Personagem

Criar no mínimo:

- rosto frontal;
- rosto em três quartos;
- perfil esquerdo e direito;
- corpo inteiro frontal;
- corpo inteiro lateral;
- vista traseira quando necessária;
- figurino principal;
- acessórios e objetos pessoais;
- referência de escala ao lado de um objeto conhecido.

As imagens-base devem ter:

- expressão neutra;
- iluminação uniforme;
- fundo simples;
- pouca distorção de lente;
- cabelo, maquiagem, roupa e acessórios claramente visíveis.

### 7.2 Cenário

Criar no mínimo:

- plano geral mestre;
- ângulo oposto;
- vista lateral;
- entradas e saídas;
- detalhes relevantes;
- versão com a iluminação definitiva;
- mapa simples da posição de móveis e objetos.

### 7.3 Objetos e produto

Para itens que precisam permanecer idênticos, salvar:

- frente;
- verso;
- laterais;
- vista superior;
- detalhes de textura;
- escala;
- logotipo e texto em arquivo separado para composição na pós-produção.

Não depender do modelo de vídeo para reproduzir texto pequeno, embalagem ou logotipo com precisão em todos os frames.

### 7.4 Convenção de nomes

```text
characters/
  char_ana_v01_front.png
  char_ana_v01_3q_left.png
  char_ana_v01_profile_right.png
  char_ana_v01_fullbody.png
  char_ana_v01_costume_a.png

environments/
  env_apartment_v01_master_wide.png
  env_apartment_v01_reverse.png
  env_apartment_v01_kitchen_detail.png
  env_apartment_v01_light_night.png

props/
  prop_red_notebook_v01_front.png
  prop_red_notebook_v01_side.png

shots/
  sc01_sh01_keyframe_v03.png
  sc01_sh01_render_kling_v02.mp4
```

Nunca sobrescrever referências aprovadas. Criar novas versões.

---

## 8) Workflow recomendado para continuidade

### Etapa 1 — Roteiro e mapa de cenas

Dividir o roteiro em cenas e planos antes de gerar qualquer vídeo.

Cada plano deve registrar:

```text
scene_id
shot_id
duração
personagens presentes
cenário
figurino
objetos
posição inicial
posição final
enquadramento
movimento de câmera
ação
iluminação
áudio/diálogo
plano anterior
plano seguinte
```

### Etapa 2 — Aprovar as referências mestres

Escolher uma única versão oficial de cada personagem, cenário, figurino e objeto. Essas versões tornam-se a fonte de verdade do projeto.

### Etapa 3 — Criar character plates e environment plates

Gerar os ângulos que serão necessários no storyboard. Não esperar que uma única foto frontal explique ao modelo como o personagem deve parecer de costas ou de perfil.

### Etapa 4 — Gerar a imagem-chave de cada plano

Antes do vídeo, gerar uma imagem estática que combine:

- personagem oficial;
- cenário oficial;
- figurino correto;
- posição inicial;
- enquadramento;
- lente;
- iluminação.

A imagem só avança para vídeo depois de aprovada.

### Etapa 5 — Animar com image-to-video

No prompt de vídeo, descrever principalmente:

- ação;
- movimento de câmera;
- movimento corporal;
- ritmo;
- duração;
- transformação temporal.

Evitar redescrever o personagem com características novas. A identidade deve vir das referências.

### Etapa 6 — Criar pontes entre planos

Para dois planos diretamente conectados:

1. exportar o último frame útil do plano anterior;
2. usar esse frame como imagem inicial do plano seguinte quando o provedor permitir;
3. manter direção do olhar, posição corporal, mão que segura o objeto e eixo de câmera;
4. usar primeiro e último frames para controlar transições quando disponível.

### Etapa 7 — Gerar planos de segurança

Incluir planos que escondam pequenas inconsistências:

- detalhe de mãos ou objeto;
- plano de costas;
- plano do ambiente;
- reação curta;
- silhueta;
- corte por movimento;
- close em elemento do cenário.

### Etapa 8 — Montagem e acabamento

Na edição:

- cortar frames instáveis no início e no fim;
- usar match cuts;
- corrigir cor e exposição;
- inserir texto e logotipo em pós-produção;
- aplicar som ambiente contínuo para unir os cortes;
- ocultar pequenas mudanças com planos de cobertura.

---

## 9) Regras para reduzir drift

1. Reutilizar sempre as referências mestres, não apenas o resultado da cena anterior.
2. Evitar uma cadeia longa de “variação da variação”. Isso acumula mudanças.
3. Manter uma versão fixa do figurino por bloco narrativo.
4. Não alterar simultaneamente personagem, cenário, roupa, lente e estilo.
5. Introduzir mudanças em etapas e aprovar cada uma.
6. Gerar clipes curtos; planos de 3 a 8 segundos são mais fáceis de controlar.
7. Usar uma ação principal por plano.
8. Evitar movimentos extremos de câmera quando o rosto ou produto precisa permanecer exato.
9. Salvar prompt, referências, provider, modelo, parâmetros e tentativa usada.
10. Tratar seed como auxílio, não como garantia de repetibilidade.

---

## 10) Prompt-base por plano

Separar elementos imutáveis dos elementos variáveis.

### 10.1 Bloco fixo de continuidade

```text
CONTINUIDADE OBRIGATÓRIA

Personagem: usar exatamente o personagem da referência [CHARACTER_REF].
Manter identidade facial, formato do rosto, olhos, nariz, cabelo, proporções corporais e idade aparente.

Figurino: manter exatamente [WARDROBE_REF], incluindo cores, tecido, calçado e acessórios.

Cenário: usar exatamente [ENVIRONMENT_REF].
Preservar arquitetura, disposição dos móveis, portas, janelas, objetos e direção principal da luz.

Objeto: preservar exatamente [PROP_REF], sem alterar forma, tamanho, cor ou posição inicial.
```

### 10.2 Bloco variável do plano

```text
PLANO [SHOT_ID]

Duração: [DURATION].
Enquadramento: [FRAMING].
Lente/aparência: [LENS].
Câmera: [CAMERA_MOVEMENT].
Ação: [ACTION].
Posição inicial: [START_POSITION].
Posição final: [END_POSITION].
Expressão: [EXPRESSION].
Ritmo: [PACE].

Evitar: mudança de rosto, cabelo, roupa, acessórios, proporções, cenário, iluminação, objetos, texto inventado, membros extras, cortes inesperados e troca de lado do objeto.
```

### 10.3 Exemplo

```text
CONTINUIDADE OBRIGATÓRIA
Usar exatamente a personagem ANA_V01 e o figurino ANA_COSTUME_A.
Usar exatamente o cenário APARTMENT_V01_NIGHT.
Manter rosto, cabelo curto castanho, jaqueta azul-marinho, calça preta e relógio no pulso esquerdo.
Preservar a mesa de madeira à direita, a janela ao fundo e a luminária amarela à esquerda.

PLANO SC02_SH04
Vídeo de 6 segundos, plano médio, lente cinematográfica natural.
A câmera faz um dolly-in lento.
Ana entra pela porta, dá dois passos, para ao lado da mesa e olha para o caderno vermelho.
Ela começa com a mão direita vazia e termina tocando o caderno com a mão direita.
A luz permanece noturna e quente.
Sem cortes, sem troca de roupa, sem alteração do apartamento e sem objetos novos.
```

---

## 11) Checklist de continuidade por plano

Antes de aprovar um clipe, validar:

### Personagem

- [ ] identidade facial;
- [ ] cabelo e linha do cabelo;
- [ ] idade aparente;
- [ ] altura e proporções;
- [ ] figurino e calçado;
- [ ] acessórios;
- [ ] mão dominante e lateralidade;
- [ ] tom de voz, quando aplicável.

### Cenário

- [ ] arquitetura;
- [ ] posição de portas e janelas;
- [ ] disposição dos móveis;
- [ ] objetos de cena;
- [ ] horário e iluminação;
- [ ] clima;
- [ ] eixo de câmera e direção do movimento.

### Ligação entre planos

- [ ] posição inicial corresponde ao plano anterior;
- [ ] direção do olhar;
- [ ] posição das mãos;
- [ ] objeto permanece na mesma mão;
- [ ] roupa e cabelo não mudaram;
- [ ] movimento termina e começa de forma compatível;
- [ ] áudio ambiente permanece coerente.

### Qualidade geral

- [ ] rosto estável durante movimento;
- [ ] mãos aceitáveis;
- [ ] sem membros ou objetos extras;
- [ ] sem texto deformado;
- [ ] sem alteração involuntária de produto ou logotipo;
- [ ] sem salto brusco de exposição ou cor.

---

## 12) POC recomendada antes de escolher o provider

Executar a mesma sequência nos três candidatos.

### Sequência de teste

```text
SC01_SH01 — plano geral externo do prédio
SC01_SH02 — personagem abre a porta e entra
SC01_SH03 — plano médio dentro da sala
SC01_SH04 — close no rosto olhando para um objeto
SC01_SH05 — contraplano mostrando o objeto e o cenário
```

Usar exatamente as mesmas referências e intenção cinematográfica.

### Critérios de pontuação

Atribuir nota de 0 a 5 para:

| Critério | Peso sugerido |
|---|---:|
| Identidade facial entre planos | 25% |
| Figurino e acessórios | 10% |
| Continuidade do cenário | 20% |
| Continuidade entre último e primeiro frame | 15% |
| Movimento e física | 10% |
| Fidelidade ao prompt | 10% |
| Custo e número de tentativas | 5% |
| Tempo de geração e operação | 5% |

Registrar também:

- taxa de aprovação na primeira geração;
- quantidade média de regenerações por plano;
- custo por segundo aprovado, não apenas por segundo gerado;
- tempo humano de correção;
- disponibilidade de API;
- política de uso comercial;
- privacidade e retenção dos arquivos.

---

## 13) Implicações para o módulo Video Cinema

O módulo deve ser **provider-agnostic**. A consistência deve existir como uma camada de produto, e não como uma opção escondida dentro de um provedor.

### Entidades sugeridas

```text
VideoCinemaProject
ContinuityBible
Character
CharacterReferenceSet
WardrobeReferenceSet
Environment
EnvironmentReferenceSet
PropReferenceSet
Scene
Shot
ShotKeyframe
GenerationAttempt
ContinuityReview
ApprovedAsset
```

### Dados mínimos por tentativa

```text
provider
model
model_version
prompt
negative_constraints
reference_asset_ids
start_frame_asset_id
end_frame_asset_id
seed_if_available
aspect_ratio
duration
resolution
cost_estimate
requested_at
completed_at
status
output_asset_id
continuity_score
review_notes
```

### Regras de produto

- referências aprovadas devem ser imutáveis e versionadas;
- cada plano deve apontar para as versões exatas das referências usadas;
- toda regeneração deve criar uma nova tentativa;
- a aprovação deve ocorrer por plano antes da montagem final;
- o sistema deve permitir trocar de provider sem reconstruir o projeto;
- o custo deve ser medido por clipe aprovado;
- a revisão humana deve ser obrigatória antes da exportação comercial.

---

## 14) Proposta de implementação em fases

### P0 — Documentação e modelo de dados

- criar `ContinuityBible`;
- cadastrar personagens, cenários, figurinos e objetos;
- versionar referências;
- definir cenas e planos;
- armazenar prompts e imagens-chave.

### P1 — Produção assistida

- upload e seleção de referências;
- geração manual ou semiautomática de keyframes;
- handoff para Kling, Runway ou Veo;
- upload do resultado;
- checklist de continuidade por plano.

### P2 — Primeira integração

- integrar um provider de image-to-video;
- enviar referências e primeiro frame;
- acompanhar job de render;
- registrar custo e tentativas;
- permitir retry com o mesmo pacote de continuidade.

### P3 — Multi-provider

- adapters para Kling, Runway e Veo;
- escolha automática por tipo de plano;
- fallback quando um provider falhar;
- comparação lado a lado;
- score automático mais revisão humana.

### P4 — Montagem

- sequência automática dos planos aprovados;
- transições;
- áudio ambiente;
- narração;
- legendas;
- exportação em 9:16 e 16:9.

---

## 15) Sora

Sora não deve ser escolhido como base para um novo fluxo. A OpenAI informa que:

- as experiências web e app foram descontinuadas em **26 de abril de 2026**;
- a API será descontinuada em **24 de setembro de 2026**.

O módulo Video Cinema deve tratar qualquer integração ou ativo do Sora apenas como legado/migração.

---

## 16) Conclusão

Para continuidade visual, o melhor resultado não vem de pedir “o mesmo personagem” em cada prompt. Ele vem de uma cadeia controlada:

```text
bíblia visual
  -> character/environment plates
  -> referências versionadas
  -> imagem-chave aprovada
  -> image-to-video
  -> frame de ligação
  -> revisão de continuidade
  -> montagem
```

A decisão inicial recomendada é testar **Kling VIDEO 3.0 Omni** como provedor principal, manter **Runway** como referência de workflow profissional e avaliar **Veo 3.1** para API e geração de alta fidelidade.

Para o Marketing Hub, a decisão arquitetural mais importante é não acoplar a continuidade a um único modelo. Personagens, cenários, referências, keyframes, prompts e avaliações devem permanecer no domínio do produto, permitindo substituir o provider quando a qualidade, o preço ou a disponibilidade mudarem.

---

## 17) Fontes oficiais

- [Kling AI — Kling VIDEO 3.0 Model User Guide](https://kling.ai/quickstart/klingai-video-3-model-user-guide)
- [Runway — Creating with Gen-4 Image References](https://help.runwayml.com/hc/en-us/articles/40042718905875-Creating-with-Gen-4-Image-References)
- [Runway — How to create longer videos and films](https://help.runwayml.com/hc/en-us/articles/26871350018835-How-to-create-longer-videos-and-films)
- [Google AI for Developers — Generate videos with Veo 3.1 in Gemini API](https://ai.google.dev/gemini-api/docs/video)
- [OpenAI Help — What to know about the Sora discontinuation](https://help.openai.com/en/articles/20001152-what-to-know-about-the-sora-discontinuation)
