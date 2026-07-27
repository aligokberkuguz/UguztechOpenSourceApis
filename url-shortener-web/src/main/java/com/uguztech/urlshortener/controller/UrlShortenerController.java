package com.uguztech.urlshortener.controller;

import com.uguztech.urlshortener.dto.ShortenRequest;
import com.uguztech.urlshortener.dto.ShortenResponse;
import com.uguztech.urlshortener.model.ShortUrl;
import com.uguztech.urlshortener.service.UrlShortenerService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.time.Duration;
import java.util.Optional;

public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final String BASE_URL;

    public UrlShortenerController(UrlShortenerService urlShortenerService, String BASE_URL) {
        this.urlShortenerService = urlShortenerService;
        this.BASE_URL = BASE_URL;
    }

    public void shorten(Context ctx) {
        ShortenRequest request = ctx.bodyAsClass(ShortenRequest.class);

        if (request.url() == null || request.url().isBlank()) {
            throw new BadRequestResponse("url must not be blank");
        }

        if (!isValidUrl(request.url())) {
            throw new BadRequestResponse("url is not a valid URL");
        }

        if (request.ttlMinutes() != null && request.ttlMinutes() <= 0) {
            throw new BadRequestResponse("ttlMinutes must be a positive number");
        }

        Duration ttl = (request.ttlMinutes() == null) ? null : Duration.ofMinutes(request.ttlMinutes());
        ShortUrl shortUrl = urlShortenerService.shorten(request.url(), ttl);
        String fullShortUrl = BASE_URL + "/" + shortUrl.code();

        ShortenResponse response = new ShortenResponse(
                shortUrl.code(), fullShortUrl, shortUrl.originalUrl(), shortUrl.createdAt(), shortUrl.expiresAt()
        );

        ctx.status(201).json(response);
    }

    private boolean isValidUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.isAbsolute() && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    public void redirect(Context ctx){
        String code = ctx.pathParam("code");

        Optional<String> originalUrl = urlShortenerService.resolve(code);

        if(originalUrl.isEmpty()){
            throw new NotFoundResponse("Short URL not found or expired");
        }

        ctx.redirect(originalUrl.get());
    }
}
