package com.uguztech.webcommon.error;

import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;

public final class ErrorHandler {

    private static final String PROBLEM_JSON = "application/problem+json";

    private ErrorHandler() {}

    public static void register(Javalin app) {
        app.exception(HttpResponseException.class, (e, ctx) -> {
            ProblemDetail problem = new ProblemDetail(
                    "about:blank",
                    reasonPhrase(e.getStatus()),
                    e.getStatus(),
                    e.getMessage(),
                    ctx.path()
            );
            ctx.status(e.getStatus());
            ctx.json(problem);
            ctx.contentType(PROBLEM_JSON); // ctx.json() content-type'ı application/json yapar, üzerine yazıyoruz
        });

        app.exception(Exception.class, (e, ctx) -> {
            ProblemDetail problem = new ProblemDetail(
                    "about:blank", "Internal Server Error", 500,
                    "Beklenmeyen bir hata olustu", ctx.path()
            );
            ctx.status(500);
            ctx.json(problem);
            ctx.contentType(PROBLEM_JSON);
        });
    }

    private static String reasonPhrase(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
}