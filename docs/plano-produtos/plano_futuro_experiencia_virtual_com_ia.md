# Plano Futuro — Plataforma de Experiências Virtuais com IA para Aprendizado Profundo

> **Objetivo:** transformar uma área de membros tradicional em uma plataforma onde o aluno aprende vivendo situações, tomando decisões, conversando com personagens, recebendo feedback e evoluindo por missões.

---

## 1. Visão do Produto

A ideia não é criar apenas uma plataforma onde o aluno assiste aulas.  
A ideia é criar uma **experiência educacional interativa**, onde o conteúdo vira uma situação prática.

### Aula tradicional

```txt
Professor explica → aluno assiste → aluno tenta aplicar depois
```

### Experiência virtual com IA

```txt
Aluno entra em um cenário → toma decisões → recebe consequências → recebe feedback → tenta melhorar → aplica o aprendizado
```

A plataforma deve fazer o aluno sentir que está dentro de uma situação real.

Exemplos:

- Curso de vendas: o aluno atende clientes simulados.
- Curso de marketing: o aluno lança campanhas fictícias e analisa resultados.
- Curso de liderança: o aluno resolve conflitos com personagens.
- Curso de programação: o aluno corrige problemas em sistemas simulados.
- Curso de atendimento: o aluno conversa com clientes insatisfeitos.
- Curso de empreendedorismo: o aluno administra uma empresa virtual.

---

## 2. Papel da Kiwify

A Kiwify pode ser usada como checkout e controle inicial de compra.

Fluxo:

```txt
Aluno compra na Kiwify
↓
Kiwify envia webhook de compra aprovada
↓
Backend Java recebe o evento
↓
Sistema cria/libera acesso do aluno
↓
Aluno entra na experiência virtual
```

Segundo a documentação da Kiwify, ao usar **área de membros externa**, a Kiwify processa o pagamento e envia o evento de compra aprovada via webhook para que a área externa entregue o acesso ao aluno.

Também existe documentação indicando que webhooks permitem enviar informações de um aplicativo para outro conforme eventos, mesmo quando não há integração nativa.

### Eventos importantes da Kiwify

Eventos que provavelmente devem ser tratados no backend:

```txt
compra_aprovada
compra_reembolsada
chargeback
subscription_canceled
subscription_late
subscription_renewed
```

### Regra importante

A Kiwify deve liberar o acesso comercial.  
A sua plataforma deve controlar:

- login;
- progresso;
- missões;
- feedback;
- histórico;
- IA;
- certificado;
- relatórios.

---

## 3. Conceito Principal da Plataforma

A plataforma pode ser pensada como um **motor de experiências educacionais**.

Em vez de cadastrar apenas vídeos, você cadastra:

```txt
Experiências
↓
Missões
↓
Cenários
↓
Personagens
↓
Decisões
↓
Consequências
↓
Feedback
↓
Progresso
```

### Exemplo de experiência

```txt
Experiência: Consultor de Vendas SaaS

Missão 1: Entender o problema do cliente
Missão 2: Apresentar solução
Missão 3: Lidar com objeção de preço
Missão 4: Negociar sem destruir valor
Missão 5: Fechar próximo passo
Missão 6: Receber relatório final
```

---

## 4. O Papel da IA

A IA não deve ser tratada como um simples chatbot.

Ela deve ter papéis específicos dentro da experiência.

### 4.1 Personagem

A IA simula uma pessoa dentro da experiência.

Exemplos:

```txt
cliente
chefe
colega de trabalho
paciente
investidor
aluno
recrutador
lead de vendas
usuário irritado
```

Exemplo:

```txt
Cliente: "Eu gostei da solução, mas achei cara."
```

O aluno precisa responder como se estivesse em uma situação real.

---

### 4.2 Tutor

A IA explica o que o aluno fez bem, o que poderia melhorar e qual conceito está por trás da situação.

Exemplo:

```txt
Você tentou resolver a objeção oferecendo desconto, mas pulou a etapa de diagnóstico. 
Antes de reduzir preço, tente entender se o problema é orçamento, confiança, prioridade ou valor percebido.
```

