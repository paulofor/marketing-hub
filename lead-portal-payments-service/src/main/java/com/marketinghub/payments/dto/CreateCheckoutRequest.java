package com.marketinghub.payments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class CreateCheckoutRequest {

    @NotNull
    private Long packageId;

    @Email
    private String buyerEmail;

    private String buyerName;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }
}
