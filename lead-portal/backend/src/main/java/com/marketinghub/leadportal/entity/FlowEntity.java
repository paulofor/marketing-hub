package com.marketinghub.leadportal.entity;

import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import com.marketinghub.leadportal.entity.converter.SimpleFormStyleDefinitionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "flows")
public class FlowEntity {

    @Id
    @Column(length = 190, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "custom_form_html", columnDefinition = "LONGTEXT")
    private String customFormHtml;

    private String model;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "image_prompt_model", length = 128)
    private String imagePromptModel;

    @Column(name = "image_prompt_template", columnDefinition = "LONGTEXT")
    private String imagePromptTemplate;

    @Column(name = "image_batch_size")
    private Integer imageBatchSize;

    @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = FlowQuestionListConverter.class)
    private List<FlowQuestion> questions;

    @Column(name = "simple_form_style_slug", length = 120)
    private String simpleFormStyleSlug;

    @Column(name = "simple_form_style_name", length = 150)
    private String simpleFormStyleName;

    @Column(name = "simple_form_style_definition", columnDefinition = "LONGTEXT")
    @Convert(converter = SimpleFormStyleDefinitionConverter.class)
    private SimpleFormStyleDefinition simpleFormStyleDefinition;

    @Column(name = "access_count", nullable = false, columnDefinition = "bigint default 0")
    private long accessCount = 0L;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCustomFormHtml() {
        return customFormHtml;
    }

    public void setCustomFormHtml(String customFormHtml) {
        this.customFormHtml = customFormHtml;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImagePromptModel() {
        return imagePromptModel;
    }

    public void setImagePromptModel(String imagePromptModel) {
        this.imagePromptModel = imagePromptModel;
    }

    public String getImagePromptTemplate() {
        return imagePromptTemplate;
    }

    public void setImagePromptTemplate(String imagePromptTemplate) {
        this.imagePromptTemplate = imagePromptTemplate;
    }

    public Integer getImageBatchSize() {
        return imageBatchSize;
    }

    public void setImageBatchSize(Integer imageBatchSize) {
        this.imageBatchSize = imageBatchSize;
    }

    public List<FlowQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<FlowQuestion> questions) {
        this.questions = questions;
    }

    public String getSimpleFormStyleSlug() {
        return simpleFormStyleSlug;
    }

    public void setSimpleFormStyleSlug(String simpleFormStyleSlug) {
        this.simpleFormStyleSlug = simpleFormStyleSlug;
    }

    public String getSimpleFormStyleName() {
        return simpleFormStyleName;
    }

    public void setSimpleFormStyleName(String simpleFormStyleName) {
        this.simpleFormStyleName = simpleFormStyleName;
    }

    public SimpleFormStyleDefinition getSimpleFormStyleDefinition() {
        return simpleFormStyleDefinition;
    }

    public void setSimpleFormStyleDefinition(SimpleFormStyleDefinition simpleFormStyleDefinition) {
        this.simpleFormStyleDefinition = simpleFormStyleDefinition;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }

    public static FlowEntity fromModel(Flow flow) {
        FlowEntity entity = new FlowEntity();
        entity.setSlug(flow.slug());
        entity.setName(flow.name());
        entity.setDescription(flow.description());
        entity.setCustomFormHtml(flow.customFormHtml());
        entity.setModel(flow.model());
        entity.setPrompt(flow.prompt());
        entity.setImagePromptModel(flow.imagePromptModel());
        entity.setImagePromptTemplate(flow.imagePromptTemplate());
        entity.setImageBatchSize(flow.imageBatchSize());
        entity.setQuestions(flow.questions());
        if (flow.simpleFormStyle() != null) {
            entity.setSimpleFormStyleSlug(flow.simpleFormStyle().slug());
            entity.setSimpleFormStyleName(flow.simpleFormStyle().name());
            entity.setSimpleFormStyleDefinition(flow.simpleFormStyle().definition());
        } else {
            entity.setSimpleFormStyleSlug(null);
            entity.setSimpleFormStyleName(null);
            entity.setSimpleFormStyleDefinition(null);
        }
        return entity;
    }

    public Flow toModel() {
        SimpleFormStyle style = null;
        if (simpleFormStyleSlug != null || simpleFormStyleName != null || simpleFormStyleDefinition != null) {
            style = new SimpleFormStyle(simpleFormStyleSlug, simpleFormStyleName, simpleFormStyleDefinition);
        }
        return new Flow(slug, name, description, customFormHtml, model, prompt, imagePromptModel, imagePromptTemplate, imageBatchSize, questions, style);
    }
}