---

### 4.3 Avaliador

A IA mede habilidades do aluno.

Exemplo:

```json
{
  "escuta_ativa": 35,
  "clareza": 70,
  "argumentacao": 55,
  "empatia": 40,
  "controle_da_conversa": 45
}
```

---

### 4.4 Narrador

A IA ajuda a avançar a história.

Exemplo:

```txt
Depois da sua resposta, a cliente ficou mais insegura e perguntou se o preço inicial era realmente justo.
```

---

## 5. Arquitetura Geral

A arquitetura recomendada é:

```txt
Frontend React/Next.js
↓
Backend Java Spring Boot
↓
Banco PostgreSQL
↓
Serviço de IA
↓
Motor de experiências
↓
Kiwify via webhook
```

### Stack sugerida

```txt
Frontend:
- React ou Next.js
- Tailwind CSS
- Componentes interativos
- Chat da simulação
- Tela de missão
- Tela de progresso
- Relatório final

Backend:
- Java 21
- Spring Boot
- Spring Security
- API REST
- WebSocket opcional
- Integração com OpenAI
- Integração com Kiwify

Banco:
- PostgreSQL
- JSONB para critérios, respostas e feedbacks

Infra:
- Docker
- Docker Compose
- Nginx opcional
- Redis opcional
```

---

## 6. Fluxo Completo da Integração com IA

O ciclo principal é este:

```txt
1. Aluno entra em uma missão
2. Sistema apresenta o cenário
3. Aluno digita uma resposta ou escolhe uma ação
4. Frontend envia a ação para o backend Java
5. Java busca contexto da missão no banco
6. Java busca progresso anterior do aluno
7. Java monta o prompt para a IA
8. Java chama a API da IA
9. IA devolve resposta estruturada em JSON
10. Java valida a resposta
11. Java salva o feedback e a pontuação
12. Java atualiza o progresso
13. Frontend mostra a consequência ao aluno
```

### Regra essencial

```txt
Frontend nunca deve chamar a IA diretamente.
```

O correto é:

```txt
React/Next.js → Java Spring Boot → API da IA
```

Motivos:

- protege a chave da API;
- controla custo;
- valida se o aluno tem acesso;
- evita abuso;
- salva progresso;
- aplica regras do produto;
- permite auditoria.

---

## 7. Exemplo de Fluxo Real

### Cenário

```txt
Missão: Cliente achou caro
Objetivo: ensinar o aluno a lidar com objeção de preço
```

### Personagem

```txt
Nome: Marina
Perfil: cliente interessada, mas insegura
Comportamento: compara preços e questiona valor percebido
```

### Fala inicial da personagem

```txt
"Eu gostei da solução, mas achei cara. Tem outras opções mais baratas no mercado."
```

### Resposta do aluno

```txt
"Posso te dar 20% de desconto se você fechar agora."
```

### O backend envia para a IA

```json
{
  "missao": "Cliente achou caro",
  "objetivo_pedagogico": "Ensinar objeção de preço sem oferecer desconto cedo demais",
  "personagem": {
    "nome": "Marina",
    "perfil": "Cliente interessada, mas insegura",
    "comportamento": "Questiona preço, compara concorrentes e testa segurança do vendedor"
  },
  "historico_aluno": "Aluno costuma oferecer solução antes de investigar o problema",
  "resposta_aluno": "Posso te dar 20% de desconto se você fechar agora.",
  "criterios": [
    "escuta ativa",
    "diagnóstico",
    "argumentação",
    "controle da negociação",
    "próximo passo"
  ]
}
```

### IA responde em JSON

