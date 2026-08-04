package com.uguztech.qrgenerator.model;

public record QrCode(
        byte[] imageData,
        ImageFormat format,
        int size
) {
    public enum ImageFormat {
        PNG,
        JPG
    }
}
