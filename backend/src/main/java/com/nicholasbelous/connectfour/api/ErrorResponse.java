package com.nicholasbelous.connectfour.api;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;

/** The JSON body of every error response. */
public record ErrorResponse(int status, String error, String message) {

    static ErrorResponse of(HttpStatusCode statusCode, String message) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String error = status != null ? status.getReasonPhrase() : "Error";
        return new ErrorResponse(statusCode.value(), error, message);
    }
}