```json
{
  "fala_personagem": "Se você consegue dar 20% de desconto tão rápido, será que o preço inicial não estava alto demais?",
  "feedback_tutor": "Você tentou remover a objeção, mas ofereceu desconto antes de investigar o motivo real da dúvida. Uma abordagem melhor seria perguntar o que exatamente fez a cliente perceber o valor como alto.",
  "pontuacao": 58,
  "habilidades_avaliadas": {
    "escuta_ativa": 30,
    "diagnostico": 25,
    "argumentacao": 55,
    "controle_da_negociacao": 40,
    "proximo_passo": 50
  },
  "proxima_etapa": "investigar_motivo_da_objeção",
  "liberar_proxima_missao": false,
  "resumo_para_historico": "Aluno ofereceu desconto cedo demais e precisa treinar diagnóstico antes de negociar preço."
}
```

---

## 8. Por que usar JSON estruturado?

Se a IA responder em texto livre, o sistema fica frágil.

Exemplo ruim:

```txt
"Você foi bem, mas poderia melhorar."
```

Isso é difícil de transformar em progresso, nota, relatório e desbloqueio de missão.

O ideal é a IA responder sempre com um formato previsível.

### Exemplo de schema

```json
{
  "type": "object",
  "properties": {
    "fala_personagem": { "type": "string" },
    "feedback_tutor": { "type": "string" },
    "pontuacao": { "type": "integer" },
    "habilidades_avaliadas": {
      "type": "object",
      "additionalProperties": { "type": "integer" }
    },
    "proxima_etapa": { "type": "string" },
    "liberar_proxima_missao": { "type": "boolean" },
    "resumo_para_historico": { "type": "string" }
  },
  "required": [
    "fala_personagem",
    "feedback_tutor",
    "pontuacao",
    "habilidades_avaliadas",
    "proxima_etapa",
    "liberar_proxima_missao",
    "resumo_para_historico"
  ],
  "additionalProperties": false
}
```

### DTO Java

```java
public record AiMissionResult(
    String falaPersonagem,
    String feedbackTutor,
    Integer pontuacao,
    Map<String, Integer> habilidadesAvaliadas,
    String proximaEtapa,
    Boolean liberarProximaMissao,
    String resumoParaHistorico
) {}
```

---

## 9. Serviços no Backend Java

A aplicação Java pode ser dividida em serviços.

```txt
AuthService
→ login, sessão, JWT, segurança

KiwifyWebhookService
→ recebe compra aprovada, reembolso, chargeback e cancelamento

ExperienceService
→ gerencia experiências

MissionService
→ gerencia missões

ExperienceAiService
→ coordena o uso da IA nas missões

OpenAiGateway
→ faz a chamada para a API da IA

ProgressService
→ salva evolução do aluno

ReportService
→ gera relatório final da experiência

CertificateService
→ gera certificado se aplicável
```

---

## 10. Endpoint Principal

O frontend chama este endpoint quando o aluno responde uma missão.

```http
POST /api/experiencias/{experienceId}/missoes/{missionId}/responder
```

Body:

```json
{
  "respostaAluno": "Eu daria 20% de desconto para fechar agora."
}
```

Resposta:

```json
{
  "falaPersonagem": "Se você consegue dar desconto tão rápido, o preço inicial era real?",
  "feedbackTutor": "Você ofereceu desconto cedo demais. Tente investigar a objeção antes.",
  "pontuacao": 58,
  "habilidadesAvaliadas": {
    "escuta_ativa": 30,
    "argumentacao": 55
  },
  "proximaEtapa": "investigar_motivo_da_objeção",
  "liberarProximaMissao": false,
  "resumoParaHistorico": "Aluno precisa investigar antes de negociar."
}
```

---

## 11. Controller Java

```java
@RestController
@RequestMapping("/api/experiencias")
public class ExperienceController {

    private final ExperienceAiService experienceAiService;

    public ExperienceController(ExperienceAiService experienceAiService) {
        this.experienceAiService = experienceAiService;
    }

    @PostMapping("/{experienceId}/missoes/{missionId}/responder")
    public AiMissionResult responderMissao(
            @PathVariable String experienceId,
            @PathVariable String missionId,
            @RequestBody StudentAnswerRequest request,
            Principal principal
    ) {
        String studentId = principal.getName();

        return experienceAiService.processarResposta(
                studentId,
                experienceId,
                missionId,
                request.respostaAluno()
        );
    }
}
```

