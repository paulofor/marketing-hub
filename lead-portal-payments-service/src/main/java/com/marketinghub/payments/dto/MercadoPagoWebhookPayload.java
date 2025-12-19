package com.marketinghub.payments.dto;

public class MercadoPagoWebhookPayload {

    private String id;
    private String type;
    private String action;
    private Data data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String extractResourceId() {
        if (data != null && data.id != null && !data.id.isBlank()) {
            return data.id;
        }
        return id;
    }

    public static class Data {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
