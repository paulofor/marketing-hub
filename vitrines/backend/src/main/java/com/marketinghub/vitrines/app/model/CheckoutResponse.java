package com.marketinghub.vitrines.app.model;

public record CheckoutResponse(String status, String paymentUrl, String planId, String email) {}