DTO:

```java
public record StudentAnswerRequest(
    String respostaAluno
) {}
```

---

## 12. Serviço de Orquestração da IA

```java
@Service
public class ExperienceAiService {

    private final MissionRepository missionRepository;
    private final StudentProgressRepository progressRepository;
    private final OpenAiGateway openAiGateway;
    private final ProgressService progressService;

    public ExperienceAiService(
            MissionRepository missionRepository,
            StudentProgressRepository progressRepository,
            OpenAiGateway openAiGateway,
            ProgressService progressService
    ) {
        this.missionRepository = missionRepository;
        this.progressRepository = progressRepository;
        this.openAiGateway = openAiGateway;
        this.progressService = progressService;
    }

    public AiMissionResult processarResposta(
            String studentId,
            String experienceId,
            String missionId,
            String respostaAluno
    ) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Missão não encontrada"));

        StudentProgress progress = progressRepository
                .findByStudentIdAndExperienceId(studentId, experienceId)
                .orElseThrow(() -> new RuntimeException("Progresso não encontrado"));

        AiPrompt prompt = montarPrompt(mission, progress, respostaAluno);

        AiMissionResult aiResult = openAiGateway.evaluateMission(prompt);

        validarResultado(aiResult);

        progressService.aplicarResultado(
                studentId,
                experienceId,
                missionId,
                respostaAluno,
                aiResult
        );

        return aiResult;
    }

    private AiPrompt montarPrompt(
            Mission mission,
            StudentProgress progress,
            String respostaAluno
    ) {
        return new AiPrompt(
                mission.getTitle(),
                mission.getPedagogicalGoal(),
                mission.getContext(),
                mission.getCharacterProfile(),
                progress.getSummary(),
                respostaAluno,
                mission.getEvaluationCriteria()
        );
    }

    private void validarResultado(AiMissionResult result) {
        if (result.pontuacao() == null || result.pontuacao() < 0 || result.pontuacao() > 100) {
            throw new RuntimeException("Pontuação inválida retornada pela IA");
        }

        if (result.falaPersonagem() == null || result.falaPersonagem().isBlank()) {
            throw new RuntimeException("Fala do personagem ausente");
        }

        if (result.feedbackTutor() == null || result.feedbackTutor().isBlank()) {
            throw new RuntimeException("Feedback ausente");
        }
    }
}
```

---

## 13. Prompt Base

Este é um exemplo de prompt que o backend pode montar.

```txt
Você é uma IA educacional para uma plataforma de experiências práticas.

Você atua em três papéis ao mesmo tempo:
1. personagem da simulação;
2. tutor pedagógico;
3. avaliador de habilidades.

Objetivo:
Ajudar o aluno a aprender por meio de prática, consequência e reflexão.

Regras:
- Não dê uma aula longa.
- Não entregue a resposta perfeita imediatamente.
- Simule uma consequência realista.
- Dê feedback curto, claro e útil.
- Avalie o aluno com base nos critérios da missão.
- Não invente dados fora do contexto recebido.
- Não libere próxima missão se os critérios mínimos não forem atingidos.
- Responda apenas no JSON Schema solicitado.

Missão:
{{titulo_missao}}

Objetivo pedagógico:
{{objetivo_pedagogico}}

Contexto:
{{contexto}}

Personagem:
{{perfil_personagem}}

Histórico resumido do aluno:
{{historico_aluno}}

Critérios de avaliação:
{{criterios}}

Resposta do aluno:
{{resposta_aluno}}
```

---

## 14. OpenAiGateway — Integração Conceitual

Este componente isola a chamada para a IA.

