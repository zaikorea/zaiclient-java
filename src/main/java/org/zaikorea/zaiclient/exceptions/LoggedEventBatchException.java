package org.zaikorea.zaiclient.exceptions;

public class LoggedEventBatchException extends RuntimeException {

    public LoggedEventBatchException() {
        super("Cannot modify an already logged event batch.");
    }
}
