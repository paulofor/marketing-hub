package com.marketinghub.ads;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "ig_account")
public class InstagramAccount {
    @Id
    private Long id;
    private String name;
    private String currency;
    private String avatarUrl;

    public InstagramAccount() {}

    public InstagramAccount(Long id, String name, String currency, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.avatarUrl = avatarUrl;
    }

    public InstagramAccount(Long id, String name, String currency) {
        this(id, name, currency, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