```java
@Component
public class OpenAiGateway {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAiGateway(
            @Value("${openai.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public AiMissionResult evaluateMission(AiPrompt prompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4.1-mini",
                "instructions", criarInstrucoesSistema(),
                "input", criarInput(prompt),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "ai_mission_result",
                                "strict", true,
                                "schema", criarSchema()
                        )
                )
        );

        String response = webClient.post()
                .uri("/responses")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extrairResultado(response);
    }

    private String criarInstrucoesSistema() {
        return """
        Você é uma IA educacional para experiências práticas.
        Você atua como personagem, tutor e avaliador.
        Sempre responda no JSON Schema solicitado.
        Não invente progresso fora dos dados recebidos.
        Não libere missão se os critérios mínimos não forem atendidos.
        """;
    }

    private String criarInput(AiPrompt prompt) {
        return """
        Missão:
        %s

        Objetivo pedagógico:
        %s

        Contexto:
        %s

        Personagem:
        %s

        Histórico do aluno:
        %s

        Critérios:
        %s

        Resposta do aluno:
        %s
        """.formatted(
                prompt.tituloMissao(),
                prompt.objetivoPedagogico(),
                prompt.contexto(),
                prompt.personagem(),
                prompt.historicoAluno(),
                prompt.criterios(),
                prompt.respostaAluno()
        );
    }

    private Map<String, Object> criarSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "fala_personagem", Map.of("type", "string"),
                        "feedback_tutor", Map.of("type", "string"),
                        "pontuacao", Map.of("type", "integer"),
                        "habilidades_avaliadas", Map.of(
                                "type", "object",
                                "additionalProperties", Map.of("type", "integer")
                        ),
                        "proxima_etapa", Map.of("type", "string"),
                        "liberar_proxima_missao", Map.of("type", "boolean"),
                        "resumo_para_historico", Map.of("type", "string")
                ),
                "required", List.of(
                        "fala_personagem",
                        "feedback_tutor",
                        "pontuacao",
                        "habilidades_avaliadas",
                        "proxima_etapa",
                        "liberar_proxima_missao",
                        "resumo_para_historico"
                ),
                "additionalProperties", false
        );
    }

    private AiMissionResult extrairResultado(String response) {
        // Implementar parser do envelope da API.
        // Idealmente criar DTOs específicos para a resposta da OpenAI
        // e mapear o JSON final para AiMissionResult.
        throw new UnsupportedOperationException("Implementar parser da resposta");
    }
}
```

> Observação: o exemplo acima é conceitual. A implementação real deve tratar erro, timeout, retry, logs, custo, limites e parsing robusto.

---

## 15. Function Calling — Quando Usar

No MVP, não é obrigatório usar function calling.

Comece simples:

```txt
Java busca os dados
↓
Java manda tudo no prompt
↓
IA responde em JSON
↓
Java salva
```

Function calling entra em uma fase mais avançada.

### Quando usar function calling?

Quando a IA precisar solicitar dados ou ações externas.

Exemplos:

```txt
buscarHistoricoAluno(alunoId)
buscarConteudoDeApoio(missaoId)
buscarTentativasAnteriores(alunoId, missaoId)
gerarRelatorioFinal(alunoId)
recomendarProximaMissao(alunoId)
```

Fluxo com function calling:

```txt
1. Java chama IA
2. IA percebe que precisa de dados
3. IA solicita uma função
4. Java executa a função
5. Java devolve o resultado para IA
6. IA gera a resposta final
```

### Importante

A IA não deve alterar banco diretamente.

Correto:

```txt
IA sugere → Java valida → Java executa
```

Errado:

```txt
IA decide sozinha → altera progresso
```

---

## 16. Modelo de Dados Inicial

### experiences

```sql
CREATE TABLE experiences (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
```

### missions

```sql
CREATE TABLE missions (
    id UUID PRIMARY KEY,
    experience_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    pedagogical_goal TEXT NOT NULL,
    context TEXT NOT NULL,
    character_profile TEXT NOT NULL,
    evaluation_criteria JSONB NOT NULL,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
```

### students

```sql
CREATE TABLE students (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    kiwify_customer_id VARCHAR(255),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);
```

### student_progress

```sql
CREATE TABLE student_progress (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    experience_id UUID NOT NULL,
    current_mission_id UUID,
    score INTEGER DEFAULT 0,
    summary TEXT,
    completed BOOLEAN DEFAULT false,
    updated_at TIMESTAMP DEFAULT now()
);
```

