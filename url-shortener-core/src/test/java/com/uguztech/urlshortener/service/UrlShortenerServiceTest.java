package com.uguztech.urlshortener.service;

import com.uguztech.urlshortener.generator.CodeGenerator;
import com.uguztech.urlshortener.model.ShortUrl;
import com.uguztech.urlshortener.store.InMemoryUrlStore;
import com.uguztech.urlshortener.store.UrlStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlShortenerServiceTest {

    // Gerçek Base62 algoritması yerine öngörülebilir kodlar üreten sahte (test double) generator.
    // Böylece testler sadece UrlShortenerService'in mantığını doğrular, kod üretim algoritmasına bağımlı olmaz.
    private final CodeGenerator fixedCodeGenerator = id -> "CODE" + id;

    private final UrlStore store = new InMemoryUrlStore();
    private final UrlShortenerService service = new UrlShortenerService(store, fixedCodeGenerator);

    @Test
    void shortenShouldGenerateCodeAndStoreShortUrl() {
        ShortUrl result = service.shorten("https://example.com", null);

        assertEquals("CODE1", result.code());
        assertEquals("https://example.com", result.originalUrl());
        assertNotNull(result.createdAt());
        assertNull(result.expiresAt());
        assertTrue(store.findByCode("CODE1").isPresent());
    }

    @Test
    void shortenWithTtlShouldSetExpiresAtAfterCreatedAt() {
        ShortUrl result = service.shorten("https://example.com", Duration.ofMinutes(10));

        assertNotNull(result.expiresAt());
        assertTrue(result.expiresAt().isAfter(result.createdAt()));
    }

    @Test
    void consecutiveShortenCallsShouldProduceDifferentCodes() {
        ShortUrl first = service.shorten("https://first.com", null);
        ShortUrl second = service.shorten("https://second.com", null);

        assertNotEquals(first.code(), second.code());
    }

    @Test
    void resolveShouldReturnOriginalUrlForValidCode() {
        ShortUrl shortUrl = service.shorten("https://example.com", null);

        Optional<String> resolved = service.resolve(shortUrl.code());

        assertTrue(resolved.isPresent());
        assertEquals("https://example.com", resolved.get());
    }

    @Test
    void resolveShouldReturnEmptyForUnknownCode() {
        Optional<String> resolved = service.resolve("nonexistent");
        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolveShouldReturnEmptyForExpiredShortUrl() {
        ShortUrl shortUrl = service.shorten("https://example.com", Duration.ofSeconds(-1));

        Optional<String> resolved = service.resolve(shortUrl.code());

        assertTrue(resolved.isEmpty());
    }
}
