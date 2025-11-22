package com.marketinghub.leadportal.entity;

import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
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

    private String model;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = FlowQuestionListConverter.class)
    private List<FlowQuestion> questions;

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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<FlowQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<FlowQuestion> questions) {
        this.questions = questions;
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
        entity.setModel(flow.model());
        entity.setPrompt(flow.prompt());
        entity.setQuestions(flow.questions());
        return entity;
    }

    public Flow toModel() {
        return new Flow(slug, name, description, model, prompt, questions);
    }
}