### student_answers

```sql
CREATE TABLE student_answers (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    mission_id UUID NOT NULL,
    answer TEXT NOT NULL,
    ai_feedback JSONB NOT NULL,
    score INTEGER,
    created_at TIMESTAMP DEFAULT now()
);
```

### access_events

```sql
CREATE TABLE access_events (
    id UUID PRIMARY KEY,
    student_email VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    raw_payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);
```

---

## 17. Docker Compose Inicial

```yaml
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/experiencias
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: app
      KIWFY_WEBHOOK_SECRET: ${KIWFY_WEBHOOK_SECRET}
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080
    depends_on:
      - backend

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: experiencias
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Arquivo `.env`:

```env
OPENAI_API_KEY=sua_chave_openai
KIWFY_WEBHOOK_SECRET=segredo_do_webhook
```

> Observação: confirmar o nome correto do segredo/header de validação usado pela Kiwify na implementação real.

---

## 18. Segurança e Controle de Custos

### Segurança

- Nunca expor `OPENAI_API_KEY` no frontend.
- Validar login antes de permitir missão.
- Validar se o aluno tem compra ativa.
- Registrar payloads de webhook.
- Validar autenticidade do webhook.
- Bloquear aluno reembolsado, cancelado ou com chargeback.
- Sanitizar entradas do aluno.
- Limitar tamanho de resposta.
- Criar rate limit por aluno.

### Custos

- Usar modelo menor para avaliações simples.
- Resumir histórico em vez de mandar tudo.
- Guardar `summary` do aluno.
- Evitar chamadas desnecessárias.
- Cachear conteúdo fixo da missão.
- Definir limite de tentativas por missão.
- Monitorar custo por aluno.

---

## 19. Memória do Aluno

Não é necessário mandar todo o histórico do aluno para a IA.

O ideal é manter um resumo atualizado.

Exemplo:

```txt
Aluno tem dificuldade em fazer perguntas antes de propor solução.
Nas últimas missões, respondeu rápido demais e demonstrou baixa escuta ativa.
Melhorou na clareza, mas ainda precisa treinar diagnóstico.
```

Esse resumo pode ficar em:

```txt
student_progress.summary
```

A cada missão, a IA devolve:

```json
{
  "resumo_para_historico": "Aluno ofereceu desconto antes de investigar. Deve treinar diagnóstico."
}
```

O backend atualiza o resumo geral.

---

## 20. Relatório Final

Ao final da experiência, a IA pode ajudar a gerar um relatório.

### Entrada para a IA

```json
{
  "experiencia": "Consultor de Vendas SaaS",
  "missoes_concluidas": 5,
  "pontuacoes": [55, 68, 72, 80, 76],
  "principais_erros": [
    "ofereceu desconto cedo",
    "falhou em investigar objeção",
    "não fechou próximo passo"
  ],
  "principais_evolucoes": [
    "melhorou clareza",
    "passou a fazer mais perguntas",
    "controlou melhor a conversa"
  ]
}
```

### Saída

```json
{
  "resumo_geral": "Você evoluiu em clareza e controle da conversa, mas ainda precisa melhorar diagnóstico.",
  "pontos_fortes": ["clareza", "persistência", "estrutura"],
  "pontos_de_melhoria": ["escuta ativa", "investigação", "negociação"],
  "plano_de_estudo": [
    "treinar perguntas abertas",
    "rever objeções de preço",
    "praticar fechamento consultivo"
  ]
}
```

---

## 21. MVP Recomendado

Não começar com uma plataforma gigante.

Começar com uma experiência pequena, mas muito bem feita.

### MVP 1

```txt
1 experiência
5 missões
3 personagens
1 tipo de relatório final
1 integração com Kiwify
1 tela de progresso
1 painel simples de admin
```

### Exemplo de MVP

```txt
Produto: Simulador de Vendas Consultivas

