package com.uguztech.urlshortener.store;

import com.uguztech.urlshortener.model.ShortUrl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUrlStoreTest {

    private final UrlStore store = new InMemoryUrlStore();

    @Test
    void savedShortUrlShouldBeFoundByCode() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://example.com", Instant.now(), null);

        store.save(shortUrl);
        Optional<ShortUrl> result = store.findByCode("abc123");

        assertTrue(result.isPresent());
        assertEquals("https://example.com", result.get().originalUrl());
    }

    @Test
    void unknownCodeShouldReturnEmptyOptional() {
        Optional<ShortUrl> result = store.findByCode("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void savingSameCodeTwiceShouldOverwritePreviousValue() {
        ShortUrl first = new ShortUrl("abc123", "https://first.com", Instant.now(), null);
        ShortUrl second = new ShortUrl("abc123", "https://second.com", Instant.now(), null);

        store.save(first);
        store.save(second);

        Optional<ShortUrl> result = store.findByCode("abc123");
        assertTrue(result.isPresent());
        assertEquals("https://second.com", result.get().originalUrl());
    }

    @Test
    void differentCodesShouldBeStoredIndependently() {
        ShortUrl first = new ShortUrl("code1", "https://first.com", Instant.now(), null);
        ShortUrl second = new ShortUrl("code2", "https://second.com", Instant.now(), null);

        store.save(first);
        store.save(second);

        assertEquals("https://first.com", store.findByCode("code1").get().originalUrl());
        assertEquals("https://second.com", store.findByCode("code2").get().originalUrl());
    }
}
