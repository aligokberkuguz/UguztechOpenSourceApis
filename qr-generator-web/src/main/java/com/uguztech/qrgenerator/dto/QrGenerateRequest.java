package com.uguztech.qrgenerator.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.uguztech.qrgenerator.model.QrCode;

public record QrGenerateRequest(
        String content,
        Integer size,
        String format,
        String errorCorrection
) {
    @JsonIgnore
    public int getEffectiveSize(){
        return size != null && size > 0 ? size : 300;
    }

    @JsonIgnore
    public QrCode.ImageFormat getEffectiveFormat(){
        if(format == null){
            return QrCode.ImageFormat.PNG;
        }
        try {
            return QrCode.ImageFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            return QrCode.ImageFormat.PNG;
        }
    }

    @JsonIgnore
    public ErrorCorrectionLevel getEffectiveErrorCorrection(){
        if(errorCorrection == null){
            return ErrorCorrectionLevel.M;
        }
        try {
            return ErrorCorrectionLevel.valueOf(errorCorrection.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ErrorCorrectionLevel.M;
        }
    }
}