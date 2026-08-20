# Biblioteca de Personas e Agente Cliente v1

## Objetivo

A Biblioteca de Personas mantém hipóteses versionadas e rastreáveis sobre clientes. O Agente
Cliente usa essas hipóteses para revisar ofertas, vídeos e páginas antes de testes humanos.

## Fonte e confiança

Toda persona exige evidências explícitas. Novas personas começam como `HIPOTESE`; somente dados
humanos oficiais podem elevar sua confiança. Texto persuasivo produzido por IA, avaliações
simuladas e estimativas nunca constituem validação.

## Separação obrigatória

`simulated_assessment` registra a previsão do agente, `hypothesis_json` registra o que deve ser
testado e `human_result_json` recebe somente resultados posteriores de sessões, vendas, feedbacks
ou outras fontes humanas oficiais. Esses campos nunca devem ser fundidos ou preenchidos um pelo
outro.

## Autoridade

O Agente Cliente opera em sandbox somente leitura. Ele pode retornar `APROVAR_TESTE`, `AJUSTAR` ou
`REPROVAR`, mas não altera ativos, preços, campanhas, publicações, personas ou resultados humanos.

Na avaliação comercial, o worker deve disponibilizar busca pública e Chromium/Playwright versionados na imagem. A pesquisa verifica linguagem, dores, desejos, objeções, alternativas concorrentes e sinais sociais/econômicos recentes, sempre sem login ou interação que altere estado. Fontes consultadas ficam na resposta bruta auditável com URL, título, data, método e aprendizado. Sinais públicos de redes sociais são hipóteses exploratórias, não validação humana, demanda confirmada ou venda.

O worker possui CI/CD dedicado, identidade Codex persistente e validação de autenticação após cada deploy. Evidências pesadas usam bucket S3 privado, criptografado, versionado e com retenção definida; MySQL continua sendo a fonte de verdade.

Avaliações que terminam em `FAILED` devem preservar a causa técnica em `last_error` e podem ser
reabertas pelo comando explícito de retry, mantendo o mesmo identificador e incrementando
`retry_count`. O retry nunca é permitido para avaliações pendentes, em execução ou concluídas. O
worker deve persistir mensagem, cadeia de causas e stack trace limitado, e seu limite padrão de
execução é quarenta minutos para evitar o encerramento prematuro observado em avaliações válidas.
O frontend deve apresentar a mensagem principal diretamente e disponibilizar, sob expansão
explícita, todo o conteúdo persistido em `last_error`; truncar a visualização à primeira linha é
proibido porque volta a tornar o diagnóstico dependente dos logs do worker.

## Métrica de qualidade

A maturidade é medida pela correspondência posterior entre objeções previstas e comportamento
humano observado, nunca pela quantidade ou eloquência dos relatórios.

## Simulador comportamental v1

Cada avaliação declara `BASELINE_V1`, `BEHAVIORAL_V1` ou `BEHAVIORAL_V2`. Registros legados mantêm
sua versão original, mas toda nova avaliação sem versão explícita usa `BEHAVIORAL_V2` para impedir
o retorno silencioso ao comportamento plenamente racional. O modo comportamental sempre executa
primeiro o baseline com a mesma persona e ativo; em seguida produz uma segunda avaliação separada,
sem sobrescrever ou reinterpretar o resultado original.

O `BEHAVIORAL_V1` deve modelar estado anterior à exposição, memória contextual limitada às
evidências fornecidas, objetivos concorrentes, atenção limitada, transições mentais progressivas,
memória seletiva e uma distribuição de ações cuja soma seja exatamente 100. Ele deve permitir
abandono antes da oferta e nunca converter probabilidade simulada em taxa real.

Baseline e resultado comportamental ficam persistidos separadamente e vinculados à mesma
avaliação. A comparação registra acordos, divergências e hipótese de ganho preditivo. Nenhuma versão
pode declarar superioridade sem comparação posterior contra eventos humanos oficiais do mesmo tipo
de ativo, persona e etapa da jornada.

A validação comparativa usa três decisões: continuar quando o modo comportamental ordenar variantes
ou localizar abandono melhor que o baseline; ajustar quando o ganho for parcial; interromper quando
apenas aumentar a elaboração narrativa sem reduzir erro preditivo. Campanhas, preço, publicação e
resultados humanos permanecem fora da autoridade do simulador.

## Simulador humano afetivo e social v2

