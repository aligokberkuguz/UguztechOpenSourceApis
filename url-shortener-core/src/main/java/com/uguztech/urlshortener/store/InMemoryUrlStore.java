package com.uguztech.urlshortener.store;

import com.uguztech.urlshortener.model.ShortUrl;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InMemoryUrlStore implements UrlStore {

    private final Map<String, ShortUrl> storage = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "url-store-cleanup");
        t.setDaemon(true); // JVM/container shutdown'ı bekletmesin
        return t;
    });

    public InMemoryUrlStore() {
        cleanupExecutor.scheduleAtFixedRate(this::removeExpired, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void save(ShortUrl shortUrl) {
        storage.put(shortUrl.code(), shortUrl);
    }

    @Override
    public Optional<ShortUrl> findByCode(String code) {
        return Optional.ofNullable(storage.get(code));
    }

    @Override
    public void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    // package-private: test 1 dakika beklemeden doğrudan çağırabilsin
    void removeExpired() {
        Instant now = Instant.now();
        storage.values().removeIf(shortUrl ->
                shortUrl.expiresAt() != null && shortUrl.expiresAt().isBefore(now));
    }
}