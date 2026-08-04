package com.uguztech.qrgenerator;

import com.uguztech.qrgenerator.controller.QrGeneratorController;
import com.uguztech.qrgenerator.service.QrCodeGenerator;
import com.uguztech.webcommon.cors.CorsConfigurer;
import com.uguztech.webcommon.error.ErrorHandler;
import com.uguztech.webcommon.json.JsonMapperFactory;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String allowedOriginsEnv = dotenv.get("ALLOWED_ORIGINS", "*");
        List<String> allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                .map(String::trim)
                .toList();

        QrCodeGenerator qrCodeGenerator = new QrCodeGenerator();
        QrGeneratorController controller = new QrGeneratorController(qrCodeGenerator);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(JsonMapperFactory.createObjectMapper(), false));
            CorsConfigurer.configure(config, allowedOrigins);

            config.registerPlugin(new OpenApiPlugin(openApiConfig -> openApiConfig
                    .withDocumentationPath("/qr/openapi")
                    .withDefinitionConfiguration((version, definition) -> definition
                            .withInfo(info -> info
                                    .title("UguztechOpenSourceApis - QR Generator")
                                    .description("Open source QR code generator API")
                            )
                    )
            ));
            config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.setDocumentationPath("/qr/openapi");
                swaggerConfig.setUiPath("/qr/swagger");
            }));
        }).start(8081);

        ErrorHandler.register(app);

        app.post("/qr/api/v1/generate", controller::generate);
        app.get("/qr/health", ctx -> ctx.result("OK"));
    }
}