Missão 1: identificar problema
Missão 2: fazer perguntas
Missão 3: apresentar solução
Missão 4: lidar com preço
Missão 5: fechar próximo passo
```

---

## 22. Roadmap

### Fase 1 — Fundação

```txt
- Criar backend Java
- Criar frontend
- Criar login
- Criar banco
- Criar integração Kiwify
- Criar estrutura de experiências e missões
```

### Fase 2 — IA básica

```txt
- Criar ExperienceAiService
- Criar OpenAiGateway
- Criar prompt base
- Criar JSON estruturado
- Salvar feedback e pontuação
```

### Fase 3 — Primeira experiência

```txt
- Criar experiência de vendas
- Criar 5 missões
- Criar personagens
- Criar critérios de avaliação
- Criar tela de missão
- Criar tela de resultado
```

### Fase 4 — Relatórios

```txt
- Gerar relatório final
- Medir habilidades
- Mostrar evolução
- Recomendar próximos passos
```

### Fase 5 — Painel admin

```txt
- Criar experiências
- Criar missões
- Editar personagens
- Ver respostas dos alunos
- Ver métricas de conclusão
```

### Fase 6 — Avançado

```txt
- Function calling
- Recomendação automática de próxima missão
- WebSocket
- Personagens com memória
- Simulações por voz
- Certificados
- Ranking ou gamificação
```

---

## 23. O Que Não Fazer no Começo

Evitar no MVP:

```txt
- mundo 3D complexo
- multiplayer
- avatares avançados
- engine de jogo
- function calling complexo
- várias experiências ao mesmo tempo
- muitos tipos de relatório
- comunidade interna completa
```

O ideal é validar primeiro se a experiência de aprendizado funciona.

---

## 24. Nomes Possíveis para o Produto

```txt
Academia de Experiências
Laboratório Virtual
Simulador de Aprendizagem
Campus Interativo
Jornada Prática
Treinamento Imersivo
Missões de Aprendizagem
```

---

## 25. Resumo da Ideia

A plataforma deve transformar conteúdo em prática.

```txt
Não é:
"Assista esta aula sobre vendas."

É:
"Entre nesta situação, converse com o cliente, tome uma decisão, veja a consequência e aprenda com o feedback."
```

A IA funciona como:

```txt
personagem
+ tutor
+ avaliador
+ narrador
```

O Java funciona como:

```txt
segurança
+ regras
+ progresso
+ acesso
+ integração Kiwify
+ controle da IA
```

O frontend funciona como:

```txt
experiência visual
+ chat
+ missões
+ feedback
+ progresso
```

---

## 26. Decisão Técnica Recomendada

Para o futuro, a stack recomendada é:

```txt
Java 21
Spring Boot
PostgreSQL
React ou Next.js
OpenAI Responses API
Structured Outputs
Docker Compose
Kiwify Webhooks
```

Começar sem function calling.  
Adicionar function calling apenas quando houver necessidade real de a IA consultar ferramentas ou executar ações.

---

## 27. Fontes Consultadas

- Kiwify — Como cadastrar produto / área de membros externa: https://ajuda.kiwify.com.br/pt-br/article/como-cadastrar-o-seu-produto-1lxh5g7/
- Kiwify — Integrações e webhooks: https://ajuda.kiwify.com.br/pt-br/category/integracoes-1633r3w/
- Kiwify — Integração Memberkit como exemplo de área externa: https://ajuda.kiwify.com.br/pt-br/article/como-integrar-com-o-memberkit-1dfehr3/
- OpenAI — Function calling: https://developers.openai.com/api/docs/guides/function-calling
- OpenAI — Tools / Responses API: https://developers.openai.com/api/docs/guides/tools

---

## 28. Próximo Passo Prático

O próximo passo ideal é criar um protótipo com apenas uma missão.

### Protótipo mínimo

```txt
Tela:
"Cliente achou caro"

Aluno:
digita uma resposta

Backend:
envia para IA

IA:
devolve fala do cliente + feedback + pontuação

Sistema:
salva e mostra resultado
```

Esse protótipo já prova a parte mais importante do produto:  
**a IA transformando uma ação do aluno em consequência, feedback e aprendizado.**
