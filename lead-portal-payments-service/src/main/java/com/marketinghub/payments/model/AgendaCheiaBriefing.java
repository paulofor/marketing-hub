package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persiste o briefing pós-compra do Agenda Cheia até a produção e entrega do kit. */
@Entity
@Table(name = "agenda_cheia_briefing")
public class AgendaCheiaBriefing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "buyer_email", nullable = false, length = 320)
    private String buyerEmail;

    @Column(name = "professional_name", nullable = false, length = 180)
    private String professionalName;

    @Column(name = "city_region", nullable = false, length = 180)
    private String cityRegion;

    @Column(name = "whatsapp", nullable = false, length = 40)
    private String whatsapp;

    @Column(name = "services", nullable = false, columnDefinition = "TEXT")
    private String services;

    @Column(name = "visual_style", nullable = false, length = 120)
    private String visualStyle;

    @Column(name = "preferred_colors", length = 180)
    private String preferredColors;

    @Column(name = "weekly_goal", nullable = false, length = 180)
    private String weeklyGoal;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public Long getId() { return id; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String value) { paymentId = value; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String value) { buyerEmail = value; }
    public String getProfessionalName() { return professionalName; }
    public void setProfessionalName(String value) { professionalName = value; }
    public String getCityRegion() { return cityRegion; }
    public void setCityRegion(String value) { cityRegion = value; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String value) { whatsapp = value; }
    public String getServices() { return services; }
    public void setServices(String value) { services = value; }
    public String getVisualStyle() { return visualStyle; }
    public void setVisualStyle(String value) { visualStyle = value; }
    public String getPreferredColors() { return preferredColors; }
    public void setPreferredColors(String value) { preferredColors = value; }
    public String getWeeklyGoal() { return weeklyGoal; }
    public void setWeeklyGoal(String value) { weeklyGoal = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant value) { submittedAt = value; }
}
