package org.zaikorea.zaiclient.exceptions;

import retrofit2.HttpException;

public class ZaiClientException extends Exception {

    private final int httpStatusCode;

    public ZaiClientException(String error, HttpException exception) {
        super(error, exception);
        this.httpStatusCode = exception.code();
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
