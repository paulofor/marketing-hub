package com.marketinghub.ads;

import jakarta.persistence.*;

@Entity
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "facebook_account_id")
    private FacebookAccount facebookAccount;

    @ManyToOne
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    public Campaign() {}

    public Campaign(String name, FacebookAccount fbAccount) {
        this.name = name;
        this.facebookAccount = fbAccount;
    }

    public Campaign(String name, InstagramAccount igAccount) {
        this.name = name;
        this.instagramAccount = igAccount;
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

    public FacebookAccount getFacebookAccount() {
        return facebookAccount;
    }

    public void setFacebookAccount(FacebookAccount facebookAccount) {
        this.facebookAccount = facebookAccount;
    }

    public InstagramAccount getInstagramAccount() {
        return instagramAccount;
    }

    public void setInstagramAccount(InstagramAccount instagramAccount) {
        this.instagramAccount = instagramAccount;
    }
}
