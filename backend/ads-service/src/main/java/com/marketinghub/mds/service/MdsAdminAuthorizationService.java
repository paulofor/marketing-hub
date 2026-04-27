package com.marketinghub.mds.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class MdsAdminAuthorizationService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MDS_OPERATOR", "OPS");

    public void assertAllowed(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "missing X-User-Role");
        }
        String normalized = roleHeader.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role not allowed for MDS admin APIs");
        }
    }
}
