package com.marketinghub.vitrines.app.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Role {
  ANON,
  LEAD,
  CLIENTE,
  ADMIN;

  public static Role from(String rawRole) {
    if (rawRole == null || rawRole.isBlank()) {
      return ANON;
    }

    try {
      return Role.valueOf(rawRole.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Role inválida. Use ANON, LEAD, CLIENTE ou ADMIN.");
    }
  }
}
