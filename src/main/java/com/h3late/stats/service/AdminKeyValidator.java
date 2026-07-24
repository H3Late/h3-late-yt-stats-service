package com.h3late.stats.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminKeyValidator {

    @Value("${admin.key:}")
    private String adminKey;

    public void validate(String providedKey) {
        if (adminKey.isBlank() || !adminKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }
    }
}
