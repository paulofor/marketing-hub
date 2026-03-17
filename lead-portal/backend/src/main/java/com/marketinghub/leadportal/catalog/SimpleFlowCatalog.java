package com.marketinghub.leadportal.catalog;

import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Keeps the catalog of simple flows that should be served without hitting the database.
 */
@Component
public class SimpleFlowCatalog {

    private static final String PERSONAL_TRAINER_SLUG = "formulario-simples-personal-trainer";

    private final Map<String, Flow> simpleFlows;

    public SimpleFlowCatalog() {
        this.simpleFlows = Map.of(PERSONAL_TRAINER_SLUG, buildPersonalTrainerFlow());
    }

    public Optional<Flow> find(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(simpleFlows.get(slug));
    }

    public boolean supports(String slug) {
        if (slug == null) {
            return false;
        }
        return simpleFlows.containsKey(slug);
    }

    public Collection<Flow> list() {
        return simpleFlows.values();
    }

    private Flow buildPersonalTrainerFlow() {
        return new Flow(
                PERSONAL_TRAINER_SLUG,
                "Formulário simples para personal trainer",
                "Fluxo simples para coleta inicial de informações sem necessidade de envio de imagens.",
                "manual",
                null,
                null,
                null,
                null,
                List.of(
                        question("Nome", "nome", FlowQuestionType.TEXT, true),
                        question(
                                "Trabalha em alguma academia ou studio? Qual nome?",
                                "academia_ou_studio",
                                FlowQuestionType.TEXT,
                                false),
                        singleChoiceQuestion(
                                "Forma de contato",
                                "forma_contato",
                                false,
                                List.of("Telefone", "WhatsApp", "Instagram")),
                        multipleChoiceQuestion(
                                "Tipo de aulas que presta",
                                "tipo_aulas",
                                true,
                                List.of("Musculação", "Yoga", "Outros")),
                        question(
                                "Se marcou outros, descreva quais aulas presta",
                                "outras_aulas",
                                FlowQuestionType.TEXTAREA,
                                false),
                        question("Email", "email", FlowQuestionType.TEXT, true)),
                defaultStyle());
    }

    private SimpleFormStyle defaultStyle() {
        SimpleFormStyleDefinition definition = new SimpleFormStyleDefinition(
                "#f5f6ff",
                "linear-gradient(135deg, #eef2ff 0%, #fdf2f8 100%)",
                null,
                "#ffffff",
                "rgba(99,102,241,0.08)",
                "0 25px 70px rgba(15,23,42,0.12)",
                "#0f172a",
                "#1f2937",
                "#6b7280",
                "#6366f1",
                "#ec4899",
                "linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)",
                "#ffffff",
                "0 10px 24px rgba(99,102,241,0.35)",
                "999px",
                "rgba(99,102,241,0.08)",
                "#ffffff",
                "#e5e7eb",
                "image-right",
                null,
                "rgba(255,255,255,0.65)");
        return new SimpleFormStyle(
                "formulario-simples-default",
                "Gradiente padrão lead portal",
                definition);
    }

    private FlowQuestion question(String title, String dataKey, FlowQuestionType type, boolean required) {
        return new FlowQuestion(title, dataKey, type, required, null, null, List.of());
    }

    private FlowQuestion singleChoiceQuestion(
            String title, String dataKey, boolean required, List<String> options) {
        return new FlowQuestion(title, dataKey, FlowQuestionType.SINGLE_CHOICE, required, null, null, options);
    }

    private FlowQuestion multipleChoiceQuestion(
            String title, String dataKey, boolean required, List<String> options) {
        return new FlowQuestion(title, dataKey, FlowQuestionType.MULTIPLE_CHOICE, required, null, null, options);
    }
}
