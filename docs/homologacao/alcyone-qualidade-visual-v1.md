# Alcyone — qualidade visual e prova funcional separadas

## Causa observada em 20/10/2026

A peça 762/1042 deriva de uma fixture geométrica de
`generate-alcyone-static-fixtures.mjs`, usada na homologação sem provedor.
Psique 763 registrou “o desenho esquemático desperta pouco encantamento”, mas aprovou
pela coerência da demonstração privada. A aceitação técnica foi usada como um padrão
visual suficiente. O usuário rejeitou a peça; a decisão foi registrada pela tela e
a execução foi pausada antes de novas inferências. Preservar os pareceres históricos.

O gerador já conserva duas imagens fotográficas sintéticas do mesmo produto, plano
e experimento (115/116). Elas são ilustrações auditadas, não fotografias de clientes,
não prova de personalização e não substituem a prova funcional aprovada.

## Alternativas e decisão

| Alternativa | Benefício | Limite/custo | Decisão |
| --- | --- | --- | --- |
| Refinar somente cores e tipografia | Pequena alteração | Mantém a fixture como centro da comunicação | Insuficiente |
| Recriar o produto e gerar todos os ativos | Pode melhorar entrega e demonstração | Muda versão e repete produção/pareceres sem necessidade comprovada | Fora deste ajuste |
| Reutilizar ilustração auditada, separada da prova funcional, e revisar seu apelo | Corrige o material apresentado e reutiliza consumo | Exige identidade, rotulagem e revisão dos novos pixels | Escolhida |

## Matriz de homologação definida antes da implementação

- Fonte: imagem concluída do mesmo produto/plano/experimento; hash e dimensões conferidos.
- Isolamento: outro produto, plano, experimento, tarefa sem reserva e hash divergente recusados.
- Composição: ilustração identificada antes do CTA; prova funcional continua vinculada e
  não é descrita como se os pixels ilustrativos fossem uma captura ou resultado pessoal.
- Qualidade: revisão separa acabamento visual, identificação/desejo e legibilidade;
  fixture técnica e critérios insuficientes impedem aprovação mesmo em preparação privada.
- Recuperação: nova composição usa bytes existentes, sem chamada de imagem; callback e
  histórico preservados. Nenhuma seleção humana ou parecer é fabricado.
- Integração local: backend simulado → fontes reais → renderer → upload → revisão,
  com duas identidades sintéticas e caminho anterior preservado.
- Desktop, iPhone 15 Pro e Pixel 7: PNG completo e reduzido, leitura e ausência de cortes.
- Economia: nenhuma mídia, vídeo, cobrança ou aumento do teto de Alcyone.

Testes comprovam comportamento e rastreabilidade; não comprovam aumento de vendas.
A candidata final depende de revisões independentes e da seleção humana de uso.

## Validação local e critério de adoção

Foram executados 95 testes focados do backend, 112 de Íris, 237 de Psique e
115 de Têmis, sem falhas/erros após correções locais. As dispensas são integrações
opcionais previamente existentes (MySQL externo, captura completa e provedor real);
a reprodução ilustrativa com a imagem auditada foi executada. O bundle dos revisores
foi reconstruído com 537 arquivos e 121 manifestos, preservando atestações históricas.
A sucessora de compatibilidade Mira v31 atualiza apenas o hash do teste de contexto
cuja construção do serviço ganhou a fonte de ilustração; não muda o produto.

A prévia local usou a imagem histórica 116, com recorte que exclui títulos e
rótulos operacionais da origem. Fonte e resultado mantêm hashes distintos. Chromium
em desktop, iPhone 15 Pro e Pixel 7 confirmou 1080×1350, ausência de overflow e
leitura da identificação ilustrativa antes do CTA. Isso valida composição técnica;
Íris e os revisores ainda precisam produzir/avaliar os pixels da entrega oficial.

Os controles novos recusam fixture aprovada, notas insuficientes, hash trocado,
recorte fora dos pixels e outra imagem não recebida. Preservam captura funcional
adequada, parecer ADJUST, replay anterior e prova vinculada sem reapresentá-la
como imagem renderizada. A regra não proíbe ilustração como estilo: proíbe tratar
placeholder técnico e baixo acabamento como qualidade suficiente.

Hipótese de melhoria: menos reprovação humana por acabamento e maior compreensão
do benefício. Adoção exige novos pareceres e seleção humana da candidata, dentro
do orçamento; métricas comerciais continuam desconhecidas até mercado voluntário.
Reversão: bloquear a candidata se fonte, limites, identidade ou qualidade divergirem,
preservando a peça rejeitada e os recibos, sem repetir inferência automaticamente.

## Contratos internos

O controller visual existente expõe `GET .../visual-inputs/illustrations` e
`GET .../visual-inputs/illustrations/{jobId}/content` para tarefa reservada. Íris
recebe no máximo duas fontes concluídas do produto, plano e experimento exatos;
Têmis recebe somente a origem selecionada pela peça atual. Nenhuma rota gera
imagem, concede aceite humano ou publica ativo. `EDITORIAL_CARD_V1` é privado;
a saída registra `LINKED_FUNCTIONAL_PROOF`, origem ilustrativa, hash e recorte.

## Correção observada na candidata 767

A candidata 767/1058 reutilizou a ilustração auditada e custou US$ 0,482244
estimados. O arquivo mostrou uma divergência determinística: o contrato previa
`O convite chegou.\nE o look?`, mas o renderizador normalizava toda separação
em espaço. A correção respeita as quebras explícitas e preserva o bloqueio de
transbordamento. O plano já pago será recomposto pelo painel, em nova tarefa,
com custo incremental de inferência zero e histórico intacto.

Matriz adicional: duas frases/identidades nos pixels; limite de altura; comando
com motivo; rejeição de origem pública, ativa e superada; repetição idempotente;
callback determinístico com custo zero; formulário desktop/iPhone/Pixel;
recuperação existente sem chamar o modelo e novos pareceres obrigatórios.
