package com.uguztech.urlshortener.store;

import com.uguztech.urlshortener.model.ShortUrl;

import java.util.Optional;

public interface UrlStore {
    void save(ShortUrl shortUrl);
    Optional<ShortUrl> findByCode(String code);
    default void shutdown() {
        // no-op by default; kaynak tutan implementasyonlar (executor, connection) override etsin
    }
}