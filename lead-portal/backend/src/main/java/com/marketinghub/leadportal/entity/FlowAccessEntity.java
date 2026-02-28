package com.marketinghub.leadportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "flow_access")
public class FlowAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flow_slug", length = 190, nullable = false)
    private String flowSlug;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(length = 1024)
    private String referer;

    @Column(name = "visitor_id", length = 128)
    private String visitorId;

    @Column(name = "campaign_code", length = 190)
    private String campaignCode;

    @CreationTimestamp
    @Column(name = "accessed_at", updatable = false)
    private Instant accessedAt;

    public Long getId() {
        return id;
    }

    public String getFlowSlug() {
        return flowSlug;
    }

    public void setFlowSlug(String flowSlug) {
        this.flowSlug = flowSlug;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }
}
