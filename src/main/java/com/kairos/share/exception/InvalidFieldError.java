package com.kairos.share.exception;

public record InvalidFieldError(
        String field,
        String error
) {

    public InvalidFieldError of(String field, String error){
        return new InvalidFieldError(field, error);
    }

}
