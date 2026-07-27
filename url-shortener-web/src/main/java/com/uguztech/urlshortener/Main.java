package com.uguztech.urlshortener;

import com.uguztech.urlshortener.controller.UrlShortenerController;
import com.uguztech.urlshortener.generator.Base62CodeGenerator;
import com.uguztech.urlshortener.service.UrlShortenerService;
import com.uguztech.urlshortener.store.InMemoryUrlStore;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import com.uguztech.webcommon.json.JsonMapperFactory;
import com.uguztech.webcommon.cors.CorsConfigurer;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String baseUrl = dotenv.get("BASE_URL", "http://localhost:7070");

        String allowedOriginsEnv = dotenv.get("ALLOWED_ORIGINS", "*");
        List<String> allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                .map(String::trim)
                .toList();

        InMemoryUrlStore store = new InMemoryUrlStore();
        UrlShortenerService service = new UrlShortenerService(store, new Base62CodeGenerator());

        UrlShortenerController controller = new UrlShortenerController(service, baseUrl);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(JsonMapperFactory.createObjectMapper(), false));
            CorsConfigurer.configure(config, allowedOrigins);
            config.events(event -> event.serverStopping(store::shutdown));

            config.registerPlugin(new OpenApiPlugin(openApiConfig -> openApiConfig
                    .withDocumentationPath("/openapi")
                    .withDefinitionConfiguration((version, definition) -> definition
                            .withInfo(info -> info
                                    .title("UguztechOpenSourceApis - URL Shortener")
                                    .description("Open source URL shortener API")
                            )
                    )
            ));
            config.registerPlugin(new SwaggerPlugin(swaggerConfig ->
                    swaggerConfig.setDocumentationPath("/openapi")
            ));
        }).start(7070);

        app.post("/api/v1/shorten", controller::shorten);
        app.get("/{code}", controller::redirect);
    }
}