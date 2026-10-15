# Roteiro persuasivo audiovisual — v1

## Evidência e causa confirmada

Em 15/10/2026, MCP e telas confirmaram Mira #10/experimento #102 e Capella #7/experimento #105.
Projetos #10–13 preservam falas como “Mira organiza suas informações, com fontes e limites” e
“Arte e texto combinam. Copie a legenda. Baixe seus arquivos”. Os vídeos de Capella estão rejeitados
pelo operador. O parecer #736 aprovou #69/#71 explicitamente nos critérios visuais; #731 havia
rejeitado a demonstração sem ações correspondentes. O feedback atual identifica fraqueza da copy.
Não se trata de falta de instrução genérica: os prompts já pediam desejo e clareza. O briefing
aceitava descrição sem fala pronta e o schema/validador de Psique não exigia avaliação textual
com evidência. O planejador de Apolo preserva copy recebida e a pós-produção pode reutilizar voz;
correção visual sozinha conserva o problema. Logs atuais de Apolo estão acessíveis, mas não são
fonte das falas históricas; os projetos e pareceres persistidos são.

## Alternativas consideradas

| Caminho | Benefício | Risco/custo | Escolha |
| --- | --- | --- | --- |
| Acrescentar conselho genérico ao prompt | Pouca mudança | Já existia e não impediu o problema | Insuficiente |
| Criar agente roteirista separado | Especialização explícita | Novo repasse, integração e custo; duplica Íris | Não necessário agora |
| Especializar Íris/Apolo e tornar a revisão textual verificável em Psique | Corrige escrita e aceite no fluxo existente | Mudança pequena de contrato e testes | Adotado |

## Matriz de aceite definida antes dos testes

| Dimensão | Verificação local necessária |
| --- | --- |
| Escrita | Briefing audiovisual sem fala pronta é recusado; ausência de vídeo mantém fluxo estático |
| Prompt real | Executor entrega instruções novas e contexto do produto; não apenas arquivo solto |
| Persuasão | Parecer simulado DESCRIPTIVE/critério baixo nunca aprova, mesmo com visual alto |
| Prova textual | Quotes devem existir na narração exata; texto ausente vira BLOCKED/ADJUST, nunca aprovação |
| Papéis | AD não precisa recitar oferta completa; continuidade paga só quando aplicável, explicando a exceção |
| Identidade | Mesma mídia/hash; outro produto/IDs e caso antes válido preservados |
| Recuperação | Saída persistível no callback; não reexecutar inferência nem sintetizar voz em teste |
| Integração | Schema, prompt, consumidor e validação reais; modelos externos substituídos localmente |
| Observabilidade | Motivos de recusa localizáveis e avaliação por critério preservada no resultado |
| Métricas | Fixtures não são clientes nem vendas; efeito comercial depende de experimento autorizado |
| Interface | Sem alteração de UI/player: navegação publicada desktop e leitura dos contratos; testes de vídeo/mobile anteriores não substituem avaliação textual |

## Adoção e limites

Adotar quando suítes relevantes passarem e contratos preservarem imagem estática, decisões de
ajuste e vínculo do vídeo. Reverter a mudança se impedir pareceres válidos ou perder recuperação;
reavaliar critérios usando rejeições posteriores. Não afirmar aumento de vendas, naturalidade de
voz ou qualidade de saída de um modelo real a partir de test doubles. Nenhuma nova produção paga
é parte desta validação. Resultados e SHA serão registrados após a execução dos testes e entrega.

## Resultados locais — 15/10/2026

- Íris: 90 testes declarados, 87 executados com sucesso; três integrações opcionais que exigem
  fixtures externas permanecem desabilitadas. O teste novo percorre a validação real da atividade:
  briefing descritivo falha, fala separada passa e pacote sem vídeo continua válido.
- Psique: 200 declarados, 189 executados com sucesso; 11 integrações opcionais de outros harnesses
  não ativadas. Após revisão de tipos nulos, repetidos somente os testes afetados. Casos Mira e
  Capella, outra identidade, narrativa ausente, citação inventada, nota baixa, avaliação descritiva,
  continuidade não aplicável, schema estrito e prompt efetivamente composto estão cobertos.
- Apolo: suíte completa, 288 declarados, 279 executados com sucesso e nove cenários opcionais
  desabilitados. Não houve mudança de render, áudio ou provider. Os contratos de recuperação
  existentes continuam testados; não houve consumo de IA nem geração de vídeo.
- Backend: 21 testes de catálogo/empacotamento dos prompts e projeção do vídeo final passaram.
  A narração real já chega no contrato usado pela nova avaliação; nenhuma API/coluna nova.
- Formatação dos arquivos Java alterados e `git diff --check` passaram. A navegação real confirmou
  a cadeia #26, os agentes e as revisões dos dois produtos; nenhuma ação paga foi disparada.

São 576 testes executados com sucesso, além de 23 opcionais não executados. As avaliações
persuasivas dos testes são simuladas e claramente identificadas. A publicação ainda deve
comprovar os mesmos recursos nos executores; o desempenho de redação do modelo e o impacto em
vendas dependerão das próximas produções e dos experimentos autorizados, não destes números.
