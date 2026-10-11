# Capella: qualidade audiovisual v2 — 2026-10-11

## Falha observada

Os vídeos #55/#56, rejeitados pelo operador, ampliavam uma captura longa da prova
técnica #685/#584 e repetiam ilustrações sintéticas de teste. O movimento sobre a
mesma tela não demonstrava a qualidade da entrega. Os pareceres de imagem não eram
pareceres do MP4 final. A main já contém a revisão temporal de Psique; esta correção
reutiliza esse mecanismo, sem criar outro gate.

## Escolha

Animar os placeholders preservaria a falha. Gerar todas as artes novamente consumiria
orçamento sem corrigir a seleção da fonte. A opção adotada é selecionar amostras da
biblioteca aprovada do próprio produto, com direitos e hash, e compor cenas variadas.
Amostra de catálogo não será chamada de captura da versão privada nem prova de vendas.
O pacote, preço e condições permanecem os mesmos. A seleção anterior continua rejeitada.

## Matriz local de aceite, antes de publicar

| Área | Verificação |
|---|---|
| Caminho feliz | Escolher fontes aprovadas pela tela, compor anúncio e demonstração, preservar voz auditada, encaminhar MP4 novo à revisão existente. |
| Qualidade visual | Artes reais da biblioteca, alternância de composição, mudanças de conteúdo e movimento perceptível; nenhum placeholder ou página longa reduzida. |
| Clareza | Oferta fiel, preço e condições legíveis, linguagem da cliente, sem prometer agenda/clientes garantidos. |
| Integração | Backend resolve fontes e identidade; executor confere bytes e compõe; resposta preserva manifesto e momentos de revisão. |
| Falhas | Rejeitar fonte de outro produto/tenant, não aprovada, sem direitos/hash, adulterada, repetição sem variedade e composição sem fonte. |
| Recuperação e custo | Voz idêntica reutilizada; contratos inválidos bloqueiam antes de TTS; sem publicar ou herdar aceite humano. |
| Regressões | Outro produto/execução e finalização anterior sem amostras continuam funcionando. |
| Dispositivos | Chromium desktop, iPhone e Pixel emulados; áudio, legendas e área segura. |
| Observabilidade | IDs, hashes, origem, tempos e resposta persistidos; testes segregados; custo desconhecido permanece desconhecido. |
| Resultado comercial | Só aferível após autorização e homologação; reprodução e testes locais não comprovam vendas. |

## Hipótese de aprendizado

Apolo deve selecionar uma fonte visual adequada ao que está sendo vendido, em vez de
usar automaticamente a primeira prova técnica disponível. Psique deve reprovar
monotonia ou artes inadequadas no MP4, mesmo quando o arquivo e a captura são íntegros.
Critério de adoção: regressões locais aprovadas e parecer independente do novo arquivo.
Critério de reversão: mistura de identidade, consumo duplicado ou perda de fidelidade.
Impacto em conversão e margem ainda não medido.

## Resultado local

- Backend: seleção da biblioteca e hashes legados, DTO HTTP, contexto de produto/tenant,
  metadados da montagem, compatibilidade V1/V2/V3 e revisão final testados. ArchUnit
  preserva a fachada existente; nenhuma alteração nas regras de arquitetura.
- Vídeo: renderizações completas de 27 s e 45 s com seis artes aprovadas de Capella,
  oito trechos de voz recuperados em cada vídeo, nenhum TTS novo. Arte e legenda têm
  áreas separadas; títulos curtos destacam benefício e R$ 67. Composição recalcula
  os intervalos a partir da voz real e registra fontes/momentos de revisão.
- Teste real identificou e corrigiu mistura indevida do áudio da fonte após substituir
  sua camada visual. Camadas estáticas são preparadas uma vez, evitando decodificar
  imagens grandes em todos os frames. Recuperação conserva custos pendentes.
- Navegação local com APIs simuladas: seleção pela tela, payload com IDs/títulos sem
  proveniência fornecida pelo cliente e sem autorizar nova locução. Desktop, iPhone
  15 Pro e Pixel 7 reproduziram ambos os MP4s 1080×1920, com áudio decodificado e sem erro.
- Os testes condicionais que exigem serviços externos não foram usados. Renderização
  FFmpeg, reprodução e fixtures de integração necessárias foram executadas localmente.
  Não houve escrita em produção nem nova chamada de IA durante a homologação local.
- Limite: frames e reprodução não comprovam naturalidade da voz, aceite da cliente ou
  conversão. O áudio foi preservado; Psique e a seleção humana continuam vinculados ao
  hash novo. A oferta permanece em preparação, sem autorização comercial decorrente
  desta correção.

## Largura do formulário na conferência publicada

A conferência detectou expansão da página de 1440 para 2983 px. A hipótese inicial
sobre descrições das artes foi descartada: o problema vinha do evento carregado
assincronamente, com JSON e identificadores extensos sem quebra no estado da
pós-produção. A correção permite quebra de texto e reduz o tamanho mínimo do bloco,
preservando a auditoria completa. Truncar os dados perderia evidência; refazer o
formulário seria desproporcional.

O teste existente `frontend/e2e/product-video-authoring-responsive.mjs` passa a
incluir pós-produção anterior e evento com URL/hash extensos, imagens carregadas,
seleção de artes e envio sem repetir locução. Mede overflow em desktop, iPhone e
Pixel com IDs segregados e nenhuma requisição externa. O cenário anterior não
fornecia eventos ao formulário e por isso não reproduzia a falha. Executar com
`FRONTEND_TEST_URL` apontando ao Vite local.

Regressão comprovada: sem a correção, o caso local expandiu 1440 para 6334 px;
com ela, passou nos três dispositivos, preservando também roteiro e importação.
A conferência visual publicada com o mesmo ajuste apenas no navegador retornou
1440 px, sem alterar dados ou enviar geração. Typecheck e build foram executados.

## Passagem para a revisão independente

A montagem publicada criou vídeos 65 (27 s) e 66 (45 s), com bytes e hashes
conferidos, 16 segmentos de voz reutilizados e nenhuma nova síntese. Reprodução
com áudio decodificado e controles passou em desktop, iPhone e Pixel.

A retomada do subprocesso criativo revelou fila circular com a espera de seleção.
Alternativas: pausar manualmente o processo de vendas, liberar toda concorrência ou
reconhecer a dependência exata. A última preserva o acompanhamento e custa menos
manutenção, sem liberar contextos independentes ou tarefas em curso.

Matriz local adicional: o caso original e outro produto devem ganhar vez para
revisão; produto/cadeia/ciclo/experimento diferentes, ciclo encerrado, pausa, falha,
etapa errada e tarefa real devem manter o bloqueio. A preparação comercial já
válida continua permitida. HTTP/JPA deve preservar espera humana, revisão, custos e
histórico, sem criar tarefa durante leitura. Os dois casos positivos falharam
antes da correção; nenhuma inferência ou cobrança é usada nos testes.

Validação: 328 testes das 15 suítes de execução, revisão final e arquitetura sem
falhas ou skips. A fixture HTTP foi corrigida para usar a chave do ciclo real em
vez da chave sem ciclo; depois passou em duas identidades. Spotless e revisão do
diff confirmaram comentários de responsabilidade em todas as classes/métodos
alterados. A correção não muda banco, orçamento, peça nem seleção humana.
