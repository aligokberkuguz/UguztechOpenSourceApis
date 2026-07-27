package com.uguztech.urlshortener.store;

import com.uguztech.urlshortener.model.ShortUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUrlStoreTest {

    private InMemoryUrlStore store = new InMemoryUrlStore();

    @AfterEach
    void tearDown() {
        store.shutdown(); // executor temizliği
    }

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

    @Test
    void removeExpiredShouldDeleteOnlyExpiredUrls() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(60);
        Instant future = now.plusSeconds(3600);

        ShortUrl expired = new ShortUrl("exp001", "https://expired.com", past.minusSeconds(120), past);
        ShortUrl active = new ShortUrl("act001", "https://active.com", now, future);
        ShortUrl neverExpires = new ShortUrl("nev001", "https://never.com", now, null);

        store.save(expired);
        store.save(active);
        store.save(neverExpires);

        store.removeExpired(); // package-private metodu direkt çağır

        assertTrue(store.findByCode("exp001").isEmpty(), "Expired URL should be removed");
        assertTrue(store.findByCode("act001").isPresent(), "Active URL should still exist");
        assertTrue(store.findByCode("nev001").isPresent(), "Never-expiring URL should still exist");
    }

    @Test
    void removeExpiredShouldNotFailOnEmptyStore() {
        assertDoesNotThrow(() -> store.removeExpired());
    }

    @Test
    void shutdownShouldBeIdempotent() {
        store.shutdown();
        // executor durduktan sonra tekrar çağrılsa bile exception fırlatmamalı
        assertDoesNotThrow(() -> store.shutdown());
    }
}
