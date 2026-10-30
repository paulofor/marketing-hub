# Capella: continuação compreensível nos estados sem pacote

## Evidência e decisão

No ciclo #17, a homologação técnica #877 aprovou nove cenários/21 capturas.
Psique #878 e #879 aprovaram experiência e recuperação. Em #880, identificou
corretamente que o botão de segurança levava ao painel administrativo e delegava
à profissional a verificação das imagens da biblioteca. A origem está tanto no
texto do backend quanto no contrato técnico que exigia esse endereço.

| Alternativa | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Alterar só o rótulo | Mudança pequena | Mantém o destino e a responsabilidade errados | Descartada |
| Criar atendimento separado | Poderia encaminhar um pedido | Canal e executor não demonstrados; aumenta escopo e dependências | Descartada |
| Explicar a responsabilidade na própria experiência | Continuação compreensível com os dados preservados | Pequena alteração compartilhada e teste real do clique | Adotada |

O link agora revela uma seção local. A equipe do produto é responsável pela
verificação, sem alegar que ela já está em execução. O estado continua bloqueado,
sem arquivos, inferência, publicação ou cobrança. Falhas de composição recebem
a mesma separação entre o que depende da equipe e o que cabe à cliente.

## Matriz definida antes da execução

| Cobertura | Validação |
| --- | --- |
| Caminho aderente e recuperação antes válidos | Cópia, download íntegro, falha controlada e retomada |
| Segurança original | Origem visual não comprovada e pedido de ação externa |
| Integração | Backend real, MySQL 5.7 e compositor real; contratos de agentes simulados |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Outro contexto | Produto/ciclo/perfil diferentes, incluindo barber-v1 |
| Observabilidade e isolamento | Capturas, manifesto, hash e eventos de QA, sem modelo ou efeito comercial |
| Continuação | Clique revela a explicação no mesmo protótipo; sem consulta ou navegação administrativa |
| Regressão do bloqueio | Reabrir não muda entrada, causa, apresentação, eventos ou ausência de pacote |

## Resultados locais

- 43 testes backend aprovados.
- 66 testes Java relevantes de Psique aprovados; dois casos condicionais pulados.
- 10 testes JavaScript aprovados, incluindo a rejeição da orientação administrativa.
- Matriz principal: nove cenários aprovados e 21 capturas.
- Outro produto/perfil legado: segurança aprovada e uma captura.
- Inspeção visual da orientação ampliada em Pixel: ação e responsabilidade legíveis,
  proteção e ausência de resultado preservadas.
- Sintaxe JavaScript e diff conferidos. Imagens e ZIPs continuam produzidos pelo
  compositor versionado; nenhum provedor pago foi acionado nos testes.

Artefatos efêmeros: `safety-acceptance-matrix.json`, `safety-harness-output.json`,
`safety-legacy-output.json` e capturas da execução local. Resultados sintéticos não
constituem parecer de produção nem evidência de preferência, demanda ou margem.

## Continuação e limites

A mudança de mensagem/CTA exige candidata sucessora pela política vigente.
Preservar os pareceres, artefatos, erros, custos e autorizações do ciclo #17;
vincular explicitamente o que for reutilizável. Os gates independentes e o aceite
humano aplicável permanecem obrigatórios. Não ampliar os tetos nem autorizar novo
audiovisual pago para acomodar a correção.
