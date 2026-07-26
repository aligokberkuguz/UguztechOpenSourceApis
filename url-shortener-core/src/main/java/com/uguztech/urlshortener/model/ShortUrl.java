package com.uguztech.urlshortener.model;

import java.time.Instant;

public record ShortUrl(String code, String originalUrl, Instant createdAt, Instant expiresAt) {

}