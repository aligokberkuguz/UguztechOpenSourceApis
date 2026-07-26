package com.uguztech.urlshortener;

import com.uguztech.urlshortener.controller.UrlShortenerController;
import com.uguztech.urlshortener.generator.Base62CodeGenerator;
import com.uguztech.urlshortener.service.UrlShortenerService;
import com.uguztech.urlshortener.store.InMemoryUrlStore;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.uguztech.webcommon.json.JsonMapperFactory;

public class Main {
    public static void main(String[] args) {
        String BASE_URL = System.getenv().getOrDefault("BASE_URL", "http://localhost:7070");
        UrlShortenerService service = new UrlShortenerService(new InMemoryUrlStore(), new Base62CodeGenerator());
        UrlShortenerController controller = new UrlShortenerController(service, BASE_URL);

        Javalin app = Javalin.create(
                config ->
                        config.jsonMapper(new JavalinJackson(JsonMapperFactory.createObjectMapper(), false))
                )
                .start(7070);


        app.post("/api/v1/shorten", controller::shorten);
        app.get("/{code}", controller::redirect);
    }
}