`BEHAVIORAL_V2` é o modo recomendado na interface e preserva `BASELINE_V1` e `BEHAVIORAL_V1`
somente para compatibilidade e comparação. O v2 nunca descreve a persona como agente perfeitamente
racional. Toda avaliação deve separar a reação afetiva inicial, a atenção seletiva, a decisão
heurística e uma possível justificativa racional posterior.

Psique deve modelar simultaneamente:

- afastamento de dor, perda, arrependimento e rejeição;
- busca de prazer, alívio, recompensa e autonomia;
- desconto subjetivo do valor por esforço mental, financeiro ou operacional;
- novidade e surpresa apoiadas em familiaridade suficiente para preservar confiança e controle;
- risco sentido, que pode divergir do risco calculado;
- ambivalência, incoerência e dependência do enquadramento;
- valor relacional: o desejo fundamental de ser aceita, admirada, lembrada, pertencente e amada e
  de evitar rejeição ou invisibilidade.

A necessidade de pertencimento e amor permanece estrutural em toda simulação, mas sua ativação no
caso concreto deve variar de zero a cinco conforme as evidências da persona, do contexto e do ativo.
É proibido inferir que toda compra busca amor explicitamente. O schema exige tanto a força
estrutural quanto a ativação situacional e a fronteira da evidência.

O mesmo núcleo comportamental versionado é obrigatório em avaliações de ativos, observações
mobile, oportunidades e atividades BPM de landing e criativo. Parecer sem impulso afetivo e leitura
de pertencimento, admiração e amor é contratualmente inválido. O baseline pode continuar racional
porque funciona como grupo de controle; ele não é a personalidade operacional recomendada de Psique.

A compreensão dessas motivações serve para criar valor e reduzir esforço, nunca para explorar
vulnerabilidades. Psique deve bloquear recomendações que pressionem por vergonha, solidão, medo,
rejeição, insegurança, humilhação, engano ou dependência emocional.

### Base científica

- Kahneman e Tversky, teoria do prospecto (1979): https://doi.org/10.2307/1914185
- Zajonc, mera exposição (1968): https://doi.org/10.1037/h0025848
- Zajonc, afeto e preferência (1980): https://doi.org/10.1037/0003-066X.35.2.151
- Berlyne, novidade, complexidade e valor hedônico (1970): https://doi.org/10.3758/BF03212593
- Baumeister e Leary, necessidade de pertencimento (1995):
  https://doi.org/10.1037/0033-2909.117.3.497
- Leary et al., valor relacional e sociômetro (1995):
  https://doi.org/10.1037/0022-3514.68.3.518
- Kool et al., evitação de demanda cognitiva (2010): https://doi.org/10.1037/a0020198
- Loewenstein et al., risco como sentimento (2001):
  https://doi.org/10.1037/0033-2909.127.2.267

## Experiência Digital Observacional

Cada navegação deve nascer de uma persona, um objetivo e uma lista explícita de fontes públicas
autorizadas. O worker opera em sandbox somente leitura, com perfil mobile, sem login, formulário,
compra, publicação ou coleta de dados pessoais. Timelines pessoais irrestritas são proibidas;
feeds devem ser pesquisas públicas governadas por tema.

Antes da interpretação por IA, o worker deve abrir cada URL autorizada em Chromium com emulação
mobile, bloquear destinos fora dos hosts autorizados, registrar status, URL final, viewport,
títulos, CTAs, formulários e reprodução de vídeo, e capturar screenshot. Esses fatos determinísticos
são a única base técnica do parecer. Screenshots são enviados ao armazenamento governado como
`EXTERNAL_OBSERVATION`, vinculados à persona e à execução; falha na captura ou persistência bloqueia
a conclusão, sem fabricar uma avaliação.

A interpretação recebe somente os fatos já capturados e não pode navegar nem chamar ferramentas.
Sua execução usa sessão efêmera, schema versionado, saída final separada dos logs e limite de quatro
minutos. A reserva de uma observação vale por quinze minutos; ao consultar a fila, o backend encerra
como `FAILED` qualquer `RUNNING` mais antigo sem callback, preservando a causa auditável e impedindo
execuções indefinidamente presas. Uma reserva expirada nunca é reaberta automaticamente, evitando
processamento concorrente e memórias duplicadas.

