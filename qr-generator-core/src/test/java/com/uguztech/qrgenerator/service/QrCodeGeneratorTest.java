package com.uguztech.qrgenerator.service;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.uguztech.qrgenerator.model.QrCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeGeneratorTest {

    private QrCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new QrCodeGenerator();
    }

    @Test
    void shouldGenerateQrCodeWithDefaultSettings() {
        String content = "https://github.com";

        QrCode qrCode = generator.generate(content);

        assertNotNull(qrCode);
        assertNotNull(qrCode.imageData());
        assertTrue(qrCode.imageData().length > 0);
        assertEquals(QrCode.ImageFormat.PNG, qrCode.format());
        assertEquals(300, qrCode.size());
    }

    @Test
    void shouldGenerateQrCodeWithCustomSize(){
        String content = "Hello World";
        int customSize = 5;

        QrCode qrCode = generator.generate(content, customSize, QrCode.ImageFormat.PNG, ErrorCorrectionLevel.H);
        assertNotNull(qrCode);
        assertEquals(customSize, qrCode.size());
        assertEquals(QrCode.ImageFormat.PNG, qrCode.format());
        assertTrue(qrCode.imageData().length > 0);
    }

    @Test
    void shouldGenerateQrCodeWithJpgFormat(){
        String content = "Test Content";

        QrCode qrCode = generator.generate(content, 300, QrCode.ImageFormat.JPG, ErrorCorrectionLevel.H);

        assertNotNull(qrCode);
        assertEquals(QrCode.ImageFormat.JPG, qrCode.format());
    }

    @Test
    void  shouldHandleUnicodeContent(){
        String content = "Merhaba Dünya! 你好世界 \uD83C\uDF89";

        QrCode qrCode = generator.generate(content);

        assertNotNull(qrCode);
        assertTrue(qrCode.imageData().length > 0);
    }

    @Test
    void shouldThrowExceptionForEmptyContent(){
        assertThrows(QrCodeGenerationException.class, () -> generator.generate(""));
    }
}
