package com.uguztech.urlshortener.dto;

public record ShortenRequest(String url, Long ttlMinutes) {
}