A memória mantém quatro camadas imutavelmente separadas: `observation_json` registra fatos e URLs;
`simulated_reaction_json` registra a reação hipotética da persona;
`commercial_hypothesis_json` registra o teste sugerido; e `human_confirmation_json` recebe apenas
dados humanos oficiais posteriores. Nenhuma das três primeiras camadas eleva confiança ou valida
demanda por si mesma.

## Memória híbrida e evidências pesadas

O MySQL permanece como fonte de verdade de persona, observação, camada de memória, procedência,
checksum, retenção e confirmação humana. Screenshots, HTML preservado, vídeos, áudios e
transcrições pesadas ficam em bucket S3 privado e dedicado, sempre referenciados por um registro
canônico no MySQL. O bucket nunca deve ser público e o acesso ao conteúdo ocorre pelo backend.

As camadas `EXTERNAL_OBSERVATION`, `SIMULATED_INTERPRETATION`, `COMMERCIAL_HYPOTHESIS`,
`HUMAN_RESULT` e `CONFIRMED_LEARNING` permanecem separadas também no storage. Evidência simulada
nunca pode ser recategorizada automaticamente como resultado humano ou aprendizado confirmado.

Cada objeto exige SHA-256, tamanho, tipo de conteúdo, persona, fonte quando disponível, prazo de
retenção e vínculo opcional com a observação que o originou. Objetos idênticos da mesma persona e
camada são deduplicados. O prefixo inclui a persona e a camada para preservar isolamento lógico.
O ciclo de vida do bucket deve expirar objetos pelo prazo operacional configurado; metadados
canônicos continuam auditáveis e qualquer índice semântico futuro deve ser derivado e reconstruível.

Credenciais AWS nunca ficam no repositório. O backend usa IAM ou a cadeia padrão de credenciais do
ambiente. Busca vetorial não integra a primeira versão e não poderá se tornar fonte de verdade em
uma evolução futura.

## Vetor motivacional auditável

Cada experiência observacional concluída registra no MySQL um vetor separado da evidência pesada
mantida no S3. O vetor usa direção `AWAY_FROM_PAIN`, `TOWARD_PLEASURE` ou `MIXED`; intensidade de
dor e prazer; pesos de medo, frustração, esforço, alívio, desejo, confiança e pertencimento; força
da evidência; confiança da classificação; fonte e justificativa. Todos os pesos usam escala inteira
de zero a cinco.

Vetores calculados pelo agente são `SIMULATED_HYPOTHESIS`. Somente resultado humano recebido pelo
endpoint oficial cria `HUMAN_CONFIRMED`. Os registros são append-only: confirmação humana não
sobrescreve, promove ou apaga a hipótese simulada. Não há backfill automático das memórias antigas,
pois ausência de evidência não pode virar peso zero nem inferência retroativa.

O S3 continua armazenando o artefato original sem scores mutáveis. O MySQL permanece como fonte de
verdade da classificação, procedência e recalibração. A qualidade é medida pela correspondência
posterior entre pesos simulados e comportamento humano real, nunca pela intensidade estimada.

O bucket dedicado é provisionado pelo template versionado
`infra/aws/customer-agent-memory-bucket.yaml`. O deploy deve informar
`CUSTOMER_AGENT_MEMORY_BUCKET` e `CUSTOMER_AGENT_MEMORY_REGION`; permissões IAM mínimas devem ficar
restritas a `s3:PutObject` e `s3:GetObject` no prefixo `customer-agent-memory/v1/*` e
`s3:ListBucket` condicionado ao mesmo prefixo.
# Observabilidade operacional do worker

O `customer-agent-worker` deve persistir seus logs em arquivo e expor somente a leitura pelo endpoint operacional versionado `/ops-customer-agent-observability-v1/customer-agent-worker-log`. O MCP deve disponibilizar essa origem no módulo `customer-agent-worker` da ferramenta `java_module_logs`, permitindo correlacionar observações, execução do Codex, codecs do navegador e callbacks sem depender apenas do erro resumido persistido no backend.

Toda execução do Codex no Agente Cliente deve usar limite operacional padrão de 40 minutos, configurável por ambiente, e persistir a causa completa quando esse limite for excedido.

Avaliações devem enviar o prompt versionado pela entrada padrão do processo e usar o schema JSON
versionado com `--output-schema`. A resposta funcional deve ser lida exclusivamente do arquivo
indicado por `--output-last-message`, validada antes do callback e persistida separadamente do log
operacional. Saída parcial do processo, mensagens de progresso e diagnóstico nunca podem ser
tratados como parecer concluído.
