package com.marketinghub.leadportal.config;

import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.service.FlowService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlowDataInitializer implements CommandLineRunner {

    private final FlowService flowService;

    public FlowDataInitializer(FlowService flowService) {
        this.flowService = flowService;
    }

    @Override
    public void run(String... args) {
        if (!flowService.list().isEmpty()) {
            return;
        }
        flowService.save(buildDefaultFlow());
    }

    private Flow buildDefaultFlow() {
        return new Flow(
                "diagnostico-conteudo-estudios-fitness",
                "Diagnóstico de Conteúdo para Estúdios Fitness",
                "Fluxo que coleta informações estratégicas para sugerir pautas e formatos de conteúdo aos estúdios fitness.",
                "gpt-4o-mini",
                "Você é um estrategista de marketing especializado em academias e estúdios fitness. Utilize as respostas do lead para montar um plano de conteúdo com temas, formatos e chamadas que reforcem proposta de valor do negócio.",
                List.of(
                        new FlowQuestion(
                                "Qual é o nome da sua academia ou estúdio?",
                                "nome_estudio",
                                FlowQuestionType.TEXT,
                                true,
                                "Informe como o público conhece o seu negócio.",
                                "Ex.: Studio Movimento",
                                List.of()),
                        new FlowQuestion(
                                "Qual é o seu nome?",
                                "responsavel_nome",
                                FlowQuestionType.TEXT,
                                true,
                                "Usaremos esta informação para personalizar as recomendações.",
                                "Ex.: Ana Souza",
                                List.of()),
                        new FlowQuestion(
                                "Qual é o seu e-mail de contato?",
                                "email",
                                FlowQuestionType.EMAIL,
                                true,
                                "Enviaremos o plano de conteúdo para este endereço.",
                                "nome@exemplo.com",
                                List.of()),
                        new FlowQuestion(
                                "Qual é o principal objetivo do seu estúdio para os próximos 3 meses?",
                                "objetivo_principal",
                                FlowQuestionType.TEXTAREA,
                                true,
                                "Descreva metas ou desafios que deseja superar.",
                                "Ex.: Aumentar matrículas do plano anual",
                                List.of()),
                        new FlowQuestion(
                                "Qual é o perfil do público que deseja atrair?",
                                "publico_alvo",
                                FlowQuestionType.TEXTAREA,
                                true,
                                "Conte um pouco sobre faixa etária, nível de experiência ou interesses.",
                                "Ex.: Mulheres de 25 a 40 anos que buscam treinos funcionais",
                                List.of()),
                        new FlowQuestion(
                                "Quais formatos de conteúdo fazem mais sentido para a sua audiência?",
                                "formatos_interesse",
                                FlowQuestionType.MULTIPLE_CHOICE,
                                false,
                                "Selecione todos os formatos que gostaria de priorizar.",
                                null,
                                List.of(
                                        "Posts para Instagram",
                                        "Sequência de Stories",
                                        "Vídeos curtos (Reels/TikTok)",
                                        "E-mail marketing",
                                        "Artigos para blog")),
                        new FlowQuestion(
                                "Envie uma foto do seu espaço ou material de divulgação atual",
                                "foto_referencia",
                                FlowQuestionType.IMAGE_UPLOAD,
                                false,
                                "A imagem ajuda a gerar sugestões de criativos alinhadas com sua identidade visual.",
                                null,
                                List.of())));
    }
}
