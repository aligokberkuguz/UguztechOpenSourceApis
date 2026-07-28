package com.uguztech.webcommon.error;

public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance
) {}