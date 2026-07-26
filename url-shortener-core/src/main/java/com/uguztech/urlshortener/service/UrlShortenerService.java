package com.uguztech.urlshortener.service;

import com.uguztech.urlshortener.generator.CodeGenerator;
import com.uguztech.urlshortener.model.ShortUrl;
import com.uguztech.urlshortener.store.UrlStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class UrlShortenerService {

    private final UrlStore store;
    private final CodeGenerator codeGenerator;
    private final AtomicLong counter = new AtomicLong();

    public UrlShortenerService(UrlStore store, CodeGenerator codeGenerator) {
        this.store = store;
        this.codeGenerator = codeGenerator;
    }

    public ShortUrl shorten(String originalUrl, Duration ttl){
        long id = counter.incrementAndGet();
        String code = codeGenerator.generate(id);

        Instant createdAt = Instant.now();
        Instant expiresAt = (ttl == null) ? null : createdAt.plus(ttl);

        ShortUrl shortUrl = new ShortUrl(code, originalUrl, createdAt, expiresAt);
        store.save(shortUrl);

        return shortUrl;
    }

    public Optional<String> resolve(String code){
        Optional<ShortUrl> shortUrlOpt = store.findByCode(code);

        if(shortUrlOpt.isEmpty()){
            return Optional.empty();
        }

        ShortUrl shortUrl = shortUrlOpt.get();

        boolean isExpired = shortUrl.expiresAt() != null && shortUrl.expiresAt().isBefore(Instant.now());

        if(isExpired){
            return Optional.empty();
        }

        return Optional.of(shortUrl.originalUrl());
    }
}
