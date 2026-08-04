package com.uguztech.qrgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uguztech.qrgenerator.dto.QrGenerateRequest;
import com.uguztech.qrgenerator.service.QrCodeGenerator;
import com.uguztech.webcommon.error.ErrorHandler;
import com.uguztech.webcommon.json.JsonMapperFactory;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestConfig;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrGeneratorControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapperFactory.createObjectMapper();
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
    private static final TestConfig TEST_CONFIG = new TestConfig(true, false, CLIENT);

    private Javalin createApp() {
        QrCodeGenerator generator = new QrCodeGenerator();
        QrGeneratorController controller = new QrGeneratorController(generator);

        Javalin app = Javalin.create(config ->
                config.jsonMapper(new JavalinJackson(OBJECT_MAPPER, false))
        );

        ErrorHandler.register(app);

        app.post("/qr/api/v1/generate", controller::generate);

        return app;
    }

    @Test
    void shouldGenerateQrCodeWithDefaultSettings() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, TEST_CONFIG, (server, client) -> {
            QrGenerateRequest request = new QrGenerateRequest("https://uguztech.com", null, null, null);
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            Response response = client.post("/qr/api/v1/generate", requestBody);

            assertEquals(200, response.code());
            assertEquals("image/png", response.header("Content-Type"));
            assertNotNull(response.body());
            assertTrue(response.body().bytes().length > 0);
        });
    }

    @Test
    void shouldGenerateQrCodeWithCustomSettings() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, TEST_CONFIG, (server, client) -> {
            QrGenerateRequest request = new QrGenerateRequest("Hello World", 500, "JPG", "H");
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            Response response = client.post("/qr/api/v1/generate", requestBody);

            assertEquals(200, response.code());
            assertEquals("image/jpeg", response.header("Content-Type"));
            assertNotNull(response.body());
            assertTrue(response.body().bytes().length > 0);
        });
    }

    @Test
    void shouldReturn400ForEmptyContent() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, TEST_CONFIG, (server, client) -> {
            QrGenerateRequest request = new QrGenerateRequest("", null, null, null);
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            Response response = client.post("/qr/api/v1/generate", requestBody);

            assertEquals(400, response.code());
        });
    }

    @Test
    void shouldReturn400ForNullContent() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, TEST_CONFIG, (server, client) -> {
            QrGenerateRequest request = new QrGenerateRequest(null, null, null, null);
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            Response response = client.post("/qr/api/v1/generate", requestBody);

            assertEquals(400, response.code());
        });
    }

    @Test
    void shouldHandleUnicodeContent() throws Exception {
        Javalin app = createApp();

        JavalinTest.test(app, TEST_CONFIG, (server, client) -> {
            QrGenerateRequest request = new QrGenerateRequest("Merhaba Dünya! 你好世界 🎉", null, null, null);
            String requestBody = OBJECT_MAPPER.writeValueAsString(request);

            Response response = client.post("/qr/api/v1/generate", requestBody);

            assertEquals(200, response.code());
            assertEquals("image/png", response.header("Content-Type"));
            assertTrue(response.body().bytes().length > 0);
        });
    }
}