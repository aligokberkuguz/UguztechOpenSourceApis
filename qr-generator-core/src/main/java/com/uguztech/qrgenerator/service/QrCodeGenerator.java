package com.uguztech.qrgenerator.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.uguztech.qrgenerator.model.QrCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QrCodeGenerator {

    private static final int DEFAULT_SIZE = 300;
    private static final ErrorCorrectionLevel DEFAULT_ERROR_CORRECTION = ErrorCorrectionLevel.L;

    public QrCode generate(String content) {
        return generate(content, DEFAULT_SIZE, QrCode.ImageFormat.PNG, DEFAULT_ERROR_CORRECTION);

    }

    public QrCode generate(String content, int size, QrCode.ImageFormat format, ErrorCorrectionLevel errorCorrection){
        if (content == null || content.isEmpty()) {
            throw new QrCodeGenerationException("content must not be null or empty", null);
        }
        if (size <= 0) {
            throw new QrCodeGenerationException("size must be greater than zero", null);
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrection);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, format.name(), outputStream);
            byte[] imageData = outputStream.toByteArray();

            return new QrCode(imageData, format, size);
        } catch (WriterException | IOException e){
            throw new QrCodeGenerationException("Failed to generate QR code for content: " + content, e);
        }
    }
}
