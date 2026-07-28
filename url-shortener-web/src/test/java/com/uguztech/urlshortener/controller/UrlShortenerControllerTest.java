package com.uguztech.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uguztech.urlshortener.dto.ShortenResponse;
import com.uguztech.urlshortener.generator.Base62CodeGenerator;
import com.uguztech.urlshortener.service.UrlShortenerService;
import com.uguztech.urlshortener.store.InMemoryUrlStore;
import com.uguztech.webcommon.error.ErrorHandler;
import com.uguztech.webcommon.error.ProblemDetail;
import com.uguztech.webcommon.json.JsonMapperFactory;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestConfig;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlShortenerControllerTest {

    private static final String BASE_URL = "http://localhost:7070";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapperFactory.createObjectMapper();

    private static final OkHttpClient NO_REDIRECT_CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .build();
    private static final TestConfig NO_REDIRECT_CONFIG = new TestConfig(true, false, NO_REDIRECT_CLIENT);

    private Javalin createApp() {
        UrlShortenerService service = new UrlShortenerService(new InMemoryUrlStore(), new Base62CodeGenerator());
        UrlShortenerController controller = new UrlShortenerController(service, BASE_URL);

        Javalin app = Javalin.create(config ->
                config.jsonMapper(new JavalinJackson(OBJECT_MAPPER, false))
        );

        ErrorHandler.register(app); // Main.java'daki global hata handler'ı burada da kayıtlı olmalı

        app.post("/api/v1/shorten", controller::shorten);
        app.get("/{code}", controller::redirect);

        return app;
    }

    // Hata response'unun RFC 7807 sözleşmesine (content-type + zorunlu alanlar) uyduğunu doğrular.
    private void assertProblemDetail(Response response, int expectedStatus) throws Exception {
        assertTrue(response.header("Content-Type").startsWith("application/problem+json"));

        ProblemDetail problem = OBJECT_MAPPER.readValue(response.body().string(), ProblemDetail.class);

        assertEquals(expectedStatus, problem.status());
        assertNotNull(problem.detail());
        assertFalse(problem.detail().isBlank());
        assertNotNull(problem.title());
        assertNotNull(problem.instance());
    }

    @Test
    void shortenShouldReturn201WithShortUrlDetails() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://example.com\", \"ttlMinutes\": null}"
            );

            assertEquals(201, response.code());

            ShortenResponse body = OBJECT_MAPPER.readValue(response.body().string(), ShortenResponse.class);

            assertNotNull(body.code());
            assertEquals("https://example.com", body.originalUrl());
            assertEquals(BASE_URL + "/" + body.code(), body.shortUrl());
            assertNotNull(body.createdAt());
            assertNull(body.expiresAt());
        });
    }

    @Test
    void shortenWithTtlShouldSetExpiresAtAfterCreatedAt() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://example.com\", \"ttlMinutes\": 10}"
            );

            ShortenResponse body = OBJECT_MAPPER.readValue(response.body().string(), ShortenResponse.class);

            assertNotNull(body.expiresAt());
            assertTrue(body.expiresAt().isAfter(body.createdAt()));
        });
    }

    @Test
    void consecutiveShortenCallsShouldProduceDifferentCodes() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response firstResponse = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://first.com\", \"ttlMinutes\": null}"
            );
            Response secondResponse = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://second.com\", \"ttlMinutes\": null}"
            );

            ShortenResponse first = OBJECT_MAPPER.readValue(firstResponse.body().string(), ShortenResponse.class);
            ShortenResponse second = OBJECT_MAPPER.readValue(secondResponse.body().string(), ShortenResponse.class);

            assertNotNull(first.code());
            assertNotNull(second.code());
            assertTrue(!first.code().equals(second.code()));
        });
    }

    @Test
    void redirectShouldReturn302WithLocationHeaderForExistingCode() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response shortenResponse = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://github.com\", \"ttlMinutes\": null}"
            );
            ShortenResponse shortUrl = OBJECT_MAPPER.readValue(shortenResponse.body().string(), ShortenResponse.class);

            Response redirectResponse = client.get("/" + shortUrl.code());

            assertEquals(302, redirectResponse.code());
            assertEquals("https://github.com", redirectResponse.header("Location"));
        });
    }

    @Test
    void redirectShouldReturn404ForUnknownCode() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.get("/doesnotexist");
            assertEquals(404, response.code());
            assertProblemDetail(response, 404);
        });
    }

    // Not: "expired short url" senaryosu (negatif Duration ile) zaten
    // UrlShortenerServiceTest (core modülü) içinde servis seviyesinde test ediliyor.
    // Web katmanında artık negatif ttlMinutes bir validasyon hatası (400) olarak
    // reddedildiği için o senaryoyu burada tekrar üretmek mümkün değil / anlamsız.

    @Test
    void shortenShouldReturn400WhenUrlIsBlank() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"\", \"ttlMinutes\": null}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }

    @Test
    void shortenShouldReturn400WhenUrlIsMissing() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"ttlMinutes\": null}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }

    @Test
    void shortenShouldReturn400WhenUrlIsNotAValidUrl() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"not-a-valid-url\", \"ttlMinutes\": null}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }

    @Test
    void shortenShouldReturn400WhenUrlSchemeIsNotHttpOrHttps() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"ftp://example.com\", \"ttlMinutes\": null}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }

    @Test
    void shortenShouldReturn400WhenTtlMinutesIsZero() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://example.com\", \"ttlMinutes\": 0}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }

    @Test
    void shortenShouldReturn400WhenTtlMinutesIsNegative() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, NO_REDIRECT_CONFIG, (server, client) -> {
            Response response = client.post(
                    "/api/v1/shorten",
                    "{\"url\": \"https://example.com\", \"ttlMinutes\": -1}"
            );

            assertEquals(400, response.code());
            assertProblemDetail(response, 400);
        });
    }
}