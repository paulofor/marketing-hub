package com.marketinghub.leadportal.dto;

/**
 * Payload for updating the image prompt configuration of a lead portal flow.
 */
public class UpdateLeadPortalImagePromptRequest {

    private String imagePromptModel;
    private String imagePromptTemplate;
    private Integer imagePromptBatchSize;

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

    public Integer getImagePromptBatchSize() {
        return imagePromptBatchSize;
    }

    public void setImagePromptBatchSize(Integer imagePromptBatchSize) {
        this.imagePromptBatchSize = imagePromptBatchSize;
    }
}
