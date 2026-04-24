package com.kairos.share.exception;

import java.util.List;

public record ErrorResponse(
        int code,
        String message,
        String description,
        String path,
        List<InvalidFieldError> fieldErrors
) {

    public static ErrorResponse toInstance(
            int code,
            String message,
            String description,
            String path
    ) {
        return new ErrorResponse(
                code,
                message,
                description,
                path,
                null
        );
    }

    public static ErrorResponse toInstance(
            int code,
            String message,
            String description,
            String path,
            List<InvalidFieldError> fieldErrors
    ) {
        return new ErrorResponse(
                code,
                message,
                description,
                path,
                fieldErrors
        );
    }


}
