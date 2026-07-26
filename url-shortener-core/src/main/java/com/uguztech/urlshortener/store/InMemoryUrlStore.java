package com.uguztech.urlshortener.store;

import com.uguztech.urlshortener.model.ShortUrl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUrlStore implements UrlStore {

    private final Map<String, ShortUrl> storage = new ConcurrentHashMap<>();

    @Override
    public void save(ShortUrl shortUrl) {
        storage.put(shortUrl.code(), shortUrl);
    }

    @Override
    public Optional<ShortUrl> findByCode(String code) {
        return Optional.ofNullable(storage.get(code));
    }
}