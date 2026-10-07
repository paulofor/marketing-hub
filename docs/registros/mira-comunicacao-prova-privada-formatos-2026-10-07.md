# Mira — prova privada e formatos — 07/10/2026

## Causa e histórico

No ciclo 9/experimento 102, Íris 634 concluiu a mensagem escolhendo demonstração em vídeo e
deixando a peça estática como alternativa. A 635 bloqueou a produção: o backend sempre exigia
imagem final, enquanto os prompts exigiam preservar os formatos escolhidos. Não há falha na
experiência V3 ou nos pixels aprovados. A hipótese de que escolher vídeo sempre quebra a rota
foi descartada pelo histórico: 500/502/530/535/545 escolheram vídeo, e 503 produziu imagem válida.
Esses contratos incluíam controles estáticos; 634 não os selecionou. Faltava distinguir prova
interna obrigatória de formato comercial e declarar esse requisito no contrato inicial.

Por que esse erro aconteceu? A exigência executável de imagem existia somente na etapa seguinte;
o contrato anterior podia excluir um formato sem conhecer a finalidade técnica dessa imagem.
O histórico LOOP-APOLO-BPM-AUDIOVISUAL-SEM-CONSUMIDOR também confirma que a presença de briefing
não substitui pedido governado de produção: a 504 bloqueou sem provider por falta desse contrato.

## Alternativas

| Alternativa | Benefício | Risco/esforço | Decisão |
|---|---|---|---|
| Refazer a mensagem escolhendo anúncio estático adicional | Alinha os formatos textuais | Nova inferência e possível mudança comercial desnecessária | Descartada |
| Suprimir a imagem quando houver vídeo | Preserva a escolha exclusiva | Remove a prova necessária aos revisores e quebra caminhos antes válidos | Descartada |
| Declarar prova interna e fronteira de produção | Preserva mensagem, provas e formato comercial | Exige contratos e regressões entre backend e executor | Adotada |

`PDE_PRIVATE_CREATIVE_PREPARATION_V1` declara origem, versão, prova interna, propósito de revisão
e ausência de autorização de publicação/gasto. Sem pedido governado vigente, o vídeo permanece
em briefing e não é produzido nem aprovado. Pedido próprio da mesma versão conserva a produção
e seus gates. A prova privada não cria variante comercial nem muda as condições da comparação.

## Matriz local de aceite, definida antes dos testes

- Caminho feliz: mensagem real 634 → declaração backend → preflight de Íris → PNG real
  renderizado/persistido → callbacks e revisões independentes → retorno ao pai e integração privada.
- Regressão: produto 10 e outro produto/experimento; IDs, origem, versão e hashes isolados.
- Caso original: mensagem escolhendo apenas vídeo, sem ativos estáticos; a prova interna continua
  obrigatória e não vira anúncio estático selecionado. A tentativa 635 permanece no histórico.
- Vídeo: briefing sem pedido não produz tarefa/provider/mídia; pedido governado da mesma versão
  conserva a exigência. Pedido de outra versão/identidade não autoriza produção.
- Legado/comercial: caminhos antes válidos com e sem vídeo conservam suas exigências.
- Falhas: declaração divergente, gasto/publicação implícitos, PNG ausente, hash/origem trocados,
  atividade em andamento e retorno sem parecer independente permanecem bloqueados.
- Retomada: nova declaração reabre somente a rota técnica; a comparação é estável entre números
  e mapas transportados. A mensagem compatível e sua auditoria não exigem nova inferência.
- Observabilidade: formato planejado, produção adiada, motivo, origem e versão persistidos;
  `NOT_APPLICABLE` não representa vídeo produzido/aprovado. Provas e custos não viram métricas comerciais.
- Visual: prova renderizada em desktop e emulação mobile Chromium a 393 pixels; Safari nativo
  não é declarado validado. Nenhum teste usa gasto externo, consumidor ou opinião solicitada.

Mira foi pausada pelo frontend antes desta correção. Custo estimado conhecido: US$ 8,5320024,
sob o teto total original de US$ 10. A imagem não existe na saída bloqueada 635: replay dessa
resposta não produz pixels. Uma nova tentativa de materialização poderá ocorrer somente após
validação e publicação da correção; preservará a mensagem 634 e todos os custos anteriores.

## Resultados locais

- Backend: 520 testes relacionados, zero falhas/erros; um replay histórico opcional não solicitado.
  Inclui a mensagem real, outra identidade, consulta indisponível, pedido anterior ao experimento,
  pedido da versão atual, dispensa reaberta, preservação de tarefas em curso e retomada única.
- Íris: 61 testes executados, zero falhas/erros. A declaração exportada pelo backend passou pelo
  preflight real do executor; callbacks HTTP locais preservaram request, resposta bruta e hashes.
  Uma resposta sem pixels não foi declarada materializável por replay.
- Persistência: MySQL 5.7 descartável, leitura privada sem escrita e conclusão transacional pelos
  contratos existentes. Os avisos de remoção de tabelas inexistentes pertencem ao `create-drop`
  inicial do teste; os quatro testes concluíram com sucesso.
- PNG real produzido pelo renderer versionado a partir de captura local da experiência V3,
  somente para QA: `aefde1e804bc617ecb99632be509c676916e7251c85c35d95fefe41a7c0c6e44`.
  O primeiro recorte foi recusado pelo limite de escala; o recorte menor passou. Não houve
  relaxamento de legibilidade nem upload produtivo de imagem sintética.
- Build administrativo real em desktop/iPhone/Pixel: pixels 1080×1350 decodificados, auditoria
  carregada pelo endpoint canônico, sem overflow, erro de página ou conexão/escrita externa.
  O harness visual foi atualizado porque simulava somente a lista resumida e omitia a consulta
  detalhada de auditoria e o resumo de vídeos agora consumidos pela interface.
- Builder de evidências: 16 testes e 408 arquivos/76 manifestos íntegros. Nenhuma atestação PDE
  atual referia bytes alterados; não houve nova imagem, versão de experiência ou manifestos extras.
- JAR executável: 4257 classes idênticas à compilação testada, 761 recursos externos íntegros
  e catálogo inicializado com 523 cartões. Scripts investigados passaram `bash -n` e ShellCheck.

Os testes usam modelos, revisores, callbacks e armazenamento locais/simulados quando necessário.
Não substituem os próximos pareceres reais de Psique/Têmis nem a decisão humana de uso.
Preparação/revisão pode terminar antes da autorização de uso ou da publicação comercial;
essa autorização não é inferida do teto de IA.
