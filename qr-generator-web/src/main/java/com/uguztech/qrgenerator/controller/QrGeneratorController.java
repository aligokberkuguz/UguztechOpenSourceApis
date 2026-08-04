package com.uguztech.qrgenerator.controller;

import com.uguztech.qrgenerator.dto.QrGenerateRequest;
import com.uguztech.qrgenerator.model.QrCode;
import com.uguztech.qrgenerator.service.QrCodeGenerator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.openapi.*;

public class QrGeneratorController {

    private final QrCodeGenerator qrCodeGenerator;

    public QrGeneratorController(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @OpenApi(
            summary = "Generate QR Code",
            operationId = "generateQrCode",
            path = "/qr/api/v1/generate",
            methods = HttpMethod.POST,
            tags = {"QR Generator"},
            requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = QrGenerateRequest.class)}),
            responses = {
                    @OpenApiResponse(status = "200", content = {@OpenApiContent(type = "image/png")}, description = "QR code image"),
                    @OpenApiResponse(status = "400", description = "Invalid request")
            }
    )
    public void generate(Context ctx) {
        QrGenerateRequest request = ctx.bodyAsClass(QrGenerateRequest.class);

        if (request.content() == null || request.content().isBlank()) {
            throw new BadRequestResponse("Content is required");
        }

        QrCode qrCode = qrCodeGenerator.generate(
                request.content(),
                request.getEffectiveSize(),
                request.getEffectiveFormat(),
                request.getEffectiveErrorCorrection()
        );

        String contentType = qrCode.format() == QrCode.ImageFormat.PNG ? "image/png" : "image/jpeg";

        ctx.status(200)
                .contentType(contentType)
                .result(qrCode.imageData());
    }
}