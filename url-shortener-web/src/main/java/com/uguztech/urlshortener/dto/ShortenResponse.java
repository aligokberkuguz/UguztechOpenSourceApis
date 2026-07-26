package com.uguztech.urlshortener.dto;

import java.time.Instant;

public record ShortenResponse(String code, String shortUrl, String originalUrl, Instant createdAt, Instant expiresAt) {
